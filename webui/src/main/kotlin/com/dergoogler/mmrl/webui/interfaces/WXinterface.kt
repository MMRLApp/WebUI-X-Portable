package com.dergoogler.mmrl.webui.interfaces

import android.app.Activity
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResult
import androidx.annotation.Keep
import androidx.annotation.UiThread
import com.dergoogler.mmrl.ext.findActivity
import dev.mmrlx.hybridwebui.HybridWebUI
import dev.mmrlx.hybridwebui.interfaces.JavaScriptInterface
import dev.mmrlx.hybridwebui.store.error
import dev.mmrlx.hybridwebui.store.trace
import dev.mmrlx.hybridwebui.store.warn
import com.dergoogler.mmrl.platform.model.ModId
import com.dergoogler.mmrl.webui.model.WebUIConfig
import com.dergoogler.mmrl.webui.util.WebUIOptions
import com.dergoogler.mmrl.webui.view.WXView
import com.dergoogler.mmrl.webui.view.WebUIView
import com.sun.jna.Native
import com.sun.jna.NativeLibrary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Runnable


data class WXOptions(
    val activity: ComponentActivity,
    val webView: WebUIView,
    val options: WebUIOptions,
)

/**
 * `WXIInterface` is an abstract base class that provides a set of common utilities for interacting with a WebView within a web-based user interface.
 *
 * It offers functionalities for running JavaScript code, handling asynchronous tasks, logging, error handling, and managing deprecation warnings.
 * This class is designed to be extended by specific WebUI interfaces, providing them with a standardized way to communicate with the web view and the underlying Android environment.
 *
 * @param wxOptions An instance of [WXOptions] containing essential configuration details such as the application context, WebView instance, and module identifier.
 *
 * @property context The Android context associated with the web UI. Typically, this is an instance of [Activity].
 * @property webView The [WXView] instance used for displaying web content.
 * @property modId A unique identifier for the module using this interface.
 * @property name The name of the entity. Must be initialized before access.
 * @property activity The [Activity] instance from context.
 * @property console An object implementing the [WXConsole] interface, offering logging capabilities.
 */
@Keep
open class WXInterface(
    val wxOptions: WXOptions,
) : JavaScriptInterface(wxOptions.activity, wxOptions.webView) {
    val scope = CoroutineScope(Dispatchers.Main)
    val webView: WebUIView = wxOptions.webView
    val options: WebUIOptions = wxOptions.options
    val modId: ModId = options.modId
    val config: WebUIConfig = options.config

    open val assetHandlers: List<Pair<String, HybridWebUI.PathHandler>> = emptyList()

    /**
     * Executes the given JavaScript script within the WebView.
     *
     * This function provides a straightforward way to inject and run JavaScript code in the web content displayed by the WebView.
     * It ensures that the script is executed on the UI thread, which is necessary for interacting with WebView components.
     *
     * @param script A string containing the JavaScript code to be executed.
     *
     * @sample
     * // Example: Changing the background color of the body element in the WebView.
     * runJs("document.body.style.backgroundColor = 'blue';")
     *
     * // Example: Calling a JavaScript function defined in the web page.
     * runJs("myJavaScriptFunction('someArgument');")
     */
    @UiThread
    fun runJs(script: String) = webView.runJs(script)

    /**
     * Executes the given [action] on the WebView's UI thread after the current message queue processing.
     *
     * This method is useful for performing actions that need to interact with the WebView's UI elements
     * or state, ensuring that these operations are executed safely on the main UI thread.
     * The [action] is a lambda function that receives the [WebView] instance as its receiver,
     * allowing direct access to its methods and properties.
     *
     * @param action A lambda function to be executed on the WebView's UI thread.
     *               The lambda has the [WebView] instance as its receiver (`this`).
     *
     * @see WebView.post
     */
    @UiThread
    fun post(action: WebUIView.() -> Unit) = webView.post { action(webView) }

    /**
     * Executes a block of code on the UI thread and catches any exceptions that occur.
     *
     * This extension function is designed to safely execute operations that might throw exceptions,
     * particularly those interacting with JavaScript or the WebView. If an exception occurs,
     * it logs the error to the console and returns `null`. Otherwise, it returns the result
     * of the executed block.
     *
     * This function ensures that the block is executed on the UI thread, which is crucial for
     * operations that interact with UI components like WebViews.
     *
     * @param T The type of the receiver object on which the block is executed.
     * @param R The return type of the block.
     * @param block A lambda function to be executed. It receives the `T` instance as its receiver.
     * @return The result of the `block` if execution is successful, or `null` if an exception occurs.
     *
     * @sample
     * ```kotlin
     * // Example: Safely accessing a property of a JavaScript object
     * val result = webView.runJsCatching {
     *     // This might throw an exception if 'someObject' or 'someProperty' doesn't exist
     *     evaluateJavascript("someObject.someProperty", null)
     * }
     * if (result != null) {
     *     // Process the result
     * } else {
     *     // Handle the error, e.g., show a message to the user
     * }
     * ```
     */
    @UiThread
    inline fun <T, R> T.runJsCatching(block: T.() -> R): R? {
        try {
            return block()
        } catch (e: Exception) {
            console.error(e)
            return null
        }
    }

    fun <R> withActivity(block: Activity.() -> R): R? {
        val act = context.findActivity()
        if (act == null) {
            consoleLogs.trace("withActivity -> activity == null")
            consoleLogs.error("[$tag->withActivity] Activity not found")
            return null
        }

        return block(activity)
    }

    open fun onActivityDestroy() {}
    open fun onActivityResume() {}
    open fun onActivityStop() {}
    open fun onActivityPause() {}

    /**
     * Handles the result from an activity that was started for a result.
     *
     * This function is called when the activity receives a result from another activity.
     * It invokes the previously set callback (if any) with the request code, result code, and intent data.
     *
     * @param requestCode The request code originally supplied to startActivityForResult(),
     *                    allowing you to identify which activity result is being received.
     * @param resultCode The result code returned by the child activity.
     * @param data An Intent, which can return result data to the parent activity.
     */
    @Deprecated("Use onActivityResult(ActivityResult) instead")
    open fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
    ) {
    }

    /**
     * Handles the result from an activity that was started for a result using the modern Activity Result APIs.
     *
     * This function is the recommended way to handle activity results, as it uses the type-safe
     * [ActivityResult] object. It is called when an activity you launched with
     * `registerForActivityResult` finishes.
     *
     * @param result An [ActivityResult] object containing the `resultCode` and `data` (an [Intent])
     *               from the finished activity.
     */
    open fun onActivityResult(
        result: ActivityResult,
    ) {
    }

    fun mainThread(callback: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            callback()
        } else {
            Handler(Looper.getMainLooper()).post { callback() }
        }
    }

    @UiThread
    fun runMainLooperPost(action: Activity.() -> Unit) {
        val act = context.findActivity()
        if (act == null) {
            consoleLogs.error("[$tag->runMainLooperPost] Activity not found")
            return
        }

        Handler(mainLooper).post {
            action(act)
        }
    }

    @UiThread
    fun runMainLooperPost(r: Runnable) {
        val act = context.findActivity()
        if (act == null) {
            consoleLogs.error("[$tag->runMainLooperPost/Runnable] Activity not found")
            return
        }

        Handler(mainLooper).post(r)
    }

    /**
     * Logs a deprecation warning to the console, indicating that a specific method will be removed in future versions.
     *
     * This function provides a standardized way to notify users about deprecated methods, guiding them towards replacements if available.
     *
     * @param method The name of the deprecated method. This will be included in the warning message.
     * @param replaceWith Optional. The name of the method that should be used as a replacement for the deprecated one.
     *                    If provided, the warning message will include instructions on what to use instead.
     *
     * @sample
     * // Deprecating a function named "oldFunction" without a replacement.
     * deprecated("oldFunction")
     *
     * // Deprecating a function named "oldFunction" and suggesting "newFunction" as a replacement.
     * deprecated("oldFunction", "newFunction")
     */
    fun deprecated(method: String, replaceWith: String? = null) {
        consoleLogs.warn(
            "%c[DEPRECATED]%c The `$method` method will be removed in future versions.${if (replaceWith != null) " Use `$replaceWith` instead." else ""}",
            "color: white; background: red; font-weight: bold; padding: 2px 6px; border-radius: 4px;",
            "color: orange; font-weight: bold;"
        );
    }

    val console = webView.console

    fun <R> runTry(
        message: String = "Unknown Error",
        default: R,
        block: () -> R,
    ): R = try {
        block()
    } catch (e: Throwable) {
        consoleLogs.error(e)
        default
    }

    fun <R> runTry(
        message: String = "Unknown Error",
        block: () -> R,
    ): R? = runTry(message, null, block)

    fun <R, T> runTryJsWith(
        with: T,
        message: String = "Unknown Error",
        block: T.() -> R,
    ): R? = runTryJsWith(with, message, null, block)

    fun <R, T> runTryJsWith(
        with: T,
        message: String = "Unknown Error",
        default: R,
        block: T.() -> R,
    ): R {
        return try {
            with(with, block)
        } catch (e: Throwable) {
            consoleLogs.error(e)
            return default
        }
    }

    fun registerLibrary(clazz: Class<*>, name: String) {
        Native.register(clazz, "${name}_$modId")
    }

    fun unregisterLibrary(clazz: Class<*>) {
        Native.unregister(clazz)
    }

    fun isLibraryRegistered(libName: String): Boolean {
        try {
            val lib: NativeLibrary? = NativeLibrary.getInstance("${libName}_$modId")
            return lib != null
        } catch (e: UnsatisfiedLinkError) {
            return false
        }
    }
}