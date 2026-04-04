@file:Suppress("unused")

package com.dergoogler.mmrl.hybridwebui

import android.annotation.SuppressLint
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import android.util.AttributeSet
import android.util.Log
import android.view.ViewGroup.LayoutParams
import android.webkit.ValueCallback
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.widget.RelativeLayout
import androidx.activity.ComponentActivity
import androidx.annotation.CallSuper
import androidx.annotation.RequiresFeature
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.dergoogler.mmrl.hybridwebui.HybridWebUIInsets.Companion.toWebUIInsets
import com.dergoogler.mmrl.hybridwebui.event.WebConsoleEvent
import com.dergoogler.mmrl.hybridwebui.interfaces.JavaScriptInterface
import com.dergoogler.mmrl.hybridwebui.interfaces.JavaScriptInterfaceImplementation
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.net.URLConnection
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.Locale

open class HybridWebUI : WebView {
    var uri: Uri
    private lateinit var _store: HybridWebUIStore
    private lateinit var _activity: ComponentActivity
    private var _applicationContext: Context? = null
    private var onReadyCallback: OnReady? = null

    constructor(context: Context, uri: Uri) : super(context) {
        this.uri = uri
        setup()
    }

    constructor(context: Context, url: String) : this(
        context, url.toUri()
    )

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        uri = attrs.getAttributeValue(null, "uri").toUri()
        setup()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        uri = attrs.getAttributeValue(null, "uri").toUri()
        setup()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        setupDependencies()
    }

    private fun setupDependencies() {
        if (!isActivityInitialized) {
            val discoveredActivity = findActivity()
            if (discoveredActivity != null) {
                // Validate the activity is not finishing or destroyed
                if (!discoveredActivity.isFinishing && !discoveredActivity.isDestroyed) {
                    _activity = discoveredActivity
                    // Cache application context early to avoid null issues later
                    _applicationContext = try {
                        discoveredActivity.applicationContext
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to get application context from activity", e)
                        context.applicationContext
                    }
                } else {
                    Log.e(TAG, "Found ComponentActivity but it's finishing or destroyed")
                    return
                }
            } else {
                Log.e(TAG, "Could not find ComponentActivity in the view context hierarchy")
                return
            }
        }

        val owner = findViewTreeViewModelStoreOwner()
            ?: _activity as? androidx.lifecycle.ViewModelStoreOwner

        if (owner == null) {
            Log.e(TAG, "No ViewModelStoreOwner found.")
            return
        }

        if (!isStoreInitialized) {
            _store = ViewModelProvider(owner)[HybridWebUIStore::class.java]
        }

        if (isStoreInitialized && isActivityInitialized) {
            if (_activity.window != null) {
                onReady(_store)
                onReadyCallback?.invoke(_store)
            } else {
                post {
                    if (isActivityInitialized && _activity.window != null) {
                        onReady(_store)
                        onReadyCallback?.invoke(_store)
                    }
                }
            }
        }
    }

    /**
     * Optimized Activity discovery helper.
     * Fixed: Added 'return' to stop the loop once the Activity is found.
     * Enhanced: Added null checks and validation to prevent crashes.
     */
    private fun findActivity(): ComponentActivity? {
        var context = this.context
        var depth = 0
        val maxDepth = 10 // Prevent infinite loops

        while (context is ContextWrapper && depth < maxDepth) {
            if (context is ComponentActivity) {
                return try {
                    // Validate the activity before returning
                    if (!context.isFinishing && !context.isDestroyed) {
                        context
                    } else {
                        Log.w(TAG, "Found ComponentActivity but it's finishing or destroyed")
                        null
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Exception while validating ComponentActivity", e)
                    null
                }
            }
            context = try {
                context.baseContext
            } catch (e: Exception) {
                Log.w(TAG, "Exception while getting baseContext", e)
                break
            }
            depth++
        }

        if (depth >= maxDepth) {
            Log.w(TAG, "Reached maximum depth while searching for ComponentActivity")
        }

        return null
    }

    val store: HybridWebUIStore
        get() = if (isStoreInitialized) _store
        else throw IllegalStateException("Store accessed before onStoreReady()")

    val activity: ComponentActivity
        get() = if (isActivityInitialized) _activity
        else throw IllegalStateException("Activity accessed before onStoreReady()")

    open val consoleLogs get() = store.buildConsoleStore(TAG)

    /**
     * Safe application context getter that provides fallbacks to prevent null crashes.
     */
    val safeApplicationContext: Context
        get() = _applicationContext
            ?: try {
                if (isActivityInitialized && !_activity.isFinishing && !_activity.isDestroyed) {
                    _activity.applicationContext
                } else {
                    context.applicationContext
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to get application context, using view context", e)
                context
            }

    /**
     * Safe base context getter with proper validation.
     */
    val safeBaseContext: Context?
        get() = try {
            if (isActivityInitialized && !_activity.isFinishing && !_activity.isDestroyed) {
                _activity.baseContext
            } else {
                (context as? ContextWrapper)?.baseContext
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get base context", e)
            null
        }

    @CallSuper
    protected open fun onReady(store: HybridWebUIStore) {
        webViewClient = HybridWebUIClient(this)
        webChromeClient = HybridWebUIChromeClient(this)

        // addEventListener("SaveFileLauncher", SaveFileLauncherEvent())
        addEventListener("__hw_web_console_internal__", WebConsoleEvent())
    }

    fun onReady(callback: OnReady) {
        onReadyCallback = callback
    }

    @SuppressLint("SetJavaScriptEnabled")
    @CallSuper
    protected open fun setup() {
        overScrollMode = OVER_SCROLL_NEVER

        id = R.id.hybridwebview

        layoutParams = LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT,
            RelativeLayout.LayoutParams.MATCH_PARENT
        )

        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.allowFileAccess = false
        settings.blockNetworkLoads = false
    }

    private var _insets: HybridWebUIInsets? = null
        get() {
            if (field != null) return field
            val rootInsets = ViewCompat.getRootWindowInsets(this)
                ?.getInsets(WindowInsetsCompat.Type.systemBars())
                ?: return null
            field = rootInsets.toWebUIInsets(context.resources.displayMetrics.density)
            return field
        }

    val areInsetsAvailable get() = _insets != null
    val insets get() = _insets ?: HybridWebUIInsets.Empty

    fun loadPage(path: String = "") {
        super.loadUrl("$uri$path")
    }

    @UiThread
    open fun runJs(script: String, callback: ValueCallback<String>? = null) {
        post {
            try {
                evaluateJavascript(script, callback)
            } catch (t: Throwable) {
                Log.e(TAG, "Exception evaluating JS", t)
            }
        }
    }

    abstract class PathHandler {
        @Deprecated("This will take no future effect")
        @WorkerThread
        open fun handle(request: HybridWebUIResourceRequest): WebResourceResponse? = null

        @WorkerThread
        open fun handle(
            view: HybridWebUI,
            request: HybridWebUIResourceRequest,
        ): WebResourceResponse? = null

        companion object {
            enum class ResponseStatus(
                val code: Int,
                val reasonPhrase: String,
            ) {
                // --- 1xx Informational ---
                CONTINUE(100, "Continue"),
                SWITCHING_PROTOCOLS(101, "Switching Protocols"),
                PROCESSING(102, "Processing"),

                // --- 2xx Success ---
                OK(200, "OK"),
                CREATED(201, "Created"),
                ACCEPTED(202, "Accepted"),
                NO_CONTENT(204, "No Content"),
                PARTIAL_CONTENT(206, "Partial Content"),

                // --- 3xx Redirection ---
                MOVED_PERMANENTLY(301, "Moved Permanently"),
                FOUND(302, "Found"),
                NOT_MODIFIED(304, "Not Modified"),
                TEMPORARY_REDIRECT(307, "Temporary Redirect"),
                PERMANENT_REDIRECT(308, "Permanent Redirect"),

                // --- 4xx Client Error ---
                BAD_REQUEST(400, "Bad Request"),
                UNAUTHORIZED(401, "Unauthorized"),
                FORBIDDEN(403, "Forbidden"),
                NOT_FOUND(404, "Not Found"),
                METHOD_NOT_ALLOWED(405, "Method Not Allowed"),
                CONFLICT(409, "Conflict"),
                GONE(410, "Gone"),
                UNSUPPORTED_MEDIA_TYPE(415, "Unsupported Media Type"),
                TOO_MANY_REQUESTS(429, "Too Many Requests"),

                // --- 5xx Server Error ---
                INTERNAL_SERVER_ERROR(500, "Internal Server Error"),
                NOT_IMPLEMENTED(501, "Not Implemented"),
                BAD_GATEWAY(502, "Bad Gateway"),
                SERVICE_UNAVAILABLE(503, "Service Unavailable"),
                GATEWAY_TIMEOUT(504, "Gateway Timeout"),

                // --- Fallback ---
                UNKNOWN(-1, "Unknown Status");

                val isInformational: Boolean get() = code in 100..199
                val isSuccess: Boolean get() = code in 200..299
                val isRedirection: Boolean get() = code in 300..399
                val isClientError: Boolean get() = code in 400..499
                val isServerError: Boolean get() = code in 500..599

                companion object {
                    private val codeMap = ResponseStatus.entries.associateBy { it.code }

                    fun fromCode(code: Int): ResponseStatus {
                        return codeMap[code] ?: UNKNOWN
                    }
                }
            }
        }

        protected val defaultEncoding = "UTF-8"
        protected val defaultHeaders = mapOf(
            "Client-Via" to "MMRL",
            "Access-Control-Allow-Origin" to "*",
        )

        protected fun response(
            mimeType: String? = "text/plain",
            encoding: String = defaultEncoding,
            status: ResponseStatus = ResponseStatus.OK,
            headers: Map<String, String> = defaultHeaders,
            data: InputStream?,
        ) = response(
            mimeType,
            encoding,
            status.code,
            status.reasonPhrase,
            headers,
            data
        )

        protected fun response(
            mimeType: String? = "text/plain",
            encoding: String = defaultEncoding,
            statusCode: Int = 200,
            reasonPhrase: String = "OK",
            headers: Map<String, String> = defaultHeaders,
            data: InputStream?,
        ) = WebResourceResponse(
            mimeType,
            encoding,
            statusCode,
            reasonPhrase,
            headers,
            data
        )

        protected fun String.toByteArrayInputStream(charset: Charset = StandardCharsets.UTF_8) =
            ByteArrayInputStream(this.toByteArray(charset))

        protected fun String.asStyleResponse(): WebResourceResponse {
            return response(
                mimeType = "text/css",
                data = this.toByteArrayInputStream()
            )
        }

        protected fun String.asScriptResponse(): WebResourceResponse {
            return response(
                mimeType = "text/javascript",
                data = this.toByteArrayInputStream()
            )
        }

        protected val notFoundResponse
            get() = response(
                mimeType = null,
                status = ResponseStatus.NOT_FOUND,
                data = null
            )

        protected val forbiddenResponse
            get() = response(
                mimeType = null,
                status = ResponseStatus.FORBIDDEN,
                data = null
            )
    }

    val isStoreInitialized get() = ::_store.isInitialized
    val isActivityInitialized get() = ::_activity.isInitialized

    /**
     * Check if the activity is in a safe state to use.
     */
    val isActivitySafe: Boolean
        get() = try {
            isActivityInitialized && !_activity.isFinishing && !_activity.isDestroyed
        } catch (e: Exception) {
            Log.w(TAG, "Exception checking activity state", e)
            false
        }

    fun addPathHandler(
        path: String,
        handler: PathHandler,
        authority: Uri = "${uri.scheme}://${uri.authority}".toUri(),
    ) {
        if (!isStoreInitialized) {
            Log.e(TAG, "Store not initialized")
            return
        }
        _store.pathMatchers.add(PathMatcher(authority, path, false, handler))
    }

    fun addEventListener(objectName: String, event: EventListener) {
        addEventListener(objectName, event, setOf("${uri.scheme}://${uri.authority}"))
    }

    /**
     * Event listeners needs to be added before the webpage loads!
     */
    @SuppressLint("RequiresFeature")
    fun addEventListener(
        objectName: String,
        event: EventListener,
        allowedOriginRules: Set<String>,
    ) {
        try {
            WebViewCompat.addWebMessageListener(
                this,
                objectName,
                setOf("*")
            ) { view, message, uri, isMainFrame, reply ->
                try {
                    val newEvent = HybridWebUIEvent(view, message, reply, uri, isMainFrame)
                    event.listen(this, newEvent)
                } catch (e: Exception) {
                    Log.e(TAG, "Error handling event", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error adding event listener", e)
        }
    }

    @SuppressLint("RequiresFeature")
    fun removeEventListener(objectName: String) {
        try {
            WebViewCompat.removeWebMessageListener(this, objectName)
        } catch (e: Exception) {
            Log.e(TAG, "Error removing event listener", e)
        }
    }

    @SuppressLint("JavascriptInterface")
    override fun addJavascriptInterface(obj: Any, name: String) {
        if (!isStoreInitialized) {
            Log.e(TAG, "Store not initialized")
            return
        }

        if (obj !is JavaScriptInterface) {
            Log.d(TAG, "$obj is not a WXInterface")
            return
        }

        if (store.jsInterfaceStore.has(name)) {
            Log.w(TAG, "Interface ${obj.name} already exists")
            return
        }
        try {
            super.addJavascriptInterface(obj, name)
            store.jsInterfaceStore += JavaScriptInterfaceImplementation.Instance(obj)
            Log.d(TAG, "Added interface $name")
        } catch (t: Throwable) {
            Log.e(TAG, "addJavascriptInterface failed", t)
        }
    }

    @Throws(IllegalStateException::class)
    @SuppressLint("JavascriptInterface")
    open fun addJavascriptInterface(obj: JavaScriptInterfaceImplementation<out JavaScriptInterface>) {
        try {
            val js = obj.createNew(activity, this)

            if (js == null) {
                Log.e(TAG, "Couldn't create new JavaScript interface. Interface was null.")
                return
            }

            addJavascriptInterface(js.instance, js.name)
        } catch (e: Exception) {
            throw IllegalStateException("Couldn't add a new JavaScript interface.")
        }
    }

    @Throws(IllegalStateException::class)
    @SuppressLint("JavascriptInterface")
    open fun addJavascriptInterface(obj: JavaScriptInterfaceImplementation.Instance) {
        try {
            addJavascriptInterface(obj.instance, obj.name)
        } catch (e: Exception) {
            throw IllegalStateException("Couldn't add a new JavaScript interface.")
        }
    }

    @Throws(IllegalStateException::class)
    @SuppressLint("JavascriptInterface")
    inline fun <reified T : JavaScriptInterface> addJavascriptInterface(
        initArgs: Array<Any>? = null,
        parameterTypes: Array<Class<*>>? = null,
    ) {
        try {
            val implementation = JavaScriptInterface(T::class.java, initArgs, parameterTypes)
            addJavascriptInterface(implementation)
        } catch (e: Exception) {
            throw IllegalStateException("Couldn't add a new JavaScript interface.")
        }
    }

    @Throws(IllegalStateException::class)
    open fun addJavascriptInterface(vararg obj: JavaScriptInterfaceImplementation<out JavaScriptInterface>) {
        obj.forEach { addJavascriptInterface(it) }
    }

    class PathMatcher(
        val mUri: Uri,
        val mPath: String,
        val mHttpEnabled: Boolean,
        val mHandler: PathHandler,
    ) {
        companion object {
            const val HTTP_SCHEME = "http"
            const val HTTPS_SCHEME = "https"
        }

        init {
            require(mPath.isNotEmpty() && mPath[0] == '/') { "Path should start with a slash '/'." }
            require(mPath.endsWith("/")) { "Path should end with a slash '/'" }
        }

        @WorkerThread
        fun match(uri: Uri): PathHandler? {
            if (uri.scheme == HTTP_SCHEME && !mHttpEnabled) {
                return null
            }
            if (uri.scheme != HTTP_SCHEME && uri.scheme != HTTPS_SCHEME) {
                return null
            }

            if (uri.authority != mUri.authority) {
                return null
            }

            if (!uri.path.orEmpty().startsWith(mPath)) {
                return null
            }

            return mHandler
        }

        @WorkerThread
        fun getSuffixPath(path: String): String {
            return path.replaceFirst(mPath, "")
        }
    }

    object MimeType {
        fun getMimeFromFileName(fileName: String?): String? {
            if (fileName == null) {
                return null
            }

            val mimeType = URLConnection.guessContentTypeFromName(fileName)

            if (mimeType != null) {
                return mimeType
            }

            return guessHardcodedMime(fileName)
        }

        private fun guessHardcodedMime(fileName: String): String? {
            val finalFullStop = fileName.lastIndexOf('.')
            if (finalFullStop == -1) {
                return null
            }

            val extension = fileName.substring(finalFullStop + 1).lowercase(Locale.getDefault())

            return when (extension) {
                "webm" -> "video/webm"
                "mpeg", "mpg" -> "video/mpeg"
                "mp3" -> "audio/mpeg"
                "wasm" -> "application/wasm"
                "xhtml", "xht", "xhtm" -> "application/xhtml+xml"
                "flac" -> "audio/flac"
                "ogg", "oga", "opus" -> "audio/ogg"
                "wav" -> "audio/wav"
                "m4a" -> "audio/x-m4a"
                "gif" -> "image/gif"
                "jpeg", "jpg", "jfif", "pjpeg", "pjp" -> "image/jpeg"
                "png" -> "image/png"
                "apng" -> "image/apng"
                "svg", "svgz" -> "image/svg+xml"
                "webp" -> "image/webp"
                "mht", "mhtml" -> "multipart/related"
                "css" -> "text/css"
                "html", "htm", "shtml", "shtm", "ehtml" -> "text/html"
                "js", "mjs" -> "application/javascript"
                "xml" -> "text/xml"
                "mp4", "m4v" -> "video/mp4"
                "ogv", "ogm" -> "video/ogg"
                "ico" -> "image/x-icon"
                "woff" -> "application/font-woff"
                "gz", "tgz" -> "application/gzip"
                "json" -> "application/json"
                "pdf" -> "application/pdf"
                "zip" -> "application/zip"
                "bmp" -> "image/bmp"
                "tiff", "tif" -> "image/tiff"
                else -> null
            }
        }
    }

    /**
     * A [HybridWebUIConsole] implementation for logging messages from the WebView.
     *
     * This object provides methods for logging messages at different levels (error, info, log, warn)
     * by executing corresponding JavaScript `console` commands in the WebView.
     * It also handles escaping special characters in messages and arguments to prevent JavaScript errors.
     */
    val console = object : HybridWebUIConsole {
        private val String.escape get() = this.replace("'", "\\'")
        private fun levelParser(level: String, message: String, vararg args: String?) =
            runJs(
                "console.$level(`${message.escape}`${
                    args.joinToString(
                        prefix = if (args.isNotEmpty()) ", " else "",
                        separator = ", "
                    ) { "'${it?.escape}'" }
                })")

        override fun error(throwable: Throwable) {
            val errorString = "Error(`${throwable.message?.replace("'", "\\'")}`, { cause: `${
                throwable.cause.toString().replace("'", "\\'")
            }` })"
            runJs("console.error($errorString)")
        }

        override fun trace(message: String) {
            levelParser("trace", message)
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

    abstract class EventListener {
        @UiThread
        @SuppressLint("RequiresFeature")
        @RequiresFeature(
            name = WebViewFeature.WEB_MESSAGE_LISTENER,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported"
        )
        @Deprecated("This will take no future effect")
        open fun listen(event: HybridWebUIEvent) {
        }

        @UiThread
        @SuppressLint("RequiresFeature")
        @RequiresFeature(
            name = WebViewFeature.WEB_MESSAGE_LISTENER,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported"
        )
        open fun listen(view: HybridWebUI, event: HybridWebUIEvent) {
        }
    }

    fun interface OnReady {
        operator fun invoke(store: HybridWebUIStore)
    }

    companion object {
        const val TAG = "HybridWebUI"
    }

    open fun clearState() {
        if (isStoreInitialized) {
            _store.clear()
            _store.jsInterfaceStore.forEach {
                onDestroy()
            }
        }
        _applicationContext = null
    }

    override fun destroy() {
        clearState()
        super.destroy()
    }
}