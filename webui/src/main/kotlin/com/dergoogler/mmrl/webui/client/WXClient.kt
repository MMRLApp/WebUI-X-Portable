package com.dergoogler.mmrl.webui.client

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.util.Log
import android.webkit.RenderProcessGoneDetail
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.compose.material3.ExperimentalMaterial3Api
import com.dergoogler.mmrl.ext.nullply
import com.dergoogler.mmrl.hybridwebui.HybridWebUI
import com.dergoogler.mmrl.hybridwebui.HybridWebUIClient
import com.dergoogler.mmrl.webui.R
import com.dergoogler.mmrl.webui.component.ErrorScreen
import com.dergoogler.mmrl.webui.model.WebResourceErrors
import com.dergoogler.mmrl.webui.model.invoke
import com.dergoogler.mmrl.webui.util.WebUIOptions
import com.dergoogler.mmrl.webui.util.drawCompose
import com.dergoogler.mmrl.webui.view.WXSwipeRefresh
import com.dergoogler.mmrl.webui.view.WebUIView

open class WXClient : HybridWebUIClient {
    private val mOptions: WebUIOptions
    internal var mSwipeView: WXSwipeRefresh? = null

    constructor(
        options: WebUIOptions,
        matchers: MutableList<HybridWebUI.PathMatcher>,
    ) : super(matchers) {
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
            return
        }

        handler.cancel()
    }

    override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail): Boolean {
        mSwipeView.nullply {
            isRefreshing = false
        }
        Log.e(TAG, "Renderer crashed. Did it crash? ${detail.didCrash()}")
        return true // or false to kill app
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onReceivedError(
        view: WebView,
        request: WebResourceRequest,
        error: WebResourceError,
    ) {
        mSwipeView.nullply {
            isRefreshing = false
        }

        if (request.isForMainFrame) {
            mOptions.drawCompose {
                ErrorScreen(
                    icon = R.drawable.exclamation_circle,
                    title = getString(R.string.failed_to_load),
                    description = getString(
                        R.string.failed_to_load_desc,
                        request.url,
                        error.description
                    ),
                    suggestions = listOf(
                        getString(R.string.check_your_internet_connection),
                        getString(R.string.refreshing_the_page),
                        getString(R.string.restarting_the_webui_x)
                    ),
                    errorCode = WebResourceErrors.from(error.errorCode)?.name ?: "UNDEFINED",
                    onRefresh = {
                        if (view is WebUIView) {
                            view.loadDomain()
                            return@ErrorScreen
                        }

                        view.reload()
                    },
                )
            }
        }
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

    private companion object {
        const val TAG = "WXClient"
    }
}