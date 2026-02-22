package com.dergoogler.mmrl.webui.view

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.util.Log
import android.view.ViewGroup.LayoutParams
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
import com.dergoogler.mmrl.ext.exception.BrickException
import com.dergoogler.mmrl.ext.findActivity
import com.dergoogler.mmrl.ext.moshi.moshi
import com.dergoogler.mmrl.hybridwebui.HybridWebUI
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
    override fun runJs(script: String) {
        if (!isReadyForWebOps()) {
            Log.w(TAG, "runJs skipped, not ready"); return
        }
        super.runJs(script)
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
        super.destroy()
        Log.d(TAG, "WebUIView destroyed")
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

    @SuppressLint("JavascriptInterface")
    override fun addJavascriptInterface(obj: Any, name: String) {
        if (obj !is WXInterface) {
            Log.d("WebUIView", "$obj is not a WXInterface")
            return
        }
        if (interfaces.any { it.name == name }) {
            Log.w(TAG, "Interface ${obj.name} already exists")
            return
        }
        try {
            super.addJavascriptInterface(obj, name)
            interfaces += JavaScriptInterface.Instance(obj)
            Log.d("WebUIView", "Added interface $name")
        } catch (t: Throwable) {
            Log.e(TAG, "addJavascriptInterface failed", t)
        }
    }

    @CallSuper
    @Throws(BrickException::class)
    @SuppressLint("JavascriptInterface")
    open fun addJavascriptInterface(obj: JavaScriptInterface<out WXInterface>) {
        try {
            val js = obj.createNew(createDefaultWxOptions(options))
            val assetHandlers = js.instance.assetHandlers
            if (assetHandlers.isNotEmpty()) {
                Log.d(TAG, "Adding ${assetHandlers.size} asset handlers")
                this.assetHandlers.addAll(assetHandlers)
            }
            addJavascriptInterface(js.instance, js.name)
        } catch (e: Exception) {
            throw BrickException(
                message = "Couldn't add a new JavaScript interface.",
                cause = e,
            )
        }
    }

    @CallSuper
    @Throws(BrickException::class)
    @SuppressLint("JavascriptInterface")
    inline fun <reified T : WXInterface> addJavascriptInterface(
        initargs: Array<Any>? = null,
        parameterTypes: Array<Class<*>>? = null,
    ) {
        try {
            val interfaceObject: JavaScriptInterface<out WXInterface> = JavaScriptInterface(
                T::class.java,
                initargs,
                parameterTypes,
            )
            addJavascriptInterface(interfaceObject)
        } catch (e: Exception) {
            throw BrickException(
                message = "Couldn't add a new JavaScript interface.",
                cause = e,
            )
        }
    }

    @CallSuper
    @Throws(BrickException::class)
    open fun addJavascriptInterface(vararg obj: JavaScriptInterface<out WXInterface>) {
        obj.forEach { addJavascriptInterface(it) }
    }

    companion object {
        private const val TAG = "WebUIView"
    }
}