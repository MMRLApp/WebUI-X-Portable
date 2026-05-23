package com.dergoogler.mmrl.wx.ui.webui

import com.dergoogler.mmrl.wx.model.module.Module
import dev.mmrlx.nio.SuFile
import dev.mmrlx.webui.WebUI
import dev.mmrlx.webui.WebUISettings
import dev.mmrlx.webui.extra

val WebUI.module: Module
    get() = settings.extra<Module>("module") ?: throw IllegalStateException("Module not set")

val WebUISettings.enableErudaConsole: Boolean
    get() = debug && extra<Boolean>("enableEruda", false)

val WebUISettings.autoOpenEruda: Boolean
    get() = enableErudaConsole && extra<Boolean>("autoOpenEruda", false)

val WebUISettings.disableGlobalExitConfirm: Boolean
    get() = extra<Boolean>("disableGlobalExitConfirm", false)

val WebUISettings.forceKillWebUIProcess: Boolean
    get() = extra<Boolean>("forceKillWebUIProcess", true)

val WebUISettings.isRootMode: Boolean
    get() = extra<Boolean>("isRootMode", false)

fun WebUI.sufile(vararg paths: Any): SuFile {
    val f = file(*paths)

    if (f !is SuFile) {
        throw IllegalArgumentException("Provided file factory is not a `dev.mmrlx.nio.SuFile`")
    }

    return f
}
