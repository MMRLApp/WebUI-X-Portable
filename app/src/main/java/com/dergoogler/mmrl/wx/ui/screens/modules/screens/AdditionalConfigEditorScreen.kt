package com.dergoogler.mmrl.wx.ui.screens.modules.screens

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.dergoogler.mmrl.ext.none
import com.dergoogler.mmrl.ext.nullable
import com.dergoogler.mmrl.ext.toBooleanOrNull
import com.dergoogler.mmrl.platform.compose.rememberConfigFile
import com.dergoogler.mmrl.platform.content.LocalModule
import com.dergoogler.mmrl.platform.file.config.JSONBoolean.Companion.toJsonBoolean
import com.dergoogler.mmrl.platform.model.ModId
import com.dergoogler.mmrl.ui.component.NavigateUpTopBar
import com.dergoogler.mmrl.ui.component.listItem.dsl.List
import com.dergoogler.mmrl.ui.component.listItem.dsl.ListScope
import com.dergoogler.mmrl.ui.component.listItem.dsl.component.SwitchItem
import com.dergoogler.mmrl.ui.component.listItem.dsl.component.item.Description
import com.dergoogler.mmrl.ui.component.listItem.dsl.component.item.Title
import com.dergoogler.mmrl.webui.model.WebUIConfig
import com.dergoogler.mmrl.webui.model.WebUIConfigAdditionalConfig
import com.dergoogler.mmrl.webui.model.WebUIConfigAdditionalConfigType
import com.dergoogler.mmrl.wx.R
import com.dergoogler.mmrl.wx.ui.providable.LocalDestinationsNavigator
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph

@Destination<RootGraph>()
@Composable
fun AdditionalConfigEditorScreen(module: LocalModule) {
    val navigator = LocalDestinationsNavigator.current
    val modId = module.id

    val (config) = rememberConfigFile(modId.WebUIConfig)

    Scaffold(
        topBar = {
            NavigateUpTopBar(
                title = stringResource(R.string.webui_additional_config),
                subtitle = module.name,
                onBack = { navigator.popBackStack() },
            )
        },
        contentWindowInsets = WindowInsets.none
    ) { innerPadding ->
        List {
            LazyColumn(
                contentPadding = innerPadding
            ) {
                itemsIndexed(
                    items = config.additionalConfig,
                    key = { index, plugin ->
                        index.toString() + plugin.key
                    }
                ) { index, item ->
                    Item(
                        index = index,
                        item = item,
                        modId = modId
                    )
                }
            }
        }
    }
}

@Composable
private fun ListScope.Item(
    index: Int,
    item: WebUIConfigAdditionalConfig,
    modId: ModId,
) {
    val (_, save) = rememberConfigFile(modId.WebUIConfig)

    when (item.type) {
        WebUIConfigAdditionalConfigType.SWITCH -> {
            SwitchItem(
                checked = item.value.toBooleanOrNull() ?: false,
                onChange = { state ->
                    save {
                        val updated = it.additionalConfig.modify(index) {
                            "value" to state.toJsonBoolean()
                        }

                        "additionalConfig" to updated
                    }
                }
            ) {
                Title(item.label ?: item.key)
                item.desc.nullable {
                    Description(it)
                }
            }
        }

        else -> {}
    }
}