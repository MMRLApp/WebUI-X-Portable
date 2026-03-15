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

    private fun <T> loopInterfaces(methodName: String, caller: JavaScriptInterface.() -> T): T? {
        if (!view.isStoreInitialized) {
            Log.w(TAG, "$methodName called before store initialization")
            return null
        }

        try {
            return store.jsInterfaceStore.loop(caller)
        } catch (e: Exception) {
            Log.e(TAG, "Error calling $methodName", e)
            return null
        }
    }

    override fun onShowFileChooser(
        view: WebView,
        filePathCallback: ValueCallback<Array<Uri>>,
        fileChooserParams: FileChooserParams,
    ): Boolean {
        return loopInterfaces("onShowFileChooser") {
            onShowFileChooser(view as HybridWebUI, filePathCallback, fileChooserParams)
        } == true

        /*if (view !is HybridWebUI) {
            return false
        }

        store.filePathCallback?.onReceiveValue(null)
        store.filePathCallback = filePathCallback
        val intent = fileChooserParams?.createIntent() ?: Intent(Intent.ACTION_GET_CONTENT).apply {
            type = ""
        }
        if (fileChooserParams?.mode == FileChooserParams.MODE_OPEN_MULTIPLE) {
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        try {
            store.fileChooserLauncher?.launch(intent)
        } catch (_: ActivityNotFoundException) {
            store.filePathCallback?.onReceiveValue(null)
            store.filePathCallback = null
            return false
        }

        return true*/
    }

    override fun onJsAlert(
        view: WebView,
        url: String,
        message: String?,
        result: JsResult
    ): Boolean = loopInterfaces("onJsAlert") {
        onJsAlert(view as HybridWebUI, url, message, result)
    } == true

    override fun onJsBeforeUnload(
        view: WebView,
        url: String,
        message: String?,
        result: JsResult
    ): Boolean = loopInterfaces("onJsBeforeUnload") {
        onJsBeforeUnload(view as HybridWebUI, url, message, result)
    } == true

    override fun onJsConfirm(
        view: WebView,
        url: String,
        message: String?,
        result: JsResult
    ): Boolean = loopInterfaces("onJsConfirm") {
        onJsConfirm(view as HybridWebUI, url, message, result)
    } == true

    override fun onJsPrompt(
        view: WebView,
        url: String,
        message: String?,
        defaultValue: String?,
        result: JsPromptResult
    ): Boolean = loopInterfaces("onJsPrompt") {
        onJsPrompt(view as HybridWebUI, url, message, defaultValue, result)
    } == true

    override fun onPermissionRequest(request: PermissionRequest) {
        loopInterfaces("onPermissionRequest") {
            onPermissionRequest(request)
        }

        super.onPermissionRequest(request)
    }

    override fun onPermissionRequestCanceled(request: PermissionRequest) {
        loopInterfaces("onPermissionRequestCanceled") {
            onPermissionRequestCanceled(request)
        }

        super.onPermissionRequestCanceled(request)
    }

    override fun onProgressChanged(view: WebView, newProgress: Int) {
        loopInterfaces("onProgressChanged") {
            onProgressChanged(view as HybridWebUI, newProgress)
        }

        super.onProgressChanged(view, newProgress)
    }

    override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
        console.add(consoleMessage.toConsoleEntry())
        return super.onConsoleMessage(consoleMessage)
    }

    override fun onReceivedIcon(view: WebView, icon: Bitmap?) {
        loopInterfaces("onReceivedIcon") {
            onReceivedIcon(view as HybridWebUI, icon)
        }

        super.onReceivedIcon(view, icon)
    }

    override fun onReceivedTitle(view: WebView, title: String?) {
        loopInterfaces("onReceivedTitle") {
            onReceivedTitle(view as HybridWebUI, title)
        }

        super.onReceivedTitle(view, title)
    }

    override fun onReceivedTouchIconUrl(view: WebView, url: String, precomposed: Boolean) {
        loopInterfaces("onReceivedTouchIconUrl") {
            onReceivedTouchIconUrl(view as HybridWebUI, url, precomposed)
        }
        super.onReceivedTouchIconUrl(view, url, precomposed)
    }

    private companion object {
        const val TAG = "HybridWebUIChromeClient"
    }
}