package com.dergoogler.mmrl.hybridwebui

import android.R.attr.tag
import android.graphics.Bitmap
import android.os.Message
import android.util.Log
import android.webkit.ClientCertRequest
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.WorkerThread
import com.dergoogler.mmrl.hybridwebui.interfaces.JavaScriptInterface
import com.dergoogler.mmrl.hybridwebui.interfaces.JavaScriptInterfaceImplementation
import java.io.ByteArrayInputStream

open class HybridWebUIClient(
    protected val view: HybridWebUI,
) : WebViewClient() {
    protected val store get() = view.store
    protected val console get() = store.consoleStore
    protected val network get() = store.networkStore
    protected val pathMatchers get() = store.pathMatchers

    private fun loopInterfaces(methodName: String, caller: JavaScriptInterface.() -> Unit) {
        if (!view.isStoreInitialized) {
            Log.w(TAG, "$methodName called before store initialization")
            return
        }

        try {
            store.jsInterfaceStore.loop(caller)
        } catch (e: Exception) {
            Log.e(TAG, "Error calling $methodName", e)
        }
    }

    override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
        loopInterfaces("onPageStarted") {
            onPageStarted(view as HybridWebUI, url, favicon)
        }

        view.evaluateJavascript(CONSOLE_OVERRIDE_JS, null)
        super.onPageStarted(view, url, favicon)
    }

    override fun onPageFinished(view: WebView, url: String) {
        loopInterfaces("onPageFinished") {
            onPageFinished(view as HybridWebUI, url)
        }

        super.onPageFinished(view, url)
    }

    override fun onFormResubmission(view: WebView, dontResend: Message, resend: Message) {
        loopInterfaces("onFormResubmission") {
            onFormResubmission(view as HybridWebUI, dontResend, resend)
        }

        super.onFormResubmission(view, dontResend, resend)
    }

    override fun onReceivedHttpError(
        view: WebView,
        request: WebResourceRequest,
        errorResponse: WebResourceResponse,
    ) {
        loopInterfaces("onReceivedHttpError") {
            onReceivedHttpError(view as HybridWebUI, request, errorResponse)
        }

        super.onReceivedHttpError(view, request, errorResponse)
    }

    override fun onReceivedClientCertRequest(view: WebView, request: ClientCertRequest) {
        loopInterfaces("onReceivedClientCertRequest") {
            onReceivedClientCertRequest(view as HybridWebUI, request)
        }

        super.onReceivedClientCertRequest(view, request)
    }

    override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest,
        error: WebResourceError
    ) {
        loopInterfaces("onReceivedError") {
            onReceivedError(view as HybridWebUI, request, error)
        }

        super.onReceivedError(view, request, error)
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

    private companion object {
        const val TAG = "HybridWebUIClient"
    }
}