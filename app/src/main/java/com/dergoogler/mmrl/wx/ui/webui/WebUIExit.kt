package com.dergoogler.mmrl.wx.ui.webui

import androidx.compose.material3.ColorScheme
import com.dergoogler.mmrl.ui.component.dialog.ConfirmData
import com.dergoogler.mmrl.ui.component.dialog.confirm
import com.dergoogler.mmrl.wx.R
import com.dergoogler.mmrl.wx.model.module.WebrootConfig
import com.dergoogler.mmrl.wx.model.module.backInterceptor
import com.dergoogler.mmrl.wx.model.module.exitConfirm
import dev.mmrlx.webui.WebUI
import dev.mmrlx.webui.WebUIInterceptorType
import kotlin.system.exitProcess

private fun WebUI.handleNativeExit(colorScheme: ColorScheme, config: WebrootConfig) {
    if (webview.canGoBack()) {
        webview.goBack()
        return
    }

    if (settings.disableGlobalExitConfirm) {
        exit()
        return
    }

    with(kontext) {

        if (config.exitConfirm) {
            kontext.confirm(
                confirmData = ConfirmData(
                    title = getString(R.string.exit),
                    description = getString(R.string.exit_desc),
                    onConfirm = { exit() },
                    onClose = {}
                ),
                colorScheme = colorScheme
            )
            return
        }
    }

    exit()
}

fun WebUI.backHandlers(colorScheme: ColorScheme): WebUI {
    val config = module.webrootConfig

    val backInterceptor = when (config.backInterceptor) {
        "native" -> WebUIInterceptorType.NATIVE
        "javascript" -> WebUIInterceptorType.JAVASCRIPT
        "javascript-full" -> WebUIInterceptorType.JAVASCRIPT_FULL
        else -> WebUIInterceptorType.NATIVE
    }

    return this
        .settings {
            backEventType = backInterceptor
        }.backEvents {
            onBackPressed {
                when (backInterceptor) {
                    WebUIInterceptorType.NATIVE -> {
                        handleNativeExit(colorScheme, config)
                    }

                    WebUIInterceptorType.JAVASCRIPT -> {
                        guardWebViewState(::emitBackPressed)
                    }

                    WebUIInterceptorType.JAVASCRIPT_FULL -> {
                        emitBackPressed()
                    }
                }
            }
        }
}

fun WebUI.exit() {
    if (!settings.forceKillWebUIProcess) {
        activity.finish()
        return
    }

    activity.finish()
    android.os.Process.killProcess(android.os.Process.myPid())
    exitProcess(0)
}