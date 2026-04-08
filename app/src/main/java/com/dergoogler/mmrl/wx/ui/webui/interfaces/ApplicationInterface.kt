package com.dergoogler.mmrl.wx.ui.webui.interfaces

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.unit.dp
import com.dergoogler.mmrl.ui.component.dialog.ConfirmData
import com.dergoogler.mmrl.ui.component.dialog.confirm
import dev.mmrlx.compose.layout.addOverlayView
import dev.mmrlx.compose.ui.Text
import dev.mmrlx.compose.ui.button.Button
import dev.mmrlx.compose.ui.button.ButtonVariant
import dev.mmrlx.compose.ui.dialog.Content
import dev.mmrlx.compose.ui.dialog.Footer
import dev.mmrlx.compose.ui.dialog.Title
import dev.mmrlx.compose.ui.dialog.rememberDialog
import dev.mmrlx.compose.ui.theme.MMRLXTheme
import dev.mmrlx.utilities.json.getByPathOrDefault
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
                activity.confirm(
                    colorScheme = colorScheme,
                    confirmData = ConfirmData(
                        title = title,
                        description = message,
                        confirmText = confirmText,
                        closeText = cancelText,
                        onConfirm = {
                            resolve(true)
                        },
                        onClose = {
                            resolve(false)
                        },
                    )
                )
                return@Promise
            }

            if (theme == "mmrlx") {
                activity.addOverlayView {
                    MMRLXTheme(
                        darkTheme = isDarkMode
                    ) {
                        val dialog = rememberDialog(true)

                        dialog {
                            Title {
                                Text(title)
                            }

                            Content(
                                contentPadding = PaddingValues(0.dp),
                            ) {
                                Text(message)
                            }

                            Footer {
                                Button(
                                    onClick = {
                                        dialog.close()
                                        resolve(false)
                                    },
                                    variant = ButtonVariant.Outline
                                ) {
                                    Text(confirmText)
                                }
                                Button(
                                    onClick = {
                                        dialog.close()
                                        resolve(true)
                                    },
                                ) {
                                    Text(cancelText)
                                }
                            }
                        }
                    }
                }
                return@Promise
            }

            reject(Exception("Unsupported theme: $theme"))
        }
    }
}