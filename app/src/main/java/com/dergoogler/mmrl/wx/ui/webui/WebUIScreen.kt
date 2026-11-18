package com.dergoogler.mmrl.wx.ui.webui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.systemBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.dergoogler.mmrl.ext.none
import com.dergoogler.mmrl.wx.datastore.providable.LocalUserPreferences
import com.dergoogler.mmrl.wx.ui.component.LocalModule
import com.dergoogler.mmrl.wx.ui.component.ModuleScope
import com.dergoogler.mmrl.wx.ui.providable.LocalBrowser
import com.dergoogler.mmrl.wx.ui.providable.LocalDestinationsNavigator
import com.dergoogler.mmrl.wx.ui.webui.components.ContextMenu
import com.dergoogler.mmrl.wx.ui.webui.pathHandlers.webrootPathHandler
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import dev.mmrlx.compose.ui.scaffold.Scaffold
import dev.mmrlx.compose.webui.WebUIRecomposer
import dev.mmrlx.compose.webui.WebUIView
import dev.mmrlx.compose.webui.insets
import dev.mmrlx.compose.webui.rememberWebUIState
import dev.mmrlx.webui.WebUIContextMenu

@Destination<RootGraph>
@Composable
fun WebUIScreen(id: String, isNavigationStack: Boolean = false) {
    ModuleScope(id, toolbar = false) {
        WebUIRecomposer {
            if (isNavigationStack) {
                Scaffold(
                    contentWindowInsets = WindowInsets.none
                ) {


                    WebUIScreen0(isNavigationStack)
                }

                return@WebUIRecomposer
            }

            WebUIScreen0(isNavigationStack)
        }
    }
}

@Composable
private fun WebUIScreen0(isNavigationStack: Boolean = false) {
    val navigator = LocalDestinationsNavigator.current
    val browser = LocalBrowser.current
    val module = LocalModule.current
    val context = LocalContext.current
    val prefs = LocalUserPreferences.current
    val paddingInsets = WindowInsets.systemBars.asPaddingValues()

    val colorScheme = remember {
        prefs.colorScheme(context)
    }

    var contextMenu by remember { mutableStateOf<WebUIContextMenu?>(null) }

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
            .base(
                module = module,
                colorScheme = colorScheme,
                prefs = prefs,
                isDebug = isDebug
            )
            .factories {
                // in no activity mode `activity.finish()` will close the app. override it.
                if (isNavigationStack) {
                    exitFactory {
                        navigator.popBackStack()
                    }
                }
            }
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
            .apply {
                if (isNavigationStack) {
                    insets(paddingInsets)
                }
            }
            .registerRoutes { webrootPathHandler(module.path.webrootDir) }
    }

    DisposableEffect(Unit) {
        onDispose {
            wstate.destroy()
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
