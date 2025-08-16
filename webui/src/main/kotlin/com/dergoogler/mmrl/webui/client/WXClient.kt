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
import android.webkit.WebViewClient
import androidx.compose.material3.ExperimentalMaterial3Api
import com.dergoogler.mmrl.ext.nullply
import com.dergoogler.mmrl.platform.file.SuFile.Companion.toSuFile
import com.dergoogler.mmrl.webui.PathHandler
import com.dergoogler.mmrl.webui.R
import com.dergoogler.mmrl.webui.WXAssetLoader
import com.dergoogler.mmrl.webui.component.ErrorScreen
import com.dergoogler.mmrl.webui.handler.internalPathHandler
import com.dergoogler.mmrl.webui.handler.suPathHandler
import com.dergoogler.mmrl.webui.handler.webrootPathHandler
import com.dergoogler.mmrl.webui.model.Insets
import com.dergoogler.mmrl.webui.model.SslErrors
import com.dergoogler.mmrl.webui.model.WebResourceErrors
import com.dergoogler.mmrl.webui.util.WebUIOptions
import com.dergoogler.mmrl.webui.util.drawCompose
import com.dergoogler.mmrl.webui.view.WXSwipeRefresh
import com.dergoogler.mmrl.webui.view.WebUIView
import com.dergoogler.mmrl.webui.wxAssetLoader

open class WXClient : WebViewClient {
    private val mOptions: WebUIOptions
    private val mWxAssetsLoader: WXAssetLoader
    internal var mSwipeView: WXSwipeRefresh? = null

    constructor(
        options: WebUIOptions,
        insets: Insets,
        assetHandlers: List<Pair<String, PathHandler>> = emptyList(),
    ) {
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

                addAll(assetHandlers)

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
        // Cancel the SSL handshake
        handler.cancel()

        val errCode = SslErrors.from(error.primaryError)

        mOptions.drawCompose {
            ErrorScreen(
                icon = R.drawable.certificate_off,
                title = getString(R.string.failed_to_open_ssl),
                description = getString(
                    R.string.failed_to_open_ssl_desc,
                    view.url,
                ),
                errorCode = errCode?.name ?: "UNDEFINED",
                onRefresh = {
                    if (view is WebUIView) {
                        view.loadDomain()
                        return@ErrorScreen
                    }

                    view.reload()
                },
                moreInfoText = R.string.proceed,
                onMoreInfo = {
                    handler.proceed()
                }
            )
        }
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
        const val TAG = "WXClient"
    }
}