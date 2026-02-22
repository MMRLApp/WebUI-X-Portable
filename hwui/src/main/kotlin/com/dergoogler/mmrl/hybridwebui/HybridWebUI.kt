@file:Suppress("unused")

package com.dergoogler.mmrl.hybridwebui

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.util.Log
import android.util.LruCache
import android.view.ViewGroup.LayoutParams
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.widget.RelativeLayout
import androidx.annotation.CallSuper
import androidx.annotation.RequiresFeature
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.dergoogler.mmrl.hybridwebui.HybridWebUIInsets.Companion.toWebUIInsets
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.net.URLConnection
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.Locale

@SuppressLint("SetJavaScriptEnabled", "ViewConstructor")
open class HybridWebUI : WebView {
    var uri: Uri
    protected val pathMatchers: MutableList<PathMatcher> = mutableListOf()
    private var onInsetsEvent: OnInsetsEvent? = null
    private val insetsCache = object : LruCache<String, HybridWebUIInsets>(16) {}

    constructor(context: Context, uri: Uri) : super(context) {
        this.uri = uri
        setup()
    }

    constructor(context: Context, url: String) : this(
        context, url.toUri()
    ) {
        setup()
    }

    @CallSuper
    protected open fun setup() {
        overScrollMode = OVER_SCROLL_NEVER

        layoutParams = LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT,
            RelativeLayout.LayoutParams.MATCH_PARENT
        )

        setupInsets()

        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.allowFileAccess = false
        settings.blockNetworkLoads = false

        webViewClient = HybridWebUIClient(pathMatchers)
    }

    val areInsetsAvailable get() = insetsCache.size() != 0 && insetsCache.get("insets") is HybridWebUIInsets

    fun interface OnInsetsEvent {
        fun onApply(view: HybridWebUI, insets: HybridWebUIInsets)
    }

    fun onInsets(event: OnInsetsEvent) {
        this.onInsetsEvent = event
        // if setup() already ran earlier, we need to attach the listener now
        setupInsets()
    }

    protected fun setupInsets() {
        // if no listener yet, nothing to do (this can happen during construction)
        if (onInsetsEvent == null) {
            return
        }

        // Check if the insets cache is not empty to retrieve a cached value
        if (insetsCache.size() != 0) {
            val value = insetsCache.get("insets") ?: HybridWebUIInsets.Empty
            onInsetsEvent?.onApply(this@HybridWebUI, value)
            return
        }

        onInsetsEvent?.let {
            ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
                val inset = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                val webUiInsets =
                    inset.toWebUIInsets(context.resources.displayMetrics.density).also { sets ->
                        insetsCache.put("insets", sets)
                    }

                it.onApply(this@HybridWebUI, webUiInsets)
                WindowInsetsCompat.CONSUMED
            }
        }
    }

    fun loadPage() {
        super.loadUrl(uri.toString())
    }

    interface PathHandler {
        @WorkerThread
        fun handle(request: HybridWebUIResourceRequest): WebResourceResponse?
    }

    @UiThread
    open fun runJs(script: String) {
        post {
            try {
                evaluateJavascript(script, null)
            } catch (t: Throwable) {
                Log.e(TAG, "Exception evaluating JS", t)
            }
        }
    }

    abstract class BasePathHandler : PathHandler {
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

    fun addPathHandler(path: String, handler: PathHandler, authority: Uri = uri) {
        pathMatchers.add(PathMatcher(authority, path, false, handler))
    }

    /**
     * Event listeners needs to be added before the webpage loads!
     */
    @SuppressLint("RequiresFeature")
    fun addEventListener(objectName: String, event: EventListener) {
        WebViewCompat.addWebMessageListener(
            this,
            objectName,
            setOf("*")
        ) { view, message, uri, isMainFrame, reply ->
            try {
                val newEvent = HybridWebUIEvent(view, message, reply, uri, isMainFrame)
                event.listen(newEvent)
            } catch (e: Exception) {
                Log.e(TAG, "Error handling event", e)
            }
        }
    }

    @SuppressLint("RequiresFeature")
    fun removeEventListener(objectName: String) {
        WebViewCompat.removeWebMessageListener(this, objectName)
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

    interface EventListener {
        @UiThread
        @SuppressLint("RequiresFeature")
        @RequiresFeature(
            name = WebViewFeature.WEB_MESSAGE_LISTENER,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported"
        )
        fun listen(event: HybridWebUIEvent)
    }

    companion object {
        const val TAG = "HybridWebUI"
    }
}