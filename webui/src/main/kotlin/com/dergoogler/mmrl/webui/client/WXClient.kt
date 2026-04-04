package com.dergoogler.mmrl.webui.client

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.util.Log
import android.view.ViewGroup
import android.webkit.RenderProcessGoneDetail
import android.webkit.SslErrorHandler
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import com.dergoogler.mmrl.ext.nullply
import dev.mmrlx.hybridwebui.HybridWebUI
import dev.mmrlx.hybridwebui.HybridWebUIClient
import com.dergoogler.mmrl.webui.model.invoke
import com.dergoogler.mmrl.webui.util.WebUIOptions
import com.dergoogler.mmrl.webui.view.WXSwipeRefresh

open class WXClient : HybridWebUIClient {
    private val mOptions: WebUIOptions
    internal var mSwipeView: WXSwipeRefresh? = null

    constructor(
        view: HybridWebUI,
        options: WebUIOptions,
    ) : super(view) {
        mOptions = options
    }

    private fun openUri(uri: Uri) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, uri)
            mOptions.context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening URI: $uri", e)
        }
    }

    override fun onPageFinished(view: WebView, url: String) {
        super.onPageFinished(view, url)
        mSwipeView.nullply {
            isRefreshing = false
        }
    }

    override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
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
            return
        }

        handler.cancel()
    }

//
//    @OptIn(ExperimentalMaterial3Api::class)
//    override fun onReceivedError(
//        view: WebView,
//        request: WebResourceRequest,
//        error: WebResourceError,
//    ) {
//        mSwipeView.nullply {
//            isRefreshing = false
//        }
//
//        if (request.isForMainFrame) {
//            mOptions.drawCompose {
//                ErrorScreen(
//                    icon = R.drawable.exclamation_circle,
//                    title = getString(R.string.failed_to_load),
//                    description = getString(
//                        R.string.failed_to_load_desc,
//                        request.url,
//                        error.description
//                    ),
//                    suggestions = listOf(
//                        getString(R.string.check_your_internet_connection),
//                        getString(R.string.refreshing_the_page),
//                        getString(R.string.restarting_the_webui_x)
//                    ),
//                    errorCode = WebResourceErrors.from(error.errorCode)?.name ?: "UNDEFINED",
//                    onRefresh = {
//                        if (view is WebUIView) {
//                            view.loadDomain()
//                            return@ErrorScreen
//                        }
//
//                        view.reload()
//                    },
//                )
//            }
//        }
//    }

    // Inside your WebViewClient class
    override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
        Log.e(TAG, "Renderer crashed! Did it exit cleanly? ${detail?.didCrash()}")

        view?.let {
            val parent = it.parent as? ViewGroup
            parent?.removeView(it)
            it.destroy()
        }

        return true
    }

    override fun onReceivedHttpError(
        view: WebView,
        request: WebResourceRequest,
        errorResponse: WebResourceResponse,
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

        val isLoadedData = mUrl.startsWith("data:")
        val isUnsafe = !mOptions.isDomainSafe(mUrl)

        if (isLoadedData) {
            return false
        }

        if (isUnsafe) {
            if (mOptions.onUnsafeDomainRequest != null) {
                mOptions.onUnsafeDomainRequest.invoke(mUri)
                return true
            }

            openUri(mUri)
            return true
        }

        view.loadUrl(mUrl)
        return false
    }

    @Deprecated("")
    private fun isAllowedUrl(requestHost: String?): Boolean = mOptions.config {
        if (requestHost == null) return@config false
        if (allowUrls.isEmpty()) return@config true
        return@config allowUrlsPatterns.any { pattern ->
            pattern.matcher(requestHost).matches()
        }
    }

    companion object {
        private const val TAG = "WXClient"
    }
}