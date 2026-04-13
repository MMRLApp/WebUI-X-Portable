package com.dergoogler.mmrl.wx.ui.webui

import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.net.toUri
import com.dergoogler.mmrl.ext.exception.BrickException
import com.dergoogler.mmrl.ext.managerVersion
import com.dergoogler.mmrl.platform.PlatformManager
import com.dergoogler.mmrl.platform.model.ModId.Companion.getModId
import com.dergoogler.mmrl.wx.datastore.model.WorkingMode.Companion.isRoot
import com.dergoogler.mmrl.wx.datastore.providable.LocalUserPreferences
import com.dergoogler.mmrl.wx.ui.component.DraggableFab
import com.dergoogler.mmrl.wx.ui.webui.devtools.DevTools
import com.dergoogler.mmrl.wx.ui.webui.devtools.LocalWebUI
import com.dergoogler.mmrl.wx.ui.webui.interfaces.ApplicationInterface
import com.dergoogler.mmrl.wx.ui.webui.interfaces.FileSystemInterface
import com.dergoogler.mmrl.wx.ui.webui.interfaces.KernelSUInterface
import com.dergoogler.mmrl.wx.ui.webui.pathHandlers.InternalPathHandler
import com.dergoogler.mmrl.wx.ui.webui.pathHandlers.SuPathHandler
import com.dergoogler.mmrl.wx.ui.webui.pathHandlers.WebrootPathHandler
import com.dergoogler.mmrl.wx.ui.webui.util.isPlatformAlive
import com.dergoogler.mmrl.wx.util.BaseActivity
import com.dergoogler.mmrl.wx.util.setBaseContent
import com.dergoogler.mmrl.wx.util.setMyCrashHandler
import dagger.hilt.android.AndroidEntryPoint
import dev.mmrlx.compose.webui.WebUIView
import dev.mmrlx.compose.webui.rememberWebUIState
import dev.mmrlx.utilities.json.jsonObject
import dev.mmrlx.webui.WebUI

@AndroidEntryPoint
class WebUIActivity : BaseActivity() {

    private val userAgent
        get(): String {
            val mmrlVersion = this.managerVersion.second

            val platform = PlatformManager.platform.name

            val platformVersion = PlatformManager.get(-1) {
                moduleManager.versionCode
            }

            val osVersion = Build.VERSION.RELEASE
            val deviceModel = Build.MODEL

            return "WebUI X/$mmrlVersion (Linux; Android $osVersion; $deviceModel; $platform/$platformVersion)"
        }

    private fun WebUI.registerSuPathHandler(
        path: String,
        directory: String,
        authority: String = baseUri.toString(),
    ): WebUI {
        return this.registerPathHandler(SuPathHandler::class.java) {
            add(String::class.java to path)
            add(Uri::class.java to authority.toUri())
            add(String::class.java to directory)
        }
    }

    @OptIn(ExperimentalComposeApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        setMyCrashHandler()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val modId = intent.getModId() ?: throw BrickException("modId cannot be null or empty")

        setBaseContent {
            val prefs = LocalUserPreferences.current
            val isActive by isPlatformAlive()

            if (!isActive) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }

                return@setBaseContent
            }

            val isDebug = prefs.developerMode
            val domain = remember {
                if (isDebug && prefs.useWebUiDevUrl) {
                    prefs.webUiDevUrl
                } else {
                    "https://mui.kernelsu.org"
                }
            }

            var openDevTools by remember { mutableStateOf(false) }
            val wstate = rememberWebUIState(domain) {
                it.settings {
                    useDefaultApplicationInterface = false
                    useDefaultFileSystem = false
                    debug = isDebug
                    forceKillProcess = prefs.forceKillWebUIProcess
                    userAgentString = userAgent
                    useConsoleInterceptor = !prefs.disableConsoleInterceptor
                    darkMode = prefs.isDarkMode()
                    extra = jsonObject {
                        "moduleId" to modId.toString()
                        "enableEruda" to prefs.enableErudaConsole
                        "autoOpenEruda" to prefs.enableAutoOpenEruda
                        "disableGlobalExitConfirm" to prefs.disableGlobalExitConfirm
                        "isRootMode" to prefs.workingMode.isRoot
                    }
                }
                    .client { }
                    .chromeClient { }
                    .registerJavascriptInterface(KernelSUInterface::class.java)
                    .registerJavascriptInterface(ApplicationInterface::class.java) {
                        add(ColorScheme::class.java to prefs.colorScheme(this@WebUIActivity))
                    }
                    .registerSuPathHandler("/.${modId}/", "/data/adb/modules/${modId}")
                    .registerSuPathHandler("/.adb/", "/data/adb")
                    .registerSuPathHandler("/.config/", "/data/adb/.config")
                    .registerSuPathHandler("/.local/", "/data/adb/.local")
//                    .registerSuPathHandler("/", "/", "https://root.mmrl.dev")
                    .registerPathHandler(InternalPathHandler::class.java) {
                        add(ColorScheme::class.java to prefs.colorScheme(this@WebUIActivity))
                    }
                    .registerJavascriptInterface(FileSystemInterface::class.java)
                    .registerPathHandler(WebrootPathHandler::class.java)
            }


//            // TODO: not a long term fix
//            BackHandler {
//                val webui = wstate.webui
//                val webview = webui.webview
//                if (webview.canGoBack()) {
//                    webview.goBack()
//                    return@BackHandler
//                }
//
//                this@WebUIActivity.finish()
//            }


            CompositionLocalProvider(
                LocalWebUI provides wstate.webui
            ) {
                WebUIView(wstate)
                if (prefs.developerMode { enableDevTools }) {
                    DraggableFab(onClick = { openDevTools = true })
                    DevTools(
                        isOpen = openDevTools,
                        onDismissRequest = { openDevTools = false }
                    )
                }
            }
        }

    }
}