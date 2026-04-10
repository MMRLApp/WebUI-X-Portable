@file:Suppress("UnusedReceiverParameter")

package com.dergoogler.mmrl.wx.ui.webui.alerts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
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
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
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
import dev.mmrlx.compose.ui.list.DialogItemSlot
import dev.mmrlx.compose.ui.text.OutlinedInput
import dev.mmrlx.compose.ui.text.rememberInputState
import dev.mmrlx.compose.ui.theme.MMRLXTheme

@Composable
internal fun ApplicationInterface.Md3Prompt(
    title: String,
    description: String?,
    value: String,
    onConfirm: (String) -> Unit,
    onClose: () -> Unit,
    confirmText: String,
    cancelText: String,
    colorScheme: ColorScheme,
    launchKeyboard: Boolean,
    imeAction: ImeAction,
    keyboardType: KeyboardType,
) {
    var text by remember { mutableStateOf(value) }
    var showDialog by remember { mutableStateOf(true) }

    val done = remember {
        {
            showDialog = false
            onConfirm(text)
        }
    }

    val close = {
        showDialog = false
        onClose()
    }

    if (showDialog) {
        MaterialTheme(
            colorScheme = colorScheme
        ) {
            TextFieldDialog(
                onDismissRequest = close,
                title = {
                    Text(text = title)
                },
                confirmButton = {
                    TextButton(
                        onClick = done,
                        enabled = text.isNotBlank(),
                    ) {
                        Text(confirmText)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = close,
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
                            text = it
                        },
                        singleLine = false,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = keyboardType,
                            imeAction = imeAction,
                        ),
                        keyboardActions = KeyboardActions {
                            if (text.isNotBlank()) done()
                        },
                        shape = RoundedCornerShape(15.dp),
                    )
                }
            }
        }
    }
}

@Composable
internal fun ApplicationInterface.MXPrompt(
    title: String,
    description: String?,
    value: String,
    onConfirm: (String) -> Unit,
    onClose: () -> Unit,
    confirmText: String,
    cancelText: String,
    supportingText: String?,
    launchKeyboard: Boolean,
    imeAction: ImeAction,
    keyboardType: KeyboardType,
) {
    MMRLXTheme(darkTheme = isDarkMode) {
        val dialog = rememberDialog(true)
        val state = rememberInputState(value)

        val focusRequester = remember { FocusRequester() }
        val keyboardController = LocalSoftwareKeyboardController.current

        LaunchedEffect(focusRequester) {
            if (launchKeyboard) {
                focusRequester.requestFocus()
                keyboardController?.show()
            }
        }

        val data =
            remember(state.text) {
                state.text.toString()
            }

        dialog.onClose {
            state.clearText()
            state.setTextAndPlaceCursorAtEnd(value)
        }

        val done: () -> Unit = remember(state.text) {
            {
                onConfirm(data)
                dialog.close()
            }
        }

        val close = {
            dialog.close()
            onClose()
        }

        dialog {
            Title {
                dev.mmrlx.compose.ui.Text(title)
            }

            Content {
                Layout(
                    content = {
                        if (description != null) {
                            dev.mmrlx.compose.ui.Text(
                                modifier = Modifier.layoutId(DialogItemSlot.Description),
                                text = description
                            )
                        }

                        OutlinedInput(
                            state = state,
                            modifier = Modifier
                                .focusRequester(focusRequester)
                                .layoutId(DialogItemSlot.Input)
                                .fillMaxWidth(),
                            textStyle = MMRLXTheme.typography.bodyLarge,
                            supportingText = supportingText?.let {
                                {
                                    dev.mmrlx.compose.ui.Text(it)
                                }
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = keyboardType,
                                imeAction = imeAction,
                            ),
                            onKeyboardAction = KeyboardActionHandler {
                                if (state.text.isNotBlank()) done()
                            },
                        )
                    },
                ) { measurables, constraints ->
                    val spacing = 16.dp.roundToPx()

                    val descriptionPlaceable =
                        measurables
                            .firstOrNull { it.layoutId == DialogItemSlot.Description }
                            ?.measure(constraints)
                    val textFieldPlaceable =
                        measurables
                            .first { it.layoutId == DialogItemSlot.Input }
                            .measure(constraints)

                    val totalHeight =
                        listOfNotNull(
                            descriptionPlaceable?.height,
                            spacing.takeIf { descriptionPlaceable != null },
                            textFieldPlaceable.height,
                        ).sum()

                    layout(constraints.maxWidth, totalHeight) {
                        var y = 0

                        descriptionPlaceable?.let {
                            it.placeRelative(0, y)
                            y += it.height + spacing
                        }

                        textFieldPlaceable.placeRelative(0, y)
                    }
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

internal fun KeyboardType.Companion.fromString(value: String): KeyboardType {
    return when(value.lowercase()) {
        "ascii" -> KeyboardType.Ascii
        "number" -> KeyboardType.Number
        "phone" -> KeyboardType.Phone
        "uri" -> KeyboardType.Uri
        "email" -> KeyboardType.Email
        "password" -> KeyboardType.Password
        "numberpassword" -> KeyboardType.NumberPassword
        "decimal" -> KeyboardType.Decimal
        else -> KeyboardType.Text
    }
}

internal fun ImeAction.Companion.fromString(value: String): ImeAction {
    return when(value) {
        "done" -> ImeAction.Done
        "go" -> ImeAction.Go
        "next" -> ImeAction.Next
        "previous" -> ImeAction.Previous
        "search" -> ImeAction.Search
        "send" -> ImeAction.Send
        else -> ImeAction.Done
    }
}