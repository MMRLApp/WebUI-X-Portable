package com.dergoogler.mmrl.webui.view

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.util.Log
import android.view.ViewGroup.LayoutParams
import android.webkit.ValueCallback
import android.webkit.WebMessage
import android.widget.FrameLayout
import androidx.activity.result.ActivityResult
import androidx.annotation.CallSuper
import androidx.annotation.Keep
import androidx.annotation.UiThread
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.drawable.toDrawable
import androidx.core.net.toUri
import androidx.core.view.doOnAttach
import androidx.webkit.WebMessageCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.dergoogler.mmrl.ext.createNewWX
import com.dergoogler.mmrl.ext.exception.BrickException
import com.dergoogler.mmrl.ext.findActivity
import com.dergoogler.mmrl.ext.moshi.moshi
import com.dergoogler.mmrl.hybridwebui.HybridWebUI
import com.dergoogler.mmrl.hybridwebui.interfaces.JavaScriptInterfaceImplementation
import com.dergoogler.mmrl.webui.R
import com.dergoogler.mmrl.webui.interfaces.WXInterface
import com.dergoogler.mmrl.webui.interfaces.WXOptions
import com.dergoogler.mmrl.webui.model.JavaScriptInterface
import com.dergoogler.mmrl.webui.model.WXEvent
import com.dergoogler.mmrl.webui.model.WXEventHandler
import com.dergoogler.mmrl.webui.model.WXRawEvent
import com.dergoogler.mmrl.webui.util.WebUIOptions
import com.dergoogler.mmrl.webui.util.WebUIOptions.Companion.defaultWebUiOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Keep
@SuppressLint("ViewConstructor")
open class WebUIView : HybridWebUI {
    private val scope = CoroutineScope(Dispatchers.Main)
    protected var initJob: Job? = null
    internal var mSwipeView: WXSwipeRefresh? = null
    private var isDestroyed = false
    protected lateinit var options: WebUIOptions

    constructor(options: WebUIOptions) : super(options.context, options.domain) {
        this.options = options
        initWhenReady()
    }

    constructor(context: Context) : this(context.defaultWebUiOptions) {
        this.options = context.defaultWebUiOptions
        throw UnsupportedOperationException("Default constructor not supported. Use constructor with options.")
    }

    constructor(context: Context, attrs: AttributeSet) : this(context.defaultWebUiOptions) {
        this.options = context.defaultWebUiOptions
        throw UnsupportedOperationException("Default constructor not supported. Use constructor with options.")
    }

    constructor(
        context: Context,
        attrs: AttributeSet,
        defStyle: Int,
    ) : this(context.defaultWebUiOptions) {
        this.options = context.defaultWebUiOptions
        throw UnsupportedOperationException("Default constructor not supported. Use constructor with options.")
    }

    protected fun createDefaultWxOptions(options: WebUIOptions): WXOptions = WXOptions(
        webView = this,
        options = options
    )

    protected val interfaces: HashSet<JavaScriptInterface.Instance> = hashSetOf()
    protected val assetHandlers: MutableList<Pair<String, PathHandler>> = mutableListOf()

    protected open suspend fun onInit() {}

    private fun initWhenReady() {
        setWebContentsDebuggingEnabled(options.debug)

        id = R.id.webuiview

        layoutParams = LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
        doOnAttach {
            initJob = scope.launch {
                withContext(Dispatchers.Main) { awaitFrame() }
                if (!isDestroyed) {
                    initView()
                    onInit()
                }
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initView() {
        if (isDestroyed) return

        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
        }

        with(options) {
            setBackgroundColor(colorScheme.background.toArgb())
            background = colorScheme.background.toArgb().toDrawable()
        }

        Log.d(TAG, "WebUIView initialized/attached")
    }

    fun <R> options(block: WebUIOptions.() -> R): R? {
        return try {
            block(options)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get options:", e)
            null
        }
    }

    private fun isReadyForWebOps(): Boolean =
        !isDestroyed && isAttachedToWindow && handler != null

    fun postMessage(message: String) {
        if (!isReadyForWebOps()) {
            Log.w(TAG, "postMessage skipped, not ready"); return
        }
        val uri = "*".toUri()
        try {
            if (WebViewFeature.isFeatureSupported(WebViewFeature.POST_WEB_MESSAGE)) {
                WebViewCompat.postWebMessage(this, WebMessageCompat(message), uri)
            } else {
                super.postWebMessage(WebMessage(message), uri)
            }
        } catch (t: Throwable) {
            Log.e(TAG, "postMessage error", t)
        }
    }

    @Keep
    fun postWXEvent(type: WXEvent) =
        postWXEvent<WXEvent, Nothing>(type, null)

    @Keep
    fun postWXEvent(type: String) =
        postWXEvent<String, Nothing>(type, null)

    @Keep
    fun <D : Any?> postWXEvent(type: WXEvent, data: D?) =
        postWXEvent<WXEvent, D>(type, data)

    @Keep
    fun <D : Any?> postWXEvent(type: String, data: D?) =
        postWXEvent<String, D>(type, data)

    @Keep
    fun <T, D : Any?> postWXEvent(type: T, data: D?) =
        postWXEvent<T, D?>(WXEventHandler<T, D?>(type, data))

    @Keep
    fun <T, D : Any?> postWXEvent(event: WXEventHandler<T, D?>) {
        if (!isReadyForWebOps()) {
            Log.w(TAG, "postWXEvent skipped, not ready")
            return
        }
        val activity = context.findActivity()
        if (activity == null) {
            console.error("[$TAG] Activity/WebView not available for postEvent")
            return
        }
        val type = event.getType()
        val data = event.data
        val newEvent = WXRawEvent(type = type, data = data)
        val adapter = moshi.adapter(WXRawEvent::class.java)
        val jsonPayload = try {
            adapter.toJson(newEvent)
        } catch (e: Exception) {
            console.error("[$TAG] Failed to serialize WXEventHandler: ${e.message}")
            return
        }
        options { postMessage(jsonPayload) }
    }

    @UiThread
    override fun runJs(script: String, callback: ValueCallback<String>?) {
        if (!isReadyForWebOps()) {
            Log.w(TAG, "runJs skipped, not ready"); return
        }
        super.runJs(script, callback)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        Log.d(TAG, "WebUIView detached from window")
    }

    open fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        interfaces.forEach { inst -> inst.instance.onActivityResult(requestCode, resultCode, data) }
    }

    open fun onActivityResult(result: ActivityResult) {
        interfaces.forEach { inst -> inst.instance.onActivityResult(result) }
    }

    open fun onActivityResumeInterfaces() {
        interfaces.forEach { it.instance.onActivityResume() }
    }

    open fun onActivityDestroyInterfaces() {
        interfaces.forEach { it.instance.onActivityDestroy() }
    }

    open fun onActivityPauseInterfaces() {
        interfaces.forEach { it.instance.onActivityPause() }
    }

    open fun onActivityStopInterfaces() {
        interfaces.forEach { it.instance.onActivityStop() }
    }

    open fun removeAllJavaScriptInterfaces() {
        for (obj in interfaces) {
            removeJavascriptInterface(obj.name)
        }
    }

    override fun destroy() {
        stopLoading()
        clearHistory()
        initJob?.cancel()
        removeAllJavaScriptInterfaces()
        isDestroyed = true
        Log.d(TAG, "WebUIView destroyed")
        super.destroy()
    }

    open fun loadDomain() {
        if (!isReadyForWebOps()) {
            Log.w(TAG, "loadDomain skipped, not ready"); return
        }
        try {
            loadUrl("${options.domain}/index.html")
        } catch (t: Throwable) {
            Log.e(TAG, "loadDomain error", t)
        }
    }

    override fun addJavascriptInterface(obj: JavaScriptInterfaceImplementation<out com.dergoogler.mmrl.hybridwebui.interfaces.JavaScriptInterface>) {
        try {
            val js = obj.createNewWX(createDefaultWxOptions(options))
            if (js == null) {
                Log.e(TAG, "Couldn't create new JavaScript interface. Interface was null.")
                return
            }

            super.addJavascriptInterface(js)
        } catch (e: Exception) {
            throw BrickException(
                message = "Couldn't add a new JavaScript interface.",
                cause = e,
            )
        }
    }

    companion object {
        private const val TAG = "WebUIView"
    }
}