package com.dergoogler.mmrl.webui.view

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.util.Log
import android.view.ViewGroup.LayoutParams
import android.webkit.WebMessage
import android.webkit.WebView
import android.widget.FrameLayout
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
import com.dergoogler.mmrl.webui.interfaces.WXConsole
import com.dergoogler.mmrl.webui.interfaces.WXInterface
import com.dergoogler.mmrl.webui.interfaces.WXOptions
import com.dergoogler.mmrl.webui.model.JavaScriptInterface
import com.dergoogler.mmrl.webui.model.WXEvent
import com.dergoogler.mmrl.webui.model.WXEventHandler
import com.dergoogler.mmrl.webui.model.WXRawEvent
import com.dergoogler.mmrl.webui.util.WebUIOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.text.replace

/**
 * A custom WebView class that provides additional functionality for WebUI.
 *
 * This class extends the base WebView and adds features such as:
 * - Options management using [WebUIOptions].
 * - Simplified message posting to the WebView.
 * - Event handling for WebUI events.
 * - Utility functions for running JavaScript code and handling errors.
 * - Console logging integration.
 *
 * @property options The options for this WebUIView.
 * @property interfaces A set of JavaScript interface names that have been added to this WebView.
 * @property console A [WXConsole] implementation for logging messages from the WebView.
 */
@Keep
@SuppressLint("ViewConstructor")
open class WebUIView(
    protected val options: WebUIOptions,
) : WebView(options.context) {
    private val scope = CoroutineScope(Dispatchers.Main)
    protected var initJob: Job? = null
    private var isInitialized = false
    internal var mSwipeView: WXSwipeRefresh? = null

    init {
        setWebContentsDebuggingEnabled(options.debug)
        initWhenReady()
    }

    constructor(context: Context) : this(WebUIOptions(context = context)) {
        throw UnsupportedOperationException("Default constructor not supported. Use constructor with options.")
    }

    constructor(context: Context, attrs: AttributeSet) : this(WebUIOptions(context = context)) {
        throw UnsupportedOperationException("Default constructor not supported. Use constructor with options.")
    }

    constructor(
        context: Context,
        attrs: AttributeSet,
        defStyle: Int,
    ) : this(WebUIOptions(context = context)) {
        throw UnsupportedOperationException("Default constructor not supported. Use constructor with options.")
    }

    protected fun createDefaultWxOptions(options: WebUIOptions): WXOptions = WXOptions(
        webView = this,
        options = options
    )

    protected val interfaces = hashSetOf<JavaScriptInterface.Instance>()

    protected open fun onInit(isInitialized: Boolean) {}

    private fun initWhenReady() {
        // Basic setup that can run immediately
        layoutParams = LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )

        // Delay full initialization until view is properly attached
        doOnAttach {
            initJob = scope.launch {
                // Wait for first frame to ensure Activity is ready
                withContext(Dispatchers.Main) { awaitFrame() }
                initView()
                onInit(isInitialized)
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initView() {
        if (isInitialized) return

        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = false
        }

        // Background and styling
        with(options) {
            setBackgroundColor(colorScheme.background.toArgb())
            background = colorScheme.background.toArgb().toDrawable()
        }

        // JavaScript interfaces (delayed until WebView is fully ready)
        post {
            isInitialized = true
        }
    }

    fun <R> options(block: WebUIOptions.() -> R): R? {
        return try {
            block(options)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get options:", e)
            null
        }
    }

    fun postMessage(message: String) {
        val uri = /* options.domain */ "*".toUri()

        if (WebViewFeature.isFeatureSupported(WebViewFeature.POST_WEB_MESSAGE)) {
            val compatMessage = WebMessageCompat(message)
            WebViewCompat.postWebMessage(
                this,
                compatMessage,
                uri
            )
        } else {
            val baseMessage = WebMessage(message)
            super.postWebMessage(baseMessage, uri)
        }
    }

    /**
     * Posts a [WXEvent] to the WebView.
     *
     * @param type The type of the event.
     */
    @Keep
    fun postWXEvent(type: WXEvent) =
        postWXEvent<WXEvent, Nothing>(type, null)

    /**
     * Posts an event with the given [type] string to the WebView.
     *
     * @param type The type of the event.
     */
    @Keep
    fun postWXEvent(type: String) =
        postWXEvent<String, Nothing>(type, null)

    /**
     * Posts a [WXEvent] with the given [data] to the WebView.
     *
     * @param type The type of the event.
     * @param data The data to be sent with the event.
     * @param D The type of the data.
     */
    @Keep
    fun <D : Any?> postWXEvent(type: WXEvent, data: D?) =
        postWXEvent<WXEvent, D>(type, data)

    /**
     * Posts an event with the given [type] string and [data] to the WebView.
     *
     * @param type The type of the event.
     * @param data The data to be sent with the event.
     * @param D The type of the data.
     */
    @Keep
    fun <D : Any?> postWXEvent(type: String, data: D?) =
        postWXEvent<String, D>(type, data)

    /**
     * Posts an event with the given [type] and [data] to the WebView.
     * This is a generic function that can be used to post any type of event.
     *
     * @param type The type of the event.
     * @param data The data to be sent with the event.
     * @param T The type of the event type.
     * @param D The type of the data.
     */
    @Keep
    fun <T, D : Any?> postWXEvent(type: T, data: D?) =
        postWXEvent<T, D?>(WXEventHandler<T, D?>(type, data))

    /**
     * Posts the given [WXEventHandler] event to the WebView.
     *
     * It serializes the event to JSON and sends it as a message to the WebView.
     * If the activity or WebView is not available, or if serialization fails,
     * an error is logged and the event is not posted.
     *
     * @param event The event to be posted.
     * @param T The type of the event type.
     * @param D The type of the data.
     */
    @Keep
    fun <T, D : Any?> postWXEvent(event: WXEventHandler<T, D?>) {
        val activity = context.findActivity()
        if (activity == null) {
            console.error("[$TAG] Activity/WebView not available for postEvent")
            return
        }

        val type = event.getType()
        val data = event.data

        val newEvent = WXRawEvent(
            type = type,
            data = data
        )

        val adapter = moshi.adapter(WXRawEvent::class.java)

        val jsonPayload = try {
            adapter.toJson(newEvent)
        } catch (e: Exception) {
            console.error("[$TAG] Failed to serialize WXEventHandler: ${e.message}")
            return
        }

        options {
            postMessage(jsonPayload)
        }
    }

    @UiThread
    fun runJs(script: String) {
        post { evaluateJavascript(script, null) }
    }

    open fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
    ) {
        interfaces.forEach { inst ->
            inst.instance.onActivityResult(requestCode, resultCode, data)
        }
    }

    open fun onActivityResumeInterfaces() {
        interfaces.forEach { inst ->
            inst.instance.onActivityResume()
        }
    }

    open fun onActivityDestroyInterfaces() {
        interfaces.forEach { inst ->
            inst.instance.onActivityDestroy()
        }
    }

    open fun onActivityPauseInterfaces() {
        interfaces.forEach { inst ->
            inst.instance.onActivityPause()
        }
    }

    open fun onActivityStopInterfaces() {
        interfaces.forEach { inst ->
            inst.instance.onActivityStop()
        }
    }

    @CallSuper
    override fun destroy() {
        stopLoading()
        clearHistory()

        initJob?.cancel()

        // remove all interfaces
        for (obj in interfaces) {
            removeJavascriptInterface(obj.name)
        }

        super.destroy()
    }

    open fun loadDomain() {
        this.loadUrl("${options.domain}/index.html")
    }

    /**
     * # DO NOT USE
     */
    @CallSuper
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


        super.addJavascriptInterface(obj, name)

        interfaces += JavaScriptInterface.Instance(obj)

        Log.d("WebUIView", "Added interface $name")
    }

    /**
     * Adds a JavaScript interface to the WebView.
     *
     * This function takes a [JavaScriptInterface] object, creates a new instance of it
     * using the provided [WXOptions], and then adds it to the WebView using the
     * [addJavascriptInterface] method.
     *
     * @param obj The [JavaScriptInterface] object to add.
     * @throws BrickException if an error occurs while adding the interface.
     */
    @Throws(BrickException::class)
    @SuppressLint("JavascriptInterface")
    fun addJavascriptInterface(obj: JavaScriptInterface<out WXInterface>) {
        try {
            val js = obj.createNew(createDefaultWxOptions(options))
            addJavascriptInterface(js.instance, js.name)
        } catch (e: Exception) {
            throw BrickException(
                message = "Couldn't add a new JavaScript interface.",
                cause = e,
            )
        }
    }

    /**
     * Adds a JavaScript interface to this WebView.
     *
     * This function simplifies the process of adding JavaScript interfaces by allowing you to
     * directly specify the interface class [T] and optionally provide constructor arguments
     * and parameter types.
     *
     * @param T The type of the JavaScript interface, which must extend [WXInterface].
     * @param initargs An optional array of arguments to be passed to the constructor of the interface.
     * @param parameterTypes An optional array of parameter types for the constructor of the interface.
     * @throws BrickException if an error occurs while adding the interface.
     */
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
                parameterTypes
            )

            addJavascriptInterface(interfaceObject)
        } catch (e: Exception) {
            throw BrickException(
                message = "Couldn't add a new JavaScript interface.",
                cause = e,
            )
        }
    }

    /**
     * Adds multiple JavaScript interfaces to this WebView.
     *
     * This function iterates over the provided JavaScript interfaces and adds each one
     * to the WebView using the [addJavascriptInterface] method.
     *
     * @param obj A vararg of [JavaScriptInterface] objects to be added.
     * @throws BrickException If an error occurs while adding any of the JavaScript interfaces.
     * @see addJavascriptInterface
     */
    @Throws(BrickException::class)
    fun addJavascriptInterface(vararg obj: JavaScriptInterface<out WXInterface>) {
        obj.forEach { addJavascriptInterface(it) }
    }

    /**
     * A [WXConsole] implementation for logging messages from the WebView.
     *
     * This object provides methods for logging messages at different levels (error, info, log, warn)
     * by executing corresponding JavaScript `console` commands in the WebView.
     * It also handles escaping special characters in messages and arguments to prevent JavaScript errors.
     */
    val console = object : WXConsole {
        private val String.escape get() = this.replace("'", "\\'")

        private fun levelParser(level: String, message: String, vararg args: String?) =
            runJs(
                "console.$level('${message.escape}'${
                    args.joinToString(
                        prefix = if (args.isNotEmpty()) ", " else "",
                        separator = ", "
                    ) { "'${it?.escape}'" }
                })")

        override fun error(throwable: Throwable) {
            val errorString = "Error('${throwable.message?.replace("'", "\\'")}', { cause: '${
                throwable.cause.toString().replace("'", "\\'")
            }' })"

            runJs("console.error($errorString)")
        }

        override fun trace(message: String) {
            if (options.debug) levelParser("trace", message)
        }

        override fun error(message: String, vararg args: String?) =
            levelParser("error", message, *args)

        override fun info(message: String, vararg args: String?) =
            levelParser("info", message, *args)

        override fun log(message: String, vararg args: String?) =
            levelParser("log", message, *args)

        override fun warn(message: String, vararg args: String?) =
            levelParser("warn", message, *args)
    }

    companion object {
        private const val TAG = "WebUIView"
    }
}