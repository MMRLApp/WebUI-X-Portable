package com.dergoogler.mmrl.wx.ui.screens.modules

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dergoogler.mmrl.platform.PlatformManager.platform
import com.dergoogler.mmrl.platform.content.State
import com.dergoogler.mmrl.wx.R
import com.dergoogler.mmrl.wx.model.module.Module
import com.dergoogler.mmrl.wx.ui.providable.LocalDestinationsNavigator
import dev.mmrlx.compose.ui.Text
import dev.mmrlx.compose.ui.button.Button
import dev.mmrlx.compose.ui.button.ButtonSize
import dev.mmrlx.compose.ui.button.ButtonVariant
import dev.mmrlx.compose.ui.dialog.Content
import dev.mmrlx.compose.ui.dialog.Footer
import dev.mmrlx.compose.ui.dialog.Title
import dev.mmrlx.compose.ui.dialog.rememberDialog
import dev.mmrlx.compose.ui.ext.with
import dev.mmrlx.compose.ui.icon.Icon
import dev.mmrlx.compose.ui.scaffold.ScaffoldScope
import java.io.File
import com.ramcosta.composedestinations.generated.destinations.ConfigEditorScreenDestination

@Composable
fun ScaffoldScope.ModulesList(
    list: List<Module>,
    state: LazyListState,
) {
    LazyColumn(
        state = state,
        modifier = Modifier.with(this@ModulesList) { it.scaffoldHazeSource() },
        contentPadding = PaddingValues(
            top = this@ModulesList.scaffoldTopPadding + 8.dp,
            start = 8.dp,
            end = 8.dp,
            bottom = this@ModulesList.scaffoldBottomPadding + 8.dp
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = list.filter { it.hasWebUI },
            key = { it.id }
        ) { module ->
            ModuleItem(
                module = module,
                placeholder = null
            )
        }
    }
//
//    VerticalFastScrollbar(
//        state = state,
//        modifier = Modifier.align(Alignment.CenterEnd)
//    )
}

@Composable
fun ModuleItem(
    module: Module,
    placeholder: Nothing?,
) {
    val context = LocalContext.current
    val navigator = LocalDestinationsNavigator.current

    val removeDialog = rememberDialog()

    ModuleItem(
        module = module,
        indicator = {
            when (module.state) {
                State.REMOVE,
                    -> StateIndicator(R.drawable.trash)

                State.UPDATE -> StateIndicator(R.drawable.device_mobile_down)
                else -> {}
            }
        },
        leadingButton = {
            ConfigButton(
                onClick = {
                       navigator.navigate(ConfigEditorScreenDestination(module.id))
                },
                enabled = module.state != State.REMOVE
            )
        },
        trailingButton = {
            ShortcutAdd(
                module = module,
            )

            if (platform.isNonRoot) {
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

@Composable
private fun ShortcutAdd(
    module: Module,
) {
//    val webUiConfig = module.id.toWebUIConfig()
//    val modConfConfig = module.id.toModConfConfig()
    val context = LocalContext.current

    Button(
        onClick = {
            if (module.hasWebUI) {
//                webUiConfig.createShortcut(context, WebUIActivity::class.java)
                return@Button
            }

            Toast.makeText(context, "Unsupported module", Toast.LENGTH_SHORT).show()
        },
//        enabled = enabled
//                && (webUiConfig.canAddWebUIShortcut() || modConfConfig.canAddWebUIShortcut())
//                && !(webUiConfig.hasWebUIShortcut(
//            context
//        ) || modConfConfig.hasWebUIShortcut(context)),
        variant = ButtonVariant.Outline,
        size = ButtonSize.Sm
    ) {
        Icon(
            modifier = Modifier.size(20.dp),
            painter = painterResource(id = R.drawable.link),
            contentDescription = null
        )

        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = stringResource(id = R.string.add_shortcut)
        )
    }
}

@Composable
private fun ConfigButton(
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        variant = ButtonVariant.Outline,
        size = ButtonSize.Sm
    ) {
        Icon(
            modifier = Modifier.size(20.dp),
            painter = painterResource(id = R.drawable.settings),
            contentDescription = null

        )
    }
}

@Composable
private fun RemoveButton(
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        variant = ButtonVariant.Destructive,
        size = ButtonSize.Sm
    ) {
        Icon(
            modifier = Modifier.size(20.dp),
            painter = painterResource(id = R.drawable.trash),
            contentDescription = null

        )
    }
}