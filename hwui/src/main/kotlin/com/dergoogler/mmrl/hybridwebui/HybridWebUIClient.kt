package com.dergoogler.mmrl.hybridwebui

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.WorkerThread
import java.io.ByteArrayInputStream

open class HybridWebUIClient(
    protected val pathMatchers: MutableList<HybridWebUI.PathMatcher>,
) : WebViewClient() {
    @WorkerThread
    override fun shouldInterceptRequest(
        view: WebView,
        request: WebResourceRequest,
    ): WebResourceResponse? {
        val url = request.url

        for (matcher in pathMatchers) {
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
                handler.handle(newRequest)
            } catch (t: Throwable) {
                shouldInterceptRequestError(t, newRequest)
            } ?: continue

            return response
        }

        return null
    }

    open fun shouldInterceptRequestError(throwable: Throwable, request: HybridWebUIResourceRequest): WebResourceResponse? {
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