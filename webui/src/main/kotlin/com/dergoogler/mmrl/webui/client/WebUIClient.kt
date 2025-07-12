package com.dergoogler.mmrl.webui.client

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.util.Log
import android.webkit.JsPromptResult
import android.webkit.JsResult
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import com.dergoogler.mmrl.ext.nullply
import com.dergoogler.mmrl.platform.Platform
import com.dergoogler.mmrl.platform.file.SuFile.Companion.toSuFile
import com.dergoogler.mmrl.ui.component.dialog.ConfirmData
import com.dergoogler.mmrl.ui.component.dialog.PromptData
import com.dergoogler.mmrl.ui.component.dialog.confirm
import com.dergoogler.mmrl.ui.component.dialog.prompt
import com.dergoogler.mmrl.webui.R
import com.dergoogler.mmrl.webui.WXAssetLoader
import com.dergoogler.mmrl.webui.handler.internalPathHandler
import com.dergoogler.mmrl.webui.handler.suPathHandler
import com.dergoogler.mmrl.webui.handler.webrootPathHandler
import com.dergoogler.mmrl.webui.model.Insets
import com.dergoogler.mmrl.webui.model.WebResourceErrors
import com.dergoogler.mmrl.webui.util.WebUIOptions
import com.dergoogler.mmrl.webui.util.errorPages.baseErrorPage
import com.dergoogler.mmrl.webui.util.errorPages.requireNewVersionErrorPage
import com.dergoogler.mmrl.webui.view.WXSwipeRefresh
import com.dergoogler.mmrl.webui.wxAssetLoader
import kotlinx.html.b
import kotlinx.html.br
import kotlinx.html.i
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

open class WXChromeClient(
    private val options: WebUIOptions,
) : WebChromeClient() {
    private companion object {
        const val TAG = "WXChromeClient"
    }

    @OptIn(ExperimentalContracts::class)
    private inline fun options(
        result: JsResult,
        options: WebUIOptions,
        block: WebUIOptions.() -> Unit,
    ): Boolean {
        contract {
            callsInPlace(block, InvocationKind.AT_MOST_ONCE)
        }

        block(options)
        return true
    }

    override fun onJsAlert(
        view: WebView?,
        url: String,
        message: String,
        result: JsResult,
    ): Boolean = options(result, options) {
        context.confirm(
            confirmData = ConfirmData(
                title = context.getString(R.string.says, modId.id),
                description = message,
                onConfirm = { result.confirm() },
                onClose = { result.cancel() }
            ),
            colorScheme = colorScheme
        )
    }

    override fun onJsConfirm(
        view: WebView,
        url: String,
        message: String,
        result: JsResult,
    ): Boolean = options(result, options) {
        context.confirm(
            confirmData = ConfirmData(
                title = context.getString(R.string.says, modId.id),
                description = message,
                onConfirm = { result.confirm() },
                onClose = { result.cancel() }
            ),
            colorScheme = colorScheme
        )
    }

    override fun onJsPrompt(
        view: WebView,
        url: String?,
        message: String?,
        defaultValue: String?,
        result: JsPromptResult,
    ): Boolean = options(result, options) {
        context.prompt(
            promptData = PromptData(
                title = message ?: context.getString(R.string.says, modId.id),
                value = defaultValue ?: "",
                onConfirm = { result.confirm(it) },
                onClose = { result.cancel() }
            ),
            colorScheme = colorScheme
        )
    }
}

open class WXClient : WebViewClient {
    private val mOptions: WebUIOptions
    private val mWxAssetsLoader: WXAssetLoader
    internal var mSwipeView: WXSwipeRefresh? = null

    constructor(options: WebUIOptions, insets: Insets) {
        mOptions = options
        mWxAssetsLoader = wxAssetLoader(
            handlers = buildList {
                add("/mmrl/" to internalPathHandler(mOptions, insets))
                add("/internal/" to internalPathHandler(mOptions, insets))
                add(".${mOptions.modId}/" to suPathHandler("/data/adb/modules/${mOptions.modId}".toSuFile()))
                add("/.adb/" to suPathHandler("/data/adb".toSuFile()))
                add("/.config/" to suPathHandler("/data/adb/.config".toSuFile()))
                add("/.local/" to suPathHandler("/data/adb/.local".toSuFile()))

                if (mOptions.config.hasRootPathPermission) {
                    add("/__root__" to suPathHandler("/".toSuFile()))
                }

                add("/" to webrootPathHandler(mOptions, insets))
            }
        )
    }

    constructor(options: WebUIOptions, assetsLoader: WXAssetLoader) {
        mOptions = options
        mWxAssetsLoader = assetsLoader
    }

    private fun openUri(uri: Uri) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, uri)
            mOptions.context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening URI: $uri", e)
        }
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        mSwipeView.nullply {
            isRefreshing = false
        }
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        mSwipeView.nullply {
            isRefreshing = true
        }
    }

    @SuppressLint("WebViewClientOnReceivedSslError")
    override fun onReceivedSslError(
        view: WebView,
        handler: SslErrorHandler,
        error: SslError,
    ) {
        if (mOptions.debug) {
            handler.proceed()
        } else {
            handler.cancel()
        }
    }

    override fun onReceivedError(
        view: WebView,
        request: WebResourceRequest,
        error: WebResourceError,
    ) {
        mSwipeView.nullply {
            isRefreshing = false
        }

        if (request.isForMainFrame) {
            val errorName = WebResourceErrors.from(error.errorCode)

            val err = mOptions.baseErrorPage(
                title = "Failed to load",
                description = {
                    b { +request.url.toString() }
                    br
                    i { +error.description.toString() }
                },
                tryFollowing = listOf(
                    "Check your internet connection",
                    "Refreshing the page",
                    "Restarting the WebUI X"
                ),
                errorCode = errorName?.name ?: "UNDEFINED"
            )

            view.loadData(
                err, "text/html", "UTF-8"
            )
        }

        super.onReceivedError(view, request, error)
    }

    override fun onReceivedHttpError(
        view: WebView?,
        request: WebResourceRequest?,
        errorResponse: WebResourceResponse?,
    ) {
        super.onReceivedHttpError(view, request, errorResponse)
        mSwipeView.nullply {
            isRefreshing = false
        }
    }

    override fun shouldOverrideUrlLoading(
        view: WebView,
        request: WebResourceRequest,
    ): Boolean {
        val mUri = request.url ?: return false
        val mUrl = mUri.toString()

        val isUnsafe = !mOptions.isDomainSafe(mUrl)

        return if (isUnsafe) {
            mOptions.onUnsafeDomainRequest?.invoke()
                ?: openUri(mUri)
            true
        } else {
            view.loadUrl(mUrl)
            false
        }
    }

    override fun shouldInterceptRequest(
        view: WebView?,
        request: WebResourceRequest,
    ): WebResourceResponse? {
        val urlString = request.url.toString()

        if (urlString.endsWith("/favicon.ico")) {
            Log.d(TAG, "Blocking favicon.ico request for $urlString")
            return WebResourceResponse("image/png", null, null)
        }

        if (mOptions.debug) Log.d(TAG, "shouldInterceptRequest: ${request.url}")
        return mWxAssetsLoader(request.url)
    }

    private companion object {
        const val TAG = "WebUIClient"
    }
}
