package com.dergoogler.mmrl.wx.ui.webui

import com.dergoogler.mmrl.platform.file.SuFile
import com.dergoogler.mmrl.platform.model.ModId
import com.dergoogler.mmrl.platform.model.ModId.Companion.toModId
import com.dergoogler.mmrl.platform.model.ModId.Companion.webrootDir
import dev.mmrlx.webui.WebUI
import dev.mmrlx.webui.WebUISettings
import org.json.JSONObject

val WebUI.modId: ModId get() = settings.extra.getString("moduleId").toModId()

val WebUISettings.enableErudaConsole: Boolean
    get() = debug && extra.optBoolean("enableEruda", false)

val WebUISettings.autoOpenEruda: Boolean
    get() = enableErudaConsole && extra.optBoolean("autoOpenEruda", false)

val WebUISettings.disableGlobalExitConfirm: Boolean
    get() = extra.optBoolean("disableGlobalExitConfirm", false)

val WebUISettings.isRootMode: Boolean
    get() = extra.optBoolean("isRootMode", false)

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