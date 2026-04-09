@file:Suppress("UnusedReceiverParameter")

package com.dergoogler.mmrl.wx.ui.webui.alerts

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.dergoogler.mmrl.ui.component.dialog.ConfirmDialog
import com.dergoogler.mmrl.wx.ui.webui.interfaces.ApplicationInterface
import dev.mmrlx.compose.ui.Text
import dev.mmrlx.compose.ui.button.Button
import dev.mmrlx.compose.ui.button.ButtonVariant
import dev.mmrlx.compose.ui.dialog.Content
import dev.mmrlx.compose.ui.dialog.Footer
import dev.mmrlx.compose.ui.dialog.Title
import dev.mmrlx.compose.ui.dialog.rememberDialog
import dev.mmrlx.compose.ui.theme.MMRLXTheme


@Composable
fun ApplicationInterface.Md3Confirm(
    title: String,
    description: String?,
    onConfirm: () -> Unit,
    onClose: () -> Unit,
    confirmText: String,
    cancelText: String,
    colorScheme: ColorScheme,
) {
    var showDialog by remember { mutableStateOf(true) }

    val done = {
        showDialog = false
        onConfirm()
    }

    val close = {
        showDialog = false
        onClose()
    }

    if (showDialog) {
        MaterialTheme(colorScheme = colorScheme) {
            ConfirmDialog(
                onDismissRequest = close,
                closeText = cancelText,
                confirmText = confirmText,
                title = title,
                description = description ?: "",
                onClose = close,
                onConfirm = done
            )
        }
    }
}

@Composable
fun ApplicationInterface.MXConfirm(
    title: String,
    description: String?,
    onConfirm: () -> Unit,
    onClose: () -> Unit,
    confirmText: String,
    cancelText: String,
) {
    MMRLXTheme(
        darkTheme = isDarkMode
    ) {
        val dialog = rememberDialog(true)

        val done = {
                dialog.close()
                onConfirm()
            }

        val close = {
            dialog.close()
            onClose()
        }

        dialog {
            Title {
                Text(title)
            }

            Content {
                Text(description ?: "")
            }

            Footer {
                Button(
                    onClick = close,
                    variant = ButtonVariant.Outline
                ) {
                    Text(cancelText)
                }
                Button(
                    onClick = done,
                ) {
                    Text(confirmText)
                }
            }
        }
    }
}