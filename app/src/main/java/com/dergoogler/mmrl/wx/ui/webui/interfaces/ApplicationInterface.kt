@file:Suppress("unused")

package com.dergoogler.mmrl.wx.ui.webui.interfaces

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.core.content.pm.PackageInfoCompat
import com.dergoogler.mmrl.wx.ui.webui.alerts.MXConfirm
import com.dergoogler.mmrl.wx.ui.webui.alerts.MXPrompt
import com.dergoogler.mmrl.wx.ui.webui.alerts.Md3Confirm
import com.dergoogler.mmrl.wx.ui.webui.alerts.Md3Prompt
import com.dergoogler.mmrl.wx.ui.webui.alerts.fromString
import com.dergoogler.mmrl.wx.ui.webui.workingMode
import dev.mmrlx.compose.layout.addOverlayView
import dev.mmrlx.utilities.json.getAs
import dev.mmrlx.utilities.json.getByPathOrDefault
import dev.mmrlx.utilities.json.jsonObject
import dev.mmrlx.webui.WebUI
import dev.mmrlx.webui.interfaces.ExportMethod
import dev.mmrlx.webui.interfaces.prebuilt.WebUIApplicationInterface
import kotlinx.coroutines.Dispatchers
import org.json.JSONObject

class ApplicationInterface(
    webui: WebUI,
    private val colorScheme: ColorScheme,
) : WebUIApplicationInterface(webui) {

    @ExportMethod
    fun getCurrentRootManager(): JSONObject {
        return jsonObject {
            "name" to settings.workingMode.toString
            "versionName" to "-1"
            "versionCode" to -1
        }
    }

    @ExportMethod
    fun getCurrentApplication(): JSONObject {
        val packageInfo = kontext.packageManager.getPackageInfo(kontext.packageName, 0)
        val versionCode = PackageInfoCompat.getLongVersionCode(packageInfo)
        val versionName = packageInfo.versionName ?: "unknown"

        return jsonObject {
            "name" to packageInfo.packageName
            "versionName" to versionName
            "versionCode" to versionCode
        }
    }

    @ExportMethod
    suspend fun prompt(
        options: JSONObject?,
    ): Promise<String?> {
        return Promise(Dispatchers.Main) {
            val theme = options.getAs<String>("theme", "md3")
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

            if (theme == "md3") {
                activity.addOverlayView {
                    this@ApplicationInterface.Md3Prompt(
                        title = title,
                        description = message,
                        value = defaultValue,
                        onConfirm = {
                            resolve(it)
                        },
                        onClose = {
                            resolve(null)
                        },
                        colorScheme = colorScheme,
                        confirmText = confirmText,
                        cancelText = cancelText,
                        launchKeyboard = launchKeyboard,
                        keyboardType = keyboardType,
                        imeAction = imeAction
                    )
                }

                return@Promise
            }

            if (theme == "mmrlx") {
                activity.addOverlayView {
                    this@ApplicationInterface.MXPrompt(
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
                        supportingText = supportingText,
                        keyboardType = keyboardType,
                        imeAction = imeAction
                    )
                }

                return@Promise
            }

            reject(Exception("Unsupported theme: $theme"))
        }
    }

    @ExportMethod
    suspend fun confirm(options: JSONObject?): Promise<Boolean> {
        return Promise(Dispatchers.Main) {
            val theme = options.getAs<String>("theme", "md3")
            val title = options.getAs<String>("title", "Confirm")
            val confirmText = options.getByPathOrDefault("buttons.confirmText", "Confirm")
            val cancelText = options.getByPathOrDefault("buttons.cancelText", "Cancel")
            val message = options.getAs<String?>("message", null)

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