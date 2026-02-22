package com.dergoogler.mmrl.webui.view

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.view.ViewGroup
import android.view.WindowInsetsController
import androidx.activity.ComponentActivity
import androidx.core.view.WindowCompat
import com.dergoogler.mmrl.compat.BuildCompat
import com.dergoogler.mmrl.ext.findActivity
import com.dergoogler.mmrl.platform.file.SuFile.Companion.toSuFile
import com.dergoogler.mmrl.webui.client.WXChromeClient
import com.dergoogler.mmrl.webui.client.WXClient
import com.dergoogler.mmrl.webui.client.WXRenderProcessClient
import com.dergoogler.mmrl.webui.interfaces.ApplicationInterface
import com.dergoogler.mmrl.webui.interfaces.FileInputInterface
import com.dergoogler.mmrl.webui.interfaces.FileInterface
import com.dergoogler.mmrl.webui.interfaces.FileOutputInterface
import com.dergoogler.mmrl.webui.interfaces.IntentInterface
import com.dergoogler.mmrl.webui.interfaces.ModuleInterface
import com.dergoogler.mmrl.webui.interfaces.PackageManagerInterface
import com.dergoogler.mmrl.webui.interfaces.UserManagerInterface
import com.dergoogler.mmrl.webui.model.WXEvent
import com.dergoogler.mmrl.webui.model.WXInsetsEventData.Companion.toEventData
import com.dergoogler.mmrl.webui.pathHandler.InternalPathHandler
import com.dergoogler.mmrl.webui.pathHandler.SuPathHandler
import com.dergoogler.mmrl.webui.pathHandler.WebrootPathHandler
import com.dergoogler.mmrl.webui.util.WebUIOptions
import com.dergoogler.mmrl.webui.util.WebUIOptions.Companion.defaultWebUiOptions
import com.dergoogler.mmrl.webui.util.errorPages.requireNewVersionErrorPage
import com.dergoogler.mmrl.webui.util.lua.LuaEngine

@SuppressLint("SetJavaScriptEnabled")
open class WXView : WebUIView {
    private lateinit var luaEngine: LuaEngine

    constructor(options: WebUIOptions) : super(options) {
        this.options = options
    }

    constructor(context: Context) : this(context.defaultWebUiOptions) {
        this.options = context.defaultWebUiOptions
        throw UnsupportedOperationException("Default constructor not supported. Use constructor with options.")
    }

    constructor(context: Context, attrs: AttributeSet) : this(context.defaultWebUiOptions) {
        this.options = context.defaultWebUiOptions
        throw UnsupportedOperationException("Default constructor not supported. Use constructor with options.")
    }

    constructor(
        context: Context,
        attrs: AttributeSet,
        defStyle: Int,
    ) : this(context.defaultWebUiOptions) {
        this.options = context.defaultWebUiOptions
        throw UnsupportedOperationException("Default constructor not supported. Use constructor with options.")
    }

    override suspend fun onInit() {
        super.onInit()

        val activity = context.findActivity()

        if (activity != null) {
            activity.apply {
                WindowCompat.setDecorFitsSystemWindows(window, false)
                if (BuildCompat.atLeastT) {
                    windowInsetsController?.systemBarsBehavior =
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
                webChromeClient = WXChromeClient(this@apply as ComponentActivity, options)
            }
        } else {
            Log.e("WXView", "WXActivity not found")
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            webViewRenderProcessClient = WXRenderProcessClient(options)
        }

        settings.apply {
            options {
                if (debug && remoteDebug) {
                    mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                }
                this@apply.userAgentString = this@options.userAgentString
            }
        }

        this.luaEngine = LuaEngine(options, this)
        luaEngine.run()

        addPathHandler(
            "/.${options.modId}/",
            SuPathHandler("/data/adb/modules/${options.modId}".toSuFile())
        )
        addPathHandler("/.adb/", SuPathHandler("/data/adb".toSuFile()))
        addPathHandler("/.config/", SuPathHandler("/data/adb/.config".toSuFile()))
        addPathHandler("/.local/", SuPathHandler("/data/adb/.local".toSuFile()))

        if (options.config.hasRootPathPermission) {
            addPathHandler("/__root__/", SuPathHandler("/".toSuFile()))
        }

        val client = WXClient(options, pathMatchers)
        client.mSwipeView = mSwipeView
        super.webViewClient = client

        addJavascriptInterfaces()

        onInsets { view, insets ->
            view.addPathHandler("/mmrl/", InternalPathHandler(options, insets))
            view.addPathHandler("/internal/", InternalPathHandler(options, insets))
            view.addPathHandler("/", WebrootPathHandler(options, insets))

            postWXEvent(
                type = WXEvent.WX_ON_INSETS,
                data = insets.toEventData()
            )
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

        if (options.pluginsEnabled && options.config.dexFiles.isNotEmpty()) {
            for (dexFile in options.config.dexFiles) {
                val interfaceObj = dexFile.getInterface(context, options.modId)
                if (interfaceObj != null) {
                    addJavascriptInterface(interfaceObj)
                }
            }
        }
    }

    override fun destroy() {
        super.destroy()

        try {
            for (inst in interfaces) {
                inst.unregister()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering interfaces", e)
        }

        (parent as? ViewGroup)?.removeView(this)
        removeAllViews()

        Log.d(TAG, "WebUI X cleaned up")
    }

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