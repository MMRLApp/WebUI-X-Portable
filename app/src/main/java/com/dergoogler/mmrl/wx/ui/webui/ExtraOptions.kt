package com.dergoogler.mmrl.wx.ui.webui

import com.dergoogler.mmrl.platform.file.SuFile
import com.dergoogler.mmrl.platform.model.ModId
import com.dergoogler.mmrl.platform.model.ModId.Companion.toModId
import com.dergoogler.mmrl.platform.model.ModId.Companion.webrootDir
import dev.mmrlx.webui.WebUI
import dev.mmrlx.webui.WebUISettings
import dev.mmrlx.webui.extra
import org.json.JSONObject

val WebUI.modId: ModId get() = settings.extra<String>("moduleId", "").toModId()

val WebUISettings.enableErudaConsole: Boolean
    get() = debug && extra<Boolean>("enableEruda", false)

val WebUISettings.autoOpenEruda: Boolean
    get() = enableErudaConsole && extra<Boolean>("autoOpenEruda", false)

val WebUISettings.disableGlobalExitConfirm: Boolean
    get() = extra<Boolean>("disableGlobalExitConfirm", false)

val WebUISettings.isRootMode: Boolean
    get() = extra<Boolean>("isRootMode", false)

val WebUI.moduleConfig: JSONObject
    get() {
        val configFile = SuFile(modId.webrootDir, "config.json")
        val configText = configFile.newInputStream().reader().use { it.readText() }

        return try {
            JSONObject(configText)
        } catch (e: Exception) {
            console.error("Failed to parse config.json", e)
            JSONObject()
        }
    }