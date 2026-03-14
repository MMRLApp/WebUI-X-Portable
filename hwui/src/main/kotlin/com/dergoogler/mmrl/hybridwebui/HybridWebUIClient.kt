package com.dergoogler.mmrl.hybridwebui

import android.graphics.Bitmap
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.WorkerThread
import java.io.ByteArrayInputStream

open class HybridWebUIClient(
    protected val view: HybridWebUI,
) : WebViewClient() {
    protected val store get() = view.store
    protected val console get() = store.consoleStore
    protected val network get() = store.networkStore
    protected val pathMatchers get() = store.pathMatchers

    override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
        view.evaluateJavascript(CONSOLE_OVERRIDE_JS, null)
        super.onPageStarted(view, url, favicon)
    }

    @WorkerThread
    override fun shouldInterceptRequest(
        view: WebView,
        request: WebResourceRequest,
    ): WebResourceResponse? {
        network.add(request)

        val url = request.url

        for (matcher in pathMatchers.all) {
            val handler = matcher.match(url) ?: continue
            val suffixPath = matcher.getSuffixPath(url.path!!)

            val newRequest = HybridWebUIResourceRequest(
                method = request.method,
                requestHeaders = request.requestHeaders,
                url = request.url,
                path = suffixPath,
                hasGesture = request.hasGesture(),
                isForMainFrame = request.isForMainFrame,
                isRedirect = request.isRedirect
            )

            val response = try {
                handler.handle(view as HybridWebUI, newRequest)
            } catch (t: Throwable) {
                shouldInterceptRequestError(t, newRequest)
            } ?: continue

            return response
        }

        return null
    }

    open fun shouldInterceptRequestError(
        throwable: Throwable,
        request: HybridWebUIResourceRequest,
    ): WebResourceResponse? {
        return WebResourceResponse(
            "plain/text",
            "UTF-8",
            500,
            "Internal Server Error",
            null,
            ByteArrayInputStream("Message: ${throwable.message}\n\nStacktrace: ${throwable.stackTraceToString()}".toByteArray())
        )
    }
}