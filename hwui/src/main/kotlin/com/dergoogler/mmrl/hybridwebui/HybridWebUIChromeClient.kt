package com.dergoogler.mmrl.hybridwebui

import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.JsPromptResult
import android.webkit.JsResult
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import com.dergoogler.mmrl.hybridwebui.ConsoleEntry.Companion.toConsoleEntry
import com.dergoogler.mmrl.hybridwebui.interfaces.JavaScriptInterface

open class HybridWebUIChromeClient(
    protected val view: HybridWebUI,
) : WebChromeClient() {
    protected val store get() = view.store
    protected val console get() = store.consoleStore
    protected val network get() = store.networkStore

    private fun <T> queryInterfaces(methodName: String, caller: JavaScriptInterface.() -> T): T? {
        if (!view.isStoreInitialized) return null
        return runCatching {
            store.jsInterfaceStore.findFirst(caller)
        }.onFailure { Log.e(TAG, "Error in $methodName", it) }.getOrNull()
    }

    private fun queryBoolean(
        methodName: String,
        caller: JavaScriptInterface.() -> Boolean
    ): Boolean {
        if (!view.isStoreInitialized) return false
        return runCatching {
            store.jsInterfaceStore.dispatchBoolean(caller)
        }.onFailure { Log.e(TAG, "Error in $methodName", it) }.getOrElse { false }
    }

    private fun notifyInterfaces(methodName: String, action: JavaScriptInterface.() -> Unit) {
        if (!view.isStoreInitialized) return
        runCatching {
            store.jsInterfaceStore.forEach(action)
        }.onFailure { Log.e(TAG, "Error in $methodName", it) }
    }

    override fun onShowFileChooser(
        view: WebView,
        filePathCallback: ValueCallback<Array<Uri>>,
        fileChooserParams: FileChooserParams,
    ): Boolean = queryBoolean("onShowFileChooser") {
        onShowFileChooser(view as HybridWebUI, filePathCallback, fileChooserParams)
    }

    override fun onJsAlert(
        view: WebView,
        url: String,
        message: String?,
        result: JsResult
    ): Boolean = queryBoolean("onJsAlert") {
        onJsAlert(view as HybridWebUI, url, message, result)
    }

    override fun onJsBeforeUnload(
        view: WebView,
        url: String,
        message: String?,
        result: JsResult
    ): Boolean = queryBoolean("onJsBeforeUnload") {
        onJsBeforeUnload(view as HybridWebUI, url, message, result)
    }

    override fun onJsConfirm(
        view: WebView,
        url: String,
        message: String?,
        result: JsResult
    ): Boolean = queryBoolean("onJsConfirm") {
        onJsConfirm(view as HybridWebUI, url, message, result)
    }

    override fun onJsPrompt(
        view: WebView,
        url: String,
        message: String?,
        defaultValue: String?,
        result: JsPromptResult
    ): Boolean = queryBoolean("onJsPrompt") {
        onJsPrompt(view as HybridWebUI, url, message, defaultValue, result)
    }

    override fun onPermissionRequest(request: PermissionRequest) {
        notifyInterfaces("onPermissionRequest") {
            onPermissionRequest(request)
        }

        super.onPermissionRequest(request)
    }

    override fun onPermissionRequestCanceled(request: PermissionRequest) {
        notifyInterfaces("onPermissionRequestCanceled") {
            onPermissionRequestCanceled(request)
        }

        super.onPermissionRequestCanceled(request)
    }

    override fun onProgressChanged(view: WebView, newProgress: Int) {
        notifyInterfaces("onProgressChanged") {
            onProgressChanged(view as HybridWebUI, newProgress)
        }

        super.onProgressChanged(view, newProgress)
    }

    override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
        console.add(consoleMessage.toConsoleEntry())
        return super.onConsoleMessage(consoleMessage)
    }

    override fun onReceivedIcon(view: WebView, icon: Bitmap?) {
        notifyInterfaces("onReceivedIcon") {
            onReceivedIcon(view as HybridWebUI, icon)
        }

        super.onReceivedIcon(view, icon)
    }

    override fun onReceivedTitle(view: WebView, title: String?) {
        notifyInterfaces("onReceivedTitle") {
            onReceivedTitle(view as HybridWebUI, title)
        }

        super.onReceivedTitle(view, title)
    }

    override fun onReceivedTouchIconUrl(view: WebView, url: String, precomposed: Boolean) {
        notifyInterfaces("onReceivedTouchIconUrl") {
            onReceivedTouchIconUrl(view as HybridWebUI, url, precomposed)
        }
        super.onReceivedTouchIconUrl(view, url, precomposed)
    }

    private companion object {
        const val TAG = "HybridWebUIChromeClient"
    }
}