@file:Suppress("unused")

package com.dergoogler.mmrl.wx.ui.webui.interfaces

import com.dergoogler.mmrl.wx.ui.webui.module
import com.dergoogler.mmrl.wx.ui.webui.workingMode
import dev.mmrlx.utilities.json.jsonObject
import dev.mmrlx.webui.JavaScriptRegistry

fun JavaScriptRegistry.appInterface() = namespace("webui") {
    val currentRootManager = jsonObject {
        "name" to settings.workingMode.toString
        "versionName" to "-1"
        "versionCode" to -1
    }

    property("currentRootManager", currentRootManager)
    function("getCurrentRootManager") {
        currentRootManager
    }

    function("createShortcut") {
        module.createShortcut(true)
    }

    property("hasShortcut", module.hasShortcut())

    dialogs()
}
