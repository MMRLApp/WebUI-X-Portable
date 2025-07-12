package com.dergoogler.mmrl.webui.view

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.WindowInsetsController
import android.webkit.WebView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.dergoogler.mmrl.compat.BuildCompat
import com.dergoogler.mmrl.ext.findActivity
import com.dergoogler.mmrl.ext.nullply
import com.dergoogler.mmrl.webui.client.WXChromeClient
import com.dergoogler.mmrl.webui.client.WXClient
import com.dergoogler.mmrl.webui.interfaces.ApplicationInterface
import com.dergoogler.mmrl.webui.interfaces.FileInputInterface
import com.dergoogler.mmrl.webui.interfaces.FileInterface
import com.dergoogler.mmrl.webui.interfaces.FileOutputInterface
import com.dergoogler.mmrl.webui.interfaces.IntentInterface
import com.dergoogler.mmrl.webui.interfaces.ModuleInterface
import com.dergoogler.mmrl.webui.interfaces.PackageManagerInterface
import com.dergoogler.mmrl.webui.interfaces.UserManagerInterface
import com.dergoogler.mmrl.webui.model.Insets
import com.dergoogler.mmrl.webui.model.WXEvent
import com.dergoogler.mmrl.webui.model.WXInsetsEventData.Companion.toEventData
import com.dergoogler.mmrl.webui.util.WebUIOptions
import com.dergoogler.mmrl.webui.util.errorPages.requireNewVersionErrorPage
import com.dergoogler.mmrl.webui.util.getRequireNewVersion

/**
 * WXView is a custom [WebView] component designed for the **WebUI X Engine**.
 * It provides enhanced functionality for web-based user interfaces within Android applications.
 *
 * This class handles the initialization of the WebView, including setting up JavaScript interfaces,
 * managing window insets, and configuring various WebView settings. It also provides helper
 * methods for interacting with the WebView from native code, such as posting messages,
 * handling events, and executing JavaScript.
 *
 * **Key Features:**
 * - **Simplified Initialization:**  Handles common WebView setup tasks automatically.
 * - **JavaScript Interface Management:**  Provides a structured way to add and manage JavaScript interfaces.
 * - **Window Inset Handling:**  Adjusts WebView content based on system window insets.
 * - **Event Handling:**  Facilitates communication between the WebView and native code through events.
 * - **Helper Methods:**  Offers convenient methods for common WebView operations.
 *
 * **Constructors:**
 * - `WXView(options: WebUIOptions)`: Initializes the WXView with the specified [WebUIOptions].
 *   This is the recommended constructor for creating a WXView instance.
 * - `WXView(context: Context)`: Constructor for creating WXView programmatically.
 *   **Note:** This constructor will throw an [UnsupportedOperationException] as default options are not supported.
 *   You must use the constructor with [WebUIOptions].
 * - `WXView(context: Context, attrs: AttributeSet)`:  Used for inflating WXView from XML layouts.
 *   **Note:** This constructor will throw an [UnsupportedOperationException] as default options are not supported.
 *   You must use the constructor with [WebUIOptions].
 * - `WXView(context: Context, attrs: AttributeSet, defStyle: Int)`: Used for inflating WXView from XML layouts
 *   with a default style attribute.
 *   **Note:** This constructor will throw an [UnsupportedOperationException] as default options are not supported.
 *   You must use the constructor with [WebUIOptions].
 *
 * @param options The [WebUIOptions] used to configure this WXView.
 */
@SuppressLint("SetJavaScriptEnabled")
open class WXView(
    options: WebUIOptions,
) : WebUIView(options) {
    constructor(context: Context) : this(WebUIOptions(context = context)) {
        throw UnsupportedOperationException("Default constructor not supported. Use constructor with options.")
    }

    constructor(context: Context, attrs: AttributeSet) : this(WebUIOptions(context = context)) {
        throw UnsupportedOperationException("Default constructor not supported. Use constructor with options.")
    }

    constructor(
        context: Context,
        attrs: AttributeSet,
        defStyle: Int,
    ) : this(WebUIOptions(context = context)) {
        throw UnsupportedOperationException("Default constructor not supported. Use constructor with options.")
    }

    override fun onInit(isInitialized: Boolean) {
        super.onInit(isInitialized)

        if (isInitialized) return

        val activity = context.findActivity()

        // Window configuration
        activity.nullply {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            if (BuildCompat.atLeastT) {
                windowInsetsController?.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }

        // WebView clients and settings
        webChromeClient = WXChromeClient(options)

        settings.apply {
            options {
                if (debug && remoteDebug) {
                    mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                }
                this@apply.userAgentString = this@options.userAgentString
            }
        }


        // Window insets handling
        ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
            val top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            val bottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            val left = insets.getInsets(WindowInsetsCompat.Type.systemBars()).left
            val right = insets.getInsets(WindowInsetsCompat.Type.systemBars()).right

            val newInsets = Insets(
                top = top.asPx,
                bottom = bottom.asPx,
                left = left.asPx,
                right = right.asPx
            )

            postWXEvent(
                type = WXEvent.WX_ON_INSETS,
                data = newInsets.toEventData()
            )

            if (options.debug) Log.d(TAG, "Insets: $newInsets")

            val client = if (options.client != null) {
                options.client(options, newInsets)
            } else {
                WXClient(options, newInsets)
            }

            client.mSwipeView = mSwipeView

            super.webViewClient = client

            insets
        }

        // JavaScript interfaces (delayed until WebView is fully ready)
        post {
            addJavascriptInterfaces()
            Log.d(TAG, "WebUI X fully initialized")
        }
    }

    private fun addJavascriptInterfaces() {
        addJavascriptInterface<FileInputInterface>()
        addJavascriptInterface<FileOutputInterface>()
        addJavascriptInterface<ApplicationInterface>()
        addJavascriptInterface<FileInterface>()
        addJavascriptInterface<ModuleInterface>()
        addJavascriptInterface<UserManagerInterface>()
        addJavascriptInterface<PackageManagerInterface>()
        addJavascriptInterface<IntentInterface>()

        if (options.config.dexFiles.isNotEmpty()) {
            for (dexFile in options.config.dexFiles) {
                val interfaceObj = dexFile.getInterface(context, options.modId)
                if (interfaceObj != null) {
                    addJavascriptInterface(interfaceObj)
                }
            }
        }
    }

    override fun cleanup() {

        stopLoading()
        webChromeClient = null
        removeView(this)
        webChromeClient = null

        super.cleanup()

        Log.d(TAG, "WebUI X cleaned up")
    }

    private val Int.asPx: Int
        get() = (this / context.resources.displayMetrics.density).toInt()


    /**
     * Loads the domain URL specified in the [WebUIOptions].
     *
     * This function checks if a new app version is required. If it is,
     * it loads a "require new version" page. Otherwise, it loads the
     * `domainUrl` from the options.
     */
    override fun loadDomain() {
        options {
            if (requireNewAppVersion?.required == true) {
                loadData(
                    requireNewVersionErrorPage(), "text/html", "UTF-8"
                )

                return@options
            }

            loadUrl(domainUrl)
        }
    }

    companion object {
        private const val TAG = "WXView"
    }
}
