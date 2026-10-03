package com.dergoogler.mmrl.wx.ui.screens.modules.components

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.dergoogler.mmrl.platform.content.State
import com.dergoogler.mmrl.wx.R
import com.dergoogler.mmrl.wx.datastore.providable.LocalUserPreferences
import com.dergoogler.mmrl.wx.model.module.Module
import com.dergoogler.mmrl.wx.ui.providable.LocalDestinationsNavigator
import com.dergoogler.mmrl.wx.ui.webui.WebUIActivity
import com.ramcosta.composedestinations.generated.destinations.ConfigEditorScreenDestination
import com.ramcosta.composedestinations.generated.destinations.ShortcutCreateScreenDestination
import dev.mmrlx.compose.ui.Text
import dev.mmrlx.compose.ui.button.Button
import dev.mmrlx.compose.ui.button.ButtonVariant
import dev.mmrlx.compose.ui.dialog.Content
import dev.mmrlx.compose.ui.dialog.Footer
import dev.mmrlx.compose.ui.dialog.Title
import dev.mmrlx.compose.ui.dialog.rememberDialog
import java.io.File

@Composable
fun ModuleItem(module: Module) {
    val context = LocalContext.current
    val prefs = LocalUserPreferences.current
    val navigator = LocalDestinationsNavigator.current

    val removeDialog = rememberDialog()

    ModuleItem0(
        module = module,
        onClick = {
            WebUIActivity.start(context, module.id)
        },
        indicator = {
            when (module.state) {
                State.REMOVE,
                    -> StateIndicator(R.drawable.trash)

                State.UPDATE -> StateIndicator(R.drawable.device_mobile_down)
                else -> {}
            }
        },
        leadingButton = {
            OptionButtonGroup(
                enabled = module.state != State.REMOVE,
                onConfigClick = {
                    navigator.navigate(ConfigEditorScreenDestination(module.id))
                },
                onShortcutClick = {
                    navigator.navigate(ShortcutCreateScreenDestination(module.id))
                },
            )
        },
        trailingButton = {
            if (prefs.isNonRoot) {
                RemoveButton {
                    removeDialog.open()
                }
            }
        }
    )

    removeDialog {
        Title {
            Text("Remove ${module.name}?")
        }

        Content {
            Text("Are you sure that you want to remove this module?")
        }

        Footer {
            Button(
                onClick = {
                    removeDialog.close()
                },
                variant = ButtonVariant.Outline
            ) {
                Text(stringResource(R.string.cancel))
            }

            Button(
                variant = ButtonVariant.Destructive,
                onClick = {
                    val file = File(module.path.moduleDir)

                    if (file.deleteRecursively()) {
                        Toast.makeText(
                            context,
                            "Successfully removed!",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@Button
                    }

                    Toast.makeText(
                        context,
                        "Failed to remove",
                        Toast.LENGTH_SHORT
                    ).show()

                    removeDialog.close()
                },
            ) {
                Text(stringResource(R.string.confirm))
            }
        }

    }
}