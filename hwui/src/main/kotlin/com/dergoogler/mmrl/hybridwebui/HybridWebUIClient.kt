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
        val encodedPath = url.encodedPath ?: ""

        for (matcher in pathMatchers) {
            val handler: HybridWebUI.PathHandler? = matcher.match(url)
            if (handler != null) {
                val suffixPath: String = matcher.getSuffixPath(encodedPath)

                val newRequest = HybridWebUIResourceRequest(
                    method = request.method,
                    requestHeaders = request.requestHeaders,
                    url = request.url,
                    path = suffixPath,
                    hasGesture = request.hasGesture(),
                    isForMainFrame = request.isForMainFrame,
                    isRedirect = request.isRedirect
                )

                try {
                    val response = handler.handle(newRequest)
                    if (response != null) {
                        return response
                    }
                } catch (t: Exception) {
                    return WebResourceResponse(
                        "application/json",
                        "UTF-8",
                        500,
                        "Internal Server Error",
                        null,
                        ByteArrayInputStream("Message: ${t.message}\n\nStacktrace: ${t.stackTraceToString()}".toByteArray())
                    )
                }
            }
        }

        return null
    }
}