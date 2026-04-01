package com.dergoogler.mmrl.wx.ui.screens.modules

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dergoogler.mmrl.modconf.config.toModConfConfig
import com.dergoogler.mmrl.platform.Platform
import com.dergoogler.mmrl.platform.content.LocalModule
import com.dergoogler.mmrl.platform.content.LocalModule.Companion.hasModConf
import com.dergoogler.mmrl.platform.content.LocalModule.Companion.hasWebUI
import com.dergoogler.mmrl.ui.component.dialog.ConfirmData
import com.dergoogler.mmrl.ui.component.dialog.confirm
import com.dergoogler.mmrl.ui.component.scrollbar.VerticalFastScrollbar
import com.dergoogler.mmrl.webui.model.toWebUIConfig
import com.dergoogler.mmrl.wx.R
import com.dergoogler.mmrl.wx.model.module.Module
import com.dergoogler.mmrl.wx.model.module.ModuleState
import com.dergoogler.mmrl.wx.ui.activity.modconf.ModConfActivity
import com.dergoogler.mmrl.wx.ui.activity.webui.WebUIActivity
import com.dergoogler.mmrl.wx.ui.providable.LocalDestinationsNavigator
import java.io.File

@Composable
fun ModulesList(
    list: List<Module>,
    state: LazyListState,
    platform: Platform,
) = Box(
    modifier = Modifier.fillMaxSize()
) {
    LazyColumn(
        state = state,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(
            items = list.filter { it.hasWebUI },
            key = { it.id }
        ) { module ->
            ModuleItem(
                platform = platform,
                module = module,
            )
        }
    }

    VerticalFastScrollbar(
        state = state,
        modifier = Modifier.align(Alignment.CenterEnd)
    )
}

@Composable
fun ModuleItem(
    module: Module,
    platform: Platform,
) {
    val context = LocalContext.current
    val navigator = LocalDestinationsNavigator.current

    ModuleItem(
        module = module,
        indicator = {
            when (module.state) {
                ModuleState.Remove -> StateIndicator(R.drawable.trash)
                ModuleState.Update -> StateIndicator(R.drawable.device_mobile_down)
                else -> {}
            }
        },
        leadingButton = {
            ConfigButton(
                onClick = {
//                    navigator.navigate(ConfigEditorScreenDestination(module))
                },
                enabled = module.state != ModuleState.Remove
            )
        },
        trailingButton = {
//            ShortcutAdd(
//                module = module,
//                enabled = isProviderAlive
//            )

            if (platform.isNonRoot) {
                val colorScheme = MaterialTheme.colorScheme
                RemoveButton {
                    context.confirm(
                        ConfirmData(
                            title = "Remove ${module.name}?",
                            description = "Are you sure that you want to remove this module?",
                            onConfirm = {
                                val file = File(module.paths.moduleDir)

                                if (file.deleteRecursively()) {
                                    Toast.makeText(
                                        context,
                                        "Successfully removed!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return@ConfirmData
                                }

                                Toast.makeText(
                                    context,
                                    "Failed to remove",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                        ),
                        colorScheme
                    )
                }
            }
        }
    )
}

@Composable
private fun ShortcutAdd(
    module: LocalModule,
    enabled: Boolean,
) {
    val webUiConfig = module.id.toWebUIConfig()
    val modConfConfig = module.id.toModConfConfig()

    val context = LocalContext.current

    FilledTonalButton(
        onClick = {
            if (module.hasModConf) {
                modConfConfig.createShortcut(context, ModConfActivity::class.java)
                return@FilledTonalButton
            }

            if (module.hasWebUI) {
                webUiConfig.createShortcut(context, WebUIActivity::class.java)
                return@FilledTonalButton
            }

            Toast.makeText(context, "Unsupported module", Toast.LENGTH_SHORT).show()
        },
        enabled = enabled
                && (webUiConfig.canAddWebUIShortcut() || modConfConfig.canAddWebUIShortcut())
                && !(webUiConfig.hasWebUIShortcut(
            context
        ) || modConfConfig.hasWebUIShortcut(context)),
        contentPadding = PaddingValues(horizontal = 12.dp)
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
) = FilledTonalButton(
    onClick = onClick,
    enabled = enabled,
    contentPadding = PaddingValues(horizontal = 12.dp)
) {
    Icon(
        modifier = Modifier.size(20.dp),
        painter = painterResource(id = R.drawable.settings),
        contentDescription = null
    )
}

@Composable
private fun RemoveButton(
    enabled: Boolean = true,
    onClick: () -> Unit,
) = FilledTonalButton(
    onClick = onClick,
    enabled = enabled,
    contentPadding = PaddingValues(horizontal = 12.dp)
) {
    Icon(
        modifier = Modifier.size(20.dp),
        painter = painterResource(id = R.drawable.trash),
        contentDescription = null
    )
}