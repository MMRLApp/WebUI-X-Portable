@file:Suppress("unused")

package com.dergoogler.mmrl.hybridwebui.interfaces

import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Message
import android.webkit.ClientCertRequest
import android.webkit.JsPromptResult
import android.webkit.JsResult
import android.webkit.PermissionRequest
import android.webkit.SslErrorHandler
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import androidx.activity.ComponentActivity
import androidx.annotation.CallSuper
import com.dergoogler.mmrl.hybridwebui.HybridWebUI
import com.dergoogler.mmrl.hybridwebui.store.NetworkRequestStore
import com.dergoogler.mmrl.hybridwebui.store.WebConsoleStore

open class JavaScriptInterface(
    protected val activity: ComponentActivity,
    protected val view: HybridWebUI,
) : ContextWrapper(activity) {
    protected val context: Context get() = view.safeApplicationContext

    /**
     * The name of the entity.
     *
     * This property holds the name associated with this object.
     * It is declared as `lateinit` which means it must be initialized before being accessed.
     * Attempting to access it before initialization will result in a [UninitializedPropertyAccessException].
     */
    open lateinit var name: String

    /**
     * A string tag used for logging and identification purposes.
     *
     * This tag helps in categorizing log messages and can be useful for debugging.
     * By default, it is initialized to "WXInterface", but it can be overridden
     * in subclasses to provide more specific identification.
     */
    open var tag: String = "JavaScriptInterface"

    protected val consoleLogs: WebConsoleStore?
        get() {
            if (!view.isStoreInitialized) {
                return null
            }

            return view.store.buildConsoleStore(tag)
        }

    protected val networkRequests: NetworkRequestStore?
        get() {
            if (!view.isStoreInitialized) {
                return null
            }

            return view.store.networkStore
        }

    open fun onPageStarted(view: HybridWebUI, url: String, favicon: Bitmap?) {}
    open fun onPageFinished(view: HybridWebUI, url: String) {}
    open fun onFormResubmission(view: HybridWebUI, dontResend: Message, resend: Message) {}
    open fun onReceivedHttpError(
        view: HybridWebUI,
        request: WebResourceRequest,
        errorResponse: WebResourceResponse,
    ) {
    }

    open fun onReceivedClientCertRequest(view: HybridWebUI, request: ClientCertRequest) {}
    open fun onReceivedError(
        view: HybridWebUI,
        request: WebResourceRequest,
        error: WebResourceError,
    ) {
    }

    open fun onReceivedSslError(view: HybridWebUI, handler: SslErrorHandler, error: SslError) {}


    open fun onJsAlert(
        view: HybridWebUI,
        url: String,
        message: String?,
        result: JsResult,
    ): Boolean = false

    open fun onJsBeforeUnload(
        view: HybridWebUI,
        url: String,
        message: String?,
        result: JsResult,
    ): Boolean = false

    open fun onJsConfirm(
        view: HybridWebUI,
        url: String,
        message: String?,
        result: JsResult,
    ): Boolean = false

    open fun onJsPrompt(
        view: HybridWebUI,
        url: String,
        message: String?,
        defaultValue: String?,
        result: JsPromptResult,
    ): Boolean = false

    open fun onPermissionRequest(request: PermissionRequest) {}

    open fun onPermissionRequestCanceled(request: PermissionRequest) {}

    open fun onProgressChanged(view: HybridWebUI, newProgress: Int) {}

    open fun onReceivedIcon(view: HybridWebUI, icon: Bitmap?) {}

    open fun onReceivedTitle(view: HybridWebUI, title: String?) {}

    open fun onReceivedTouchIconUrl(view: HybridWebUI, url: String, precomposed: Boolean) {}

    /**
     * Called when the [HybridWebUI] is being destroyed.
     *
     * Use this method to perform any necessary cleanup, such as unregistering
     * listeners or releasing resources associated with the JavaScript interface.
     *
     * **Note:** Implementation must call `super.onDestroy()` to ensure
     * proper lifecycle management.
     */
    @CallSuper
    open fun onDestroy() {
        consoleLogs?.clear()
    }

    open fun onShowFileChooser(
        webView: HybridWebUI,
        filePathCallback: ValueCallback<Array<Uri>>,
        fileChooserParams: WebChromeClient.FileChooserParams,
    ): Boolean = false

    companion object {
        operator fun <T : JavaScriptInterface> invoke(
            clazz: Class<T>,
            initArgs: Array<Any>? = null,
            parameterTypes: Array<Class<*>>? = null,
        ): JavaScriptInterfaceImplementation<T> {
            return JavaScriptInterfaceImplementation(clazz, initArgs, parameterTypes)
        }
    }
}