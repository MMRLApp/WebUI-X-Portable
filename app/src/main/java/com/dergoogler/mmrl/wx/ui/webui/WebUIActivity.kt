package com.dergoogler.mmrl.wx.ui.webui

import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import com.dergoogler.mmrl.ext.exception.BrickException
import com.dergoogler.mmrl.ext.managerVersion
import com.dergoogler.mmrl.platform.PlatformManager
import com.dergoogler.mmrl.wx.datastore.model.WorkingMode.Companion.isRoot
import com.dergoogler.mmrl.wx.datastore.providable.LocalUserPreferences
import com.dergoogler.mmrl.wx.model.module.Module
import com.dergoogler.mmrl.wx.model.module.ModuleUIState
import com.dergoogler.mmrl.wx.ui.component.DraggableFab
import com.dergoogler.mmrl.wx.ui.component.ErrorContent
import com.dergoogler.mmrl.wx.ui.component.LoadingContent
import com.dergoogler.mmrl.wx.ui.webui.devtools.DevTools
import com.dergoogler.mmrl.wx.ui.webui.devtools.LocalWebUI
import com.dergoogler.mmrl.wx.ui.webui.interfaces.ApplicationInterface
import com.dergoogler.mmrl.wx.ui.webui.interfaces.FileSystemInterface
import com.dergoogler.mmrl.wx.ui.webui.interfaces.KernelSUInterface
import com.dergoogler.mmrl.wx.ui.webui.pathHandlers.InternalPathHandler
import com.dergoogler.mmrl.wx.ui.webui.pathHandlers.SuPathHandler
import com.dergoogler.mmrl.wx.ui.webui.pathHandlers.WebrootPathHandler
import com.dergoogler.mmrl.wx.ui.webui.util.luaPlugin
import com.dergoogler.mmrl.wx.util.BaseActivity
import com.dergoogler.mmrl.wx.util.setBaseContent
import com.dergoogler.mmrl.wx.util.setMyCrashHandler
import dagger.hilt.android.AndroidEntryPoint
import dev.mmrlx.compose.webui.WebUIView
import dev.mmrlx.compose.webui.rememberWebUIState
import dev.mmrlx.nio.SuFile
import dev.mmrlx.nio.SuFileInputStream
import dev.mmrlx.nio.SuFileOutputStream
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

        val moduleId = intent.getStringExtra("MODULE_ID")
            ?: throw BrickException("moduleId cannot be null or empty")

        setBaseContent {
            val prefs = LocalUserPreferences.current
            val state by Module.rememberCreate(moduleId)

            val colorScheme = remember {
                prefs.colorScheme(this@WebUIActivity)
            }

            when (state) {
                ModuleUIState.Loading -> {
                    LoadingContent()
                }

                is ModuleUIState.Error -> {
                    ErrorContent((state as ModuleUIState.Error).message)
                }

                is ModuleUIState.Ready -> {
                    val module = (state as ModuleUIState.Ready).module

                    val isDebug = prefs.developerMode

                    val domain = remember {
                        if (isDebug && prefs.useWebUiDevUrl) {
                            prefs.webUiDevUrl
                        } else {
                            "https://mui.kernelsu.org"
                        }
                    }

                    var openDevTools by remember {
                        mutableStateOf(false)
                    }

                    val wstate = rememberWebUIState(domain) {
                        it
                            .factories {
                                inputStreamFactory { paths ->
                                    SuFileInputStream(paths.first)
                                }

                                outputStreamFactory { paths ->
                                    SuFileOutputStream(paths.first)
                                }

                                fileFactory { paths ->
                                    SuFile(*paths)
                                }
                            }
                            .settings {
                                useDefaultApplicationInterface = false
                                useDefaultFileSystem = false
                                debug = isDebug
                                forceKillProcess =
                                    prefs.forceKillWebUIProcess
                                userAgentString = userAgent
                                useConsoleInterceptor =
                                    !prefs.disableConsoleInterceptor
                                darkMode = prefs.isDarkMode()

                                extra = mapOf(
                                    "module" to module,
                                    "enableEruda" to prefs.enableErudaConsole,
                                    "autoOpenEruda" to prefs.enableAutoOpenEruda,
                                    "disableGlobalExitConfirm" to prefs.disableGlobalExitConfirm,
                                    "isRootMode" to prefs.workingMode.isRoot,
                                )
                            }
                            .backHandlers(colorScheme)
                            .client { }
                            .chromeClient { }
                            .luaPlugin()
                            .registerJavascriptInterface(
                                KernelSUInterface::class.java
                            )
                            .registerJavascriptInterface(
                                ApplicationInterface::class.java
                            ) {
                                add(
                                    ColorScheme::class.java to colorScheme
                                )
                            }
                            .registerJavascriptInterface(
                                FileSystemInterface::class.java
                            )
                            .registerPathHandler(
                                WebrootPathHandler::class.java
                            )
                            .registerSuPathHandler(
                                "/.${module.id}/",
                                module.path.moduleDir
                            )
                            .registerSuPathHandler(
                                "/.adb/",
                                module.adbPath.baseDir
                            )
                            .registerSuPathHandler(
                                "/.config/",
                                module.adbPath.configDir
                            )
                            .registerSuPathHandler(
                                "/.local/",
                                module.adbPath.localDir
                            )
                            .registerPathHandler(
                                InternalPathHandler::class.java
                            ) {
                                add(
                                    ColorScheme::class.java to
                                            prefs.colorScheme(this@WebUIActivity)
                                )
                            }
                    }

                    CompositionLocalProvider(
                        LocalWebUI provides wstate.webui
                    ) {
                        WebUIView(wstate)

                        if (prefs.developerMode { enableDevTools }) {
                            DraggableFab(
                                onClick = { openDevTools = true }
                            )

                            DevTools(
                                isOpen = openDevTools,
                                onDismissRequest = {
                                    openDevTools = false
                                }
                            )
                        }
                    }
                }
            }
        }

    }

}