package com.dergoogler.mmrl.wx.ui.screens.modules

import com.dergoogler.mmrl.platform.content.LocalModule
import com.dergoogler.mmrl.platform.content.State
import com.dergoogler.mmrl.wx.R
import com.dergoogler.mmrl.ui.component.scrollbar.VerticalFastScrollbar
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dergoogler.mmrl.ext.navigateSingleTopTo
import com.dergoogler.mmrl.platform.content.LocalModule.Companion.hasWebUI
import com.dergoogler.mmrl.ui.providable.LocalNavController
import com.dergoogler.mmrl.webui.model.WebUIConfig
import com.dergoogler.mmrl.webui.model.WebUIConfig.Companion.webUiConfig
import com.dergoogler.mmrl.wx.ui.activity.webui.WebUIActivity
import com.dergoogler.mmrl.wx.ui.navigation.graphs.ModulesScreen

@Composable
fun ModulesList(
    list: List<LocalModule>,
    state: LazyListState,
    isProviderAlive: Boolean,
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
                isProviderAlive = isProviderAlive,
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
    module: LocalModule,
    isProviderAlive: Boolean,
) {
    val navController = LocalNavController.current

    ModuleItem(
        module = module,
        indicator = {
            when (module.state) {
                State.REMOVE -> StateIndicator(R.drawable.trash)
                State.UPDATE -> StateIndicator(R.drawable.device_mobile_down)
                else -> {}
            }
        },
        leadingButton = {
            ConfigButton(
                onClick = {
                    navController.navigateSingleTopTo(
                        route =  ModulesScreen.Config.route,
                        args = mapOf("id" to module.id.toString())
                    )
                },
                enabled = module.state != State.REMOVE
            )
        },
        trailingButton = {
            val config = module.webUiConfig

            ShortcutAdd(
                config = config,
                enabled = isProviderAlive && config.canAddWebUIShortcut()
            )
        }
    )
}


@Composable
private fun ShortcutAdd(
    config: WebUIConfig,
    enabled: Boolean,
) {
    val context = LocalContext.current

    FilledTonalButton(
        onClick = {
            config.createShortcut(context, WebUIActivity::class.java)
        },
        enabled = enabled && !config.hasWebUIShortcut(context),
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