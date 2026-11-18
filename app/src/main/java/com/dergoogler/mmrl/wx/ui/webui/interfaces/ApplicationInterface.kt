@file:Suppress("unused")

package com.dergoogler.mmrl.wx.ui.webui.interfaces

import com.dergoogler.mmrl.wx.ui.webui.module
import com.dergoogler.mmrl.wx.ui.webui.overlay.WebUIOverlay
import com.dergoogler.mmrl.wx.ui.webui.overlay.WebUIOverlayActivity
import com.dergoogler.mmrl.wx.ui.webui.workingMode
import dev.mmrlx.utilities.json.jsonObject
import dev.mmrlx.webui.JavaScriptRegistry
import dev.mmrlx.webui.overlay.OverlayService
import dev.mmrlx.webui.overlay.isOverlay

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

    activity {
        dialogs()
    }

    if (!isOverlay) {
        function("launchOverlay") {
            val directory = checkString(0)
            val alpha = optFloat(1, 0.8f)
            activity?.runOnUiThread {
                OverlayService.start<WebUIOverlay>(kontext) {
                    putExtra(WebUIOverlayActivity.EXTRA_MODULE_ID, module.id)
                    putExtra(WebUIOverlayActivity.EXTRA_DIRECTORY, directory)
                    putExtra(OverlayService.EXTRA_ALPHA, alpha)
                }
            }
        }
    }
}
