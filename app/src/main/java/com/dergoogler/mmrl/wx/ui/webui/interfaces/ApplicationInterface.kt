@file:Suppress("unused")

package com.dergoogler.mmrl.wx.ui.webui.interfaces

import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.dergoogler.mmrl.wx.ui.webui.alerts.Confirm
import com.dergoogler.mmrl.wx.ui.webui.alerts.Prompt
import com.dergoogler.mmrl.wx.ui.webui.alerts.fromString
import com.dergoogler.mmrl.wx.ui.webui.module
import com.dergoogler.mmrl.wx.ui.webui.workingMode
import dev.mmrlx.compose.layout.addOverlayView
import dev.mmrlx.utilities.json.getAs
import dev.mmrlx.utilities.json.getByPathOrDefault
import dev.mmrlx.utilities.json.jsonObject
import dev.mmrlx.webui.JavaScriptRegistry
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

    asyncFunction("prompt") {
        val deferred = CompletableDeferred<String?>()
        val options = checkObject(0)

        val theme = options.getAs<String?>("theme", null)
        val title = options.getAs<String>("title", "Confirm")
        val launchKeyboard = options.getAs<Boolean>("launchKeyboard", true)
        val confirmText = options.getByPathOrDefault<String>("buttons.confirmText", "Confirm")
        val cancelText = options.getByPathOrDefault<String>("buttons.cancelText", "Cancel")
        val defaultValue = options.getAs<String>("defaultValue", "")
        val supportingText = options.getAs<String?>("supportingText", null)
        val message = options.getAs<String?>("message", null)
            ?: throw IllegalArgumentException("Message must not null")

        val keyboardType = options.getAs<String>("keyboardType", "done").let {
            KeyboardType.fromString(it)
        }
        val imeAction = options.getAs<String>("imeAction", "text").let {
            ImeAction.fromString(it)
        }

        withContext(Dispatchers.Main) {
            activity.addOverlayView {
                this@appInterface.Prompt(
                    title = title,
                    description = message,
                    value = defaultValue,
                    onConfirm = {
                        deferred.complete(it)
                    },
                    onClose = {
                        deferred.complete(null)
                    },
                    confirmText = confirmText,
                    cancelText = cancelText,
                    launchKeyboard = launchKeyboard,
                    keyboardType = keyboardType,
                    imeAction = imeAction,
                    theme = theme,
                    supportingText = supportingText
                )
            }
        }

        return@asyncFunction deferred.await()
    }

    asyncFunction("confirm") {
        val deferred = CompletableDeferred<Boolean>()
        val options = checkObject(0)

        val theme = options.getAs<String?>("theme", null)
        val title = options.getAs<String>("title", "Confirm")
        val confirmText = options.getByPathOrDefault("buttons.confirmText", "Confirm")
        val cancelText = options.getByPathOrDefault("buttons.cancelText", "Cancel")
        val message = options.getAs<String?>("message", null)
            ?: throw IllegalArgumentException("Message must not null")

        withContext(Dispatchers.Main) {
            activity.addOverlayView {
                this@appInterface.Confirm(
                    title = title,
                    description = message,
                    onConfirm = {
                        deferred.complete(true)
                    },
                    onClose = {
                        deferred.complete(false)
                    },
                    confirmText = confirmText,
                    cancelText = cancelText,
                    theme = theme
                )
            }
        }

        return@asyncFunction deferred.await()
    }
}
