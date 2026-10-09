package com.dergoogler.mmrl.wx.ui.webui

import android.os.Build
import android.system.OsConstants.O_RDONLY
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.dergoogler.mmrl.ext.managerVersion
import com.dergoogler.mmrl.wx.datastore.model.WorkingMode
import com.dergoogler.mmrl.wx.datastore.model.WorkingMode.Companion.isRoot
import com.dergoogler.mmrl.wx.datastore.providable.LocalUserPreferences
import com.dergoogler.mmrl.wx.ui.component.LocalModule
import com.dergoogler.mmrl.wx.ui.providable.LocalBrowser
import com.dergoogler.mmrl.wx.ui.webui.components.ContextMenu
import com.dergoogler.mmrl.wx.ui.webui.interfaces.appInterface
import com.dergoogler.mmrl.wx.ui.webui.interfaces.fileSystemInterface
import com.dergoogler.mmrl.wx.ui.webui.interfaces.ksu.kernelSUInterface
import com.dergoogler.mmrl.wx.ui.webui.interfaces.legacy.legacyFileInputInterface
import com.dergoogler.mmrl.wx.ui.webui.interfaces.legacy.legacyFileOutputInterface
import com.dergoogler.mmrl.wx.ui.webui.interfaces.legacy.legacyModuleInterface
import com.dergoogler.mmrl.wx.ui.webui.interfaces.moduleInterface
import com.dergoogler.mmrl.wx.ui.webui.pathHandlers.internalPathHandler
import com.dergoogler.mmrl.wx.ui.webui.pathHandlers.ksu.iconPathHandler
import com.dergoogler.mmrl.wx.ui.webui.pathHandlers.suRoute
import com.dergoogler.mmrl.wx.ui.webui.pathHandlers.webrootPathHandler
import com.dergoogler.mmrl.wx.ui.webui.util.dexPlugin
import com.dergoogler.mmrl.wx.ui.webui.util.luaPlugin
import com.dergoogler.mmrl.wx.util.withNewRootShell
import com.topjohnwu.superuser.ShellUtils
import dev.mmrlx.compose.webui.WebUIView
import dev.mmrlx.compose.webui.rememberWebUIState
import dev.mmrlx.nio.SuFile
import dev.mmrlx.nio.SuFileInputStream
import dev.mmrlx.nio.SuFileOutputStream
import dev.mmrlx.nio.SuRandomAccessFile
import dev.mmrlx.webui.WebUIContextMenu

@Composable
fun WebUIScreen() {
    val browser = LocalBrowser.current
    val module = LocalModule.current
    val context = LocalContext.current
    val prefs = LocalUserPreferences.current

    val colorScheme = remember {
        prefs.colorScheme(context)
    }

    var contextMenu by remember { mutableStateOf<WebUIContextMenu?>(null) }

    val userAgent = remember {
        val versionCode = context.managerVersion.second

        val platform = prefs.workingMode

        val platformVersion = if (platform == WorkingMode.MODE_NON_ROOT) {
            -1
        } else {
            (withNewRootShell { ShellUtils.fastCmd(this, "su -V") }).toIntOrNull() ?: -1
        }

        val osVersion = Build.VERSION.RELEASE
        val deviceModel = Build.MODEL

        "WebUI X/$versionCode (Linux; Android $osVersion; $deviceModel; ${platform.toString}/$platformVersion)"
    }

    val isDebug = prefs.developerMode

    val domain = remember {
        if (isDebug && prefs.useWebUiDevUrl) {
            prefs.webUiDevUrl
        } else {
            "https://mui.kernelsu.org"
        }
    }

    val wstate = rememberWebUIState(domain) {
        it
            .factories {
                inputStreamFactory { paths ->
                    val path = paths.first
                    val mode: Int = paths[1, O_RDONLY]
                    SuFileInputStream(SuFile(path), mode, 0)
                }

                outputStreamFactory { paths ->
                    val path = paths.first
                    val append = paths[1, false]
                    SuFileOutputStream(path, append)
                }

                randomAccessFileFactory { paths ->
                    val path = paths.first
                    val mode = paths[1, "r"]
                    SuRandomAccessFile(path, mode)
                }

                fileFactory { paths ->
                    SuFile(*paths)
                }
            }
            .settings {
                schemeWhitelist += "ksu"
                useDefaultApplicationInterface = false
                useDefaultFileSystem = false
                debug = isDebug
                forceKillProcess =
                    prefs.forceKillWebUIProcess
                userAgentString = userAgent
                darkMode = prefs.isDarkMode()
                showEventPayloadInConsole = prefs.logEventPayload

                extra = mapOf(
                    "module" to module,
                    "enableEruda" to prefs.enableErudaConsole,
                    "autoOpenEruda" to prefs.enableAutoOpenEruda,
                    "disableGlobalExitConfirm" to prefs.disableGlobalExitConfirm,
                    "isRootMode" to prefs.workingMode.isRoot,
                    "workingMode" to prefs.workingMode,
                    "mdColorScheme" to colorScheme,
                )
            }
            .registerJavaScriptInterfaces {
                moduleInterface()
                kernelSUInterface()
                appInterface()
                fileSystemInterface()
                // legacy
                legacyModuleInterface()
                legacyFileInputInterface()
                legacyFileOutputInterface()
            }
            .backHandlers()
            .client {
                onUntrustedUrl { uri ->
                    browser.open(uri)
                }

                if (prefs.enableContextMenuInWebUI) {
                    onContextMenu { state ->
                        contextMenu = state
                    }
                }
            }
            .chromeClient { }
            .luaPlugin()
            .dexPlugin()
            .registerRoutes {
                // ksu://icon/
                iconPathHandler()
            }
            .registerRoutes {
                internalPathHandler()
                suRoute(
                    "/.${module.id}/",
                    module.path.moduleDir
                )

                suRoute(
                    "/.adb/",
                    module.adbPath.baseDir
                )
                suRoute(
                    "/.config/",
                    module.adbPath.configDir
                )
                suRoute(
                    "/.local/",
                    module.adbPath.localDir
                )

                webrootPathHandler()
            }
    }

    WebUIView(wstate)

    val currentMenu = contextMenu
    if (prefs.enableContextMenuInWebUI && currentMenu != null) {
        ContextMenu(
            webui = wstate,
            menu = currentMenu,
            onDismiss = { contextMenu = null }
        )
    }
}
