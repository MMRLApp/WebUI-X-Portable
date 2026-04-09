@file:Suppress("unused")

package com.dergoogler.mmrl.wx.ui.webui.interfaces

import androidx.compose.material3.ColorScheme
import com.dergoogler.mmrl.wx.ui.webui.alerts.MXConfirm
import com.dergoogler.mmrl.wx.ui.webui.alerts.MXPrompt
import com.dergoogler.mmrl.wx.ui.webui.alerts.Md3Confirm
import com.dergoogler.mmrl.wx.ui.webui.alerts.Md3Prompt
import dev.mmrlx.compose.layout.addOverlayView
import dev.mmrlx.utilities.json.getByPathOrDefault
import dev.mmrlx.webui.WebUI
import dev.mmrlx.webui.interfaces.ExportCallback
import dev.mmrlx.webui.interfaces.ExportMethod
import dev.mmrlx.webui.interfaces.prebuilt.WebUIApplicationInterface
import kotlinx.coroutines.Dispatchers
import org.json.JSONObject

class ApplicationInterface(
    webui: WebUI,
    private val colorScheme: ColorScheme,
) : WebUIApplicationInterface(webui) {
    @ExportMethod
    suspend fun prompt(
        options: JSONObject,
        @ExportCallback onValid: ((String) -> Boolean)? = null,
    ): Promise<String?> {
        return Promise(Dispatchers.Main) {
            val theme = options.optString("theme", "md3")
            val title = options.optString("title", "Confirm")
            val launchKeyboard = options.optBoolean("launchKeyboard", true)
            val confirmText = options.getByPathOrDefault("buttons.confirmText", "Confirm")
            val cancelText = options.getByPathOrDefault("buttons.cancelText", "Cancel")
            val default = options.getByPathOrDefault("default", "")
            val message: String? = options.getString("message")

            if (message == null) {
                reject(Exception("Message must not null"))
                return@Promise
            }

            if (theme == "md3") {
                activity.addOverlayView {
                    this@ApplicationInterface.Md3Prompt(
                        title = title,
                        description = message,
                        value = default,
                        onValid = onValid,
                        onConfirm = {
                            resolve(it)
                        },
                        onClose = {
                            resolve(null)
                        },
                        colorScheme = colorScheme,
                        confirmText = confirmText,
                        cancelText = cancelText,
                        launchKeyboard = launchKeyboard
                    )
                }

                return@Promise
            }

            if (theme == "mmrlx") {
                activity.addOverlayView {
                    this@ApplicationInterface.MXPrompt(
                        title = title,
                        description = message,
                        value = default,
                        onConfirm = {
                            resolve(it)
                        },
                        onClose = {
                            resolve(null)
                        },
                        confirmText = confirmText,
                        cancelText = cancelText,
                        launchKeyboard = launchKeyboard
                    )
                }

                return@Promise
            }

            reject(Exception("Unsupported theme: $theme"))
        }
    }

    @ExportMethod
    override suspend fun confirm(options: JSONObject): Promise<Boolean> {
        return Promise(Dispatchers.Main) {
            val theme = options.optString("theme", "md3")
            val title = options.optString("title", "Confirm")
            val confirmText = options.getByPathOrDefault("buttons.confirmText", "Confirm")
            val cancelText = options.getByPathOrDefault("buttons.cancelText", "Cancel")
            val message: String? = options.getString("message")

            if (message == null) {
                reject(Exception("Message must not null"))
                return@Promise
            }

            if (theme == "md3") {
                activity.addOverlayView {
                    this@ApplicationInterface.Md3Confirm(
                        title = title,
                        description = message,
                        onConfirm = {
                            resolve(true)
                        },
                        onClose = {
                            resolve(false)
                        },
                        colorScheme = colorScheme,
                        confirmText = confirmText,
                        cancelText = cancelText,
                    )
                }

                return@Promise
            }

            if (theme == "mmrlx") {
                activity.addOverlayView {
                    this@ApplicationInterface.MXConfirm(
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

                        )
                }
                return@Promise
            }

            reject(Exception("Unsupported theme: $theme"))
        }
    }
}