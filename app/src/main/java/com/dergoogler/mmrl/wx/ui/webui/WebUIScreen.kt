package com.dergoogler.mmrl.wx.ui.webui

import android.net.Uri
import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import com.dergoogler.mmrl.ext.managerVersion
import com.dergoogler.mmrl.platform.PlatformManager
import com.dergoogler.mmrl.wx.datastore.model.WorkingMode
import com.dergoogler.mmrl.wx.datastore.model.WorkingMode.Companion.isRoot
import com.dergoogler.mmrl.wx.datastore.providable.LocalUserPreferences
import com.dergoogler.mmrl.wx.ui.component.DraggableFab
import com.dergoogler.mmrl.wx.ui.component.LocalModule
import com.dergoogler.mmrl.wx.ui.webui.devtools.DevTools
import com.dergoogler.mmrl.wx.ui.webui.devtools.LocalWebUI
import com.dergoogler.mmrl.wx.ui.webui.interfaces.ApplicationInterface
import com.dergoogler.mmrl.wx.ui.webui.interfaces.FileSystemInterface
import com.dergoogler.mmrl.wx.ui.webui.interfaces.KernelSUInterface
import com.dergoogler.mmrl.wx.ui.webui.interfaces.legacy.FileInputInterface
import com.dergoogler.mmrl.wx.ui.webui.interfaces.legacy.FileOutputInterface
import com.dergoogler.mmrl.wx.ui.webui.interfaces.legacy.ModuleInterface
import com.dergoogler.mmrl.wx.ui.webui.pathHandlers.InternalPathHandler
import com.dergoogler.mmrl.wx.ui.webui.pathHandlers.SuPathHandler
import com.dergoogler.mmrl.wx.ui.webui.pathHandlers.WebrootPathHandler
import com.dergoogler.mmrl.wx.ui.webui.util.luaPlugin
import dev.mmrlx.compose.webui.WebUIView
import dev.mmrlx.compose.webui.rememberWebUIState
import dev.mmrlx.nio.SuFile
import dev.mmrlx.nio.SuFileInputStream
import dev.mmrlx.nio.SuFileOutputStream
import dev.mmrlx.webui.WebUI

@Composable
fun WebUIScreen() {
    val module = LocalModule.current
    val context = LocalContext.current
    val prefs = LocalUserPreferences.current

    val colorScheme = remember {
        prefs.colorScheme(context)
    }

    val userAgent = remember {
        val mmrlVersion = context.managerVersion.second

        val platform = when (prefs.workingMode) {
            WorkingMode.MODE_NON_ROOT -> "NonRoot"
            WorkingMode.MODE_MAGISK -> "Magisk"
            WorkingMode.MODE_KERNEL_SU -> "KernelSU"
            WorkingMode.MODE_KERNEL_SU_NEXT -> "KsuNext"
            WorkingMode.MODE_APATCH -> "APatch"
            WorkingMode.MODE_MKSU -> "MKSU"
            WorkingMode.MODE_RKSU -> "RKSU"
            WorkingMode.MODE_SUKISU -> "SukiSu"
            else -> "Unknown"
        }

        val platformVersion = PlatformManager.get(-1) {
            moduleManager.versionCode
        }

        val osVersion = Build.VERSION.RELEASE
        val deviceModel = Build.MODEL

        "WebUI X/$mmrlVersion (Linux; Android $osVersion; $deviceModel; $platform/$platformVersion)"
    }

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
                    val path = paths.first
                    val append = paths.append
                    SuFileOutputStream(path, append)
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
                    "workingMode" to prefs.workingMode
                )
            }
            // legacy interfaces
            .registerJavascriptInterface(ModuleInterface::class.java)
            .registerJavascriptInterface(FileInputInterface::class.java)
            .registerJavascriptInterface(FileOutputInterface::class.java)
            // end
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
                    ColorScheme::class.java to colorScheme
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