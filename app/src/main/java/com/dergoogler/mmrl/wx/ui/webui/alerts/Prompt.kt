@file:Suppress("UnusedReceiverParameter")

package com.dergoogler.mmrl.wx.ui.webui.alerts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.dergoogler.mmrl.ui.component.dialog.TextFieldDialog
import com.dergoogler.mmrl.wx.ui.webui.interfaces.ApplicationInterface
import dev.mmrlx.compose.ui.button.Button
import dev.mmrlx.compose.ui.button.ButtonVariant
import dev.mmrlx.compose.ui.dialog.Content
import dev.mmrlx.compose.ui.dialog.Footer
import dev.mmrlx.compose.ui.dialog.Title
import dev.mmrlx.compose.ui.dialog.rememberDialog
import dev.mmrlx.compose.ui.icon.Icon
import dev.mmrlx.compose.ui.text.Input
import dev.mmrlx.compose.ui.text.rememberInputState
import dev.mmrlx.compose.ui.theme.MMRLXTheme
import dev.mmrlx.ui.R

@Composable
fun ApplicationInterface.Md3Prompt(
    title: String,
    description: String?,
    value: String,
    onValid: ((String) -> Boolean)? = null,
    onConfirm: (String) -> Unit,
    onClose: () -> Unit,
    confirmText: String,
    cancelText: String,
    colorScheme: ColorScheme,
    launchKeyboard: Boolean,
) {
    var text by remember { mutableStateOf(value) }
    var isError by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(true) }

    if (showDialog) {
        MaterialTheme(colorScheme = colorScheme) {
            val onDone = {
                showDialog = false
                onConfirm(text)
            }

            val onClose = {
                showDialog = false
                onClose()
            }

            onValid?.let { c ->
                LaunchedEffect(c, text) {
                    isError = !c(text)
                }
            }

            TextFieldDialog(
                onDismissRequest = onClose,
                title = {
                    Text(text = title)
                },
                confirmButton = {
                    TextButton(
                        onClick = onDone,
                        enabled = !isError && text.isNotBlank(),
                    ) {
                        Text(confirmText)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = onClose,
                    ) {
                        Text(cancelText)
                    }
                },
                launchKeyboard = launchKeyboard
            ) { focusRequester ->
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    if (description != null) {
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }

                    OutlinedTextField(
                        modifier = Modifier.focusRequester(focusRequester),
                        textStyle = MaterialTheme.typography.bodyLarge,
                        value = text,
                        onValueChange = {
                            if (onValid != null) {
                                isError = !onValid(it)
                            }
                            text = it
                        },
                        singleLine = false,
                        isError = isError,
                        keyboardOptions =
                            KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Done,
                            ),
                        keyboardActions =
                            KeyboardActions {
                                if (text.isNotBlank()) onDone()
                            },
                        shape = RoundedCornerShape(15.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun ApplicationInterface.MXPrompt(
    title: String,
    description: String?,
    value: String,
    onConfirm: (String) -> Unit,
    onClose: () -> Unit,
    confirmText: String,
    cancelText: String,
    launchKeyboard: Boolean,
) {
    MMRLXTheme(darkTheme = isDarkMode) {
        val dialog = rememberDialog(true)
        val state = rememberInputState(value)

        val done = remember(state) {
            {
                dialog.close()
                onConfirm(state.text.toString())
            }
        }

        val close = {
            dialog.close()
            onClose()
        }

        val focusRequester = remember { FocusRequester() }
        val keyboardController = LocalSoftwareKeyboardController.current

        LaunchedEffect(focusRequester) {
            if (launchKeyboard) {
                focusRequester.requestFocus()
                keyboardController?.show()
            }
        }

        dialog {
            Title {
                dev.mmrlx.compose.ui.Text(title)
            }

            Content(
                contentPadding = PaddingValues(0.dp),
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    if (description != null) {
                        dev.mmrlx.compose.ui.Text(description)
                    }

                    Input(
                        modifier = Modifier.focusRequester(focusRequester),
                        state = state,
                        keyboardOptions =
                            KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Search,
                            ),
                        onKeyboardAction = {
                            keyboardController?.hide()
                        },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(id = R.drawable.search),
                                contentDescription = null,
                            )
                        },
                        textStyle = MMRLXTheme.typography.bodyLarge,
                    )
                }
            }

            Footer {
                Button(
                    onClick = done,
                    variant = ButtonVariant.Outline
                ) {
                    dev.mmrlx.compose.ui.Text(confirmText)
                }
                Button(
                    onClick = close,
                ) {
                    dev.mmrlx.compose.ui.Text(cancelText)
                }
            }
        }
    }
}