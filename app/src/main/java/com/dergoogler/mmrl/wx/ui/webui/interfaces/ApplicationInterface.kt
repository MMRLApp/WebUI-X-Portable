@file:Suppress("unused")

package com.dergoogler.mmrl.wx.ui.webui.interfaces

import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.dergoogler.mmrl.wx.ui.webui.alerts.Confirm
import com.dergoogler.mmrl.wx.ui.webui.alerts.Prompt
import com.dergoogler.mmrl.wx.ui.webui.alerts.fromString
import com.dergoogler.mmrl.wx.ui.webui.workingMode
import dev.mmrlx.compose.layout.addOverlayView
import dev.mmrlx.utilities.json.getAs
import dev.mmrlx.utilities.json.getByPathOrDefault
import dev.mmrlx.utilities.json.jsonObject
import dev.mmrlx.webui.WebUI
import dev.mmrlx.webui.interfaces.prebuilt.WebUIApplicationInterface
import dev.mmrlx.webui.javascript.annotation.ExportMethod
import kotlinx.coroutines.Dispatchers
import org.json.JSONObject

class ApplicationInterface(webui: WebUI) : WebUIApplicationInterface(webui) {

    @ExportMethod
    fun getCurrentRootManager(): JSONObject {
        return jsonObject {
            "name" to settings.workingMode.toString
            "versionName" to "-1"
            "versionCode" to -1
        }
    }

    @ExportMethod
    suspend fun prompt(
        options: JSONObject?,
    ): Promise<String?> {
        return Promise(Dispatchers.Main) {
            val theme = options.getAs<String?>("theme", null)
            val title = options.getAs<String>("title", "Confirm")
            val launchKeyboard = options.getAs<Boolean>("launchKeyboard", true)
            val confirmText = options.getByPathOrDefault<String>("buttons.confirmText", "Confirm")
            val cancelText = options.getByPathOrDefault<String>("buttons.cancelText", "Cancel")
            val defaultValue = options.getAs<String>("defaultValue", "")
            val supportingText = options.getAs<String?>("supportingText", null)
            val message = options.getAs<String?>("message", null)

            val keyboardType = options.getAs<String>("keyboardType", "done").let {
                KeyboardType.fromString(it)
            }
            val imeAction = options.getAs<String>("imeAction", "text").let {
                ImeAction.fromString(it)
            }

            if (message == null) {
                reject(Exception("Message must not null"))
                return@Promise
            }

            activity.addOverlayView {
                this@ApplicationInterface.Prompt(
                    title = title,
                    description = message,
                    value = defaultValue,
                    onConfirm = {
                        resolve(it)
                    },
                    onClose = {
                        resolve(null)
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
    }

    @ExportMethod
    suspend fun confirm(options: JSONObject?): Promise<Boolean> {
        return Promise(Dispatchers.Main) {
            val theme = options.getAs<String?>("theme", null)
            val title = options.getAs<String>("title", "Confirm")
            val confirmText = options.getByPathOrDefault("buttons.confirmText", "Confirm")
            val cancelText = options.getByPathOrDefault("buttons.cancelText", "Cancel")
            val message = options.getAs<String?>("message", null)

            activity.addOverlayView {
                this@ApplicationInterface.Confirm(
                    title = title,
                    description = message,
                    onConfirm = {
                        resolve(true)
                    },
                    onClose = {
                        resolve(false)
                    },
                    confirmText = confirmText,
                    cancelText = cancelText,
                    theme = theme
                )
            }
        }
    }
}