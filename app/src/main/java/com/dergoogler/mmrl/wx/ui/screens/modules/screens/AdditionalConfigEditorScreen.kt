package com.dergoogler.mmrl.wx.ui.screens.modules.screens

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dergoogler.mmrl.ext.none
import com.dergoogler.mmrl.ext.nullable
import com.dergoogler.mmrl.ext.toBooleanOrNull
import com.dergoogler.mmrl.platform.content.LocalModule
import com.dergoogler.mmrl.platform.model.ModId
import com.dergoogler.mmrl.ui.component.NavigateUpTopBar
import com.dergoogler.mmrl.ui.component.listItem.dsl.List
import com.dergoogler.mmrl.ui.component.listItem.dsl.ListScope
import com.dergoogler.mmrl.ui.component.listItem.dsl.component.SwitchItem
import com.dergoogler.mmrl.ui.component.listItem.dsl.component.item.Description
import com.dergoogler.mmrl.ui.component.listItem.dsl.component.item.Title
import com.dergoogler.mmrl.webui.model.JSONBoolean.Companion.toJsonBoolean
import com.dergoogler.mmrl.webui.model.MutableConfig
import com.dergoogler.mmrl.webui.model.WebUIConfig
import com.dergoogler.mmrl.webui.model.WebUIConfig.Companion.asWebUIConfigFlow
import com.dergoogler.mmrl.webui.model.WebUIConfigAdditionalConfig
import com.dergoogler.mmrl.webui.model.WebUIConfigAdditionalConfigType
import com.dergoogler.mmrl.wx.R
import com.dergoogler.mmrl.wx.ui.providable.LocalDestinationsNavigator
import com.dergoogler.mmrl.wx.util.asMutableMap
import com.dergoogler.mmrl.wx.util.toDataClass
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import kotlinx.coroutines.launch

@Destination<RootGraph>()
@Composable
fun AdditionalConfigEditorScreen(module: LocalModule) {
    val navigator = LocalDestinationsNavigator.current
    val modId = module.id

    val stableFlow = remember(modId) { modId.asWebUIConfigFlow }
    val config by stableFlow.collectAsStateWithLifecycle(WebUIConfig(modId))

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
    val coroutineScope = rememberCoroutineScope()
    val stableFlow = remember(modId) { modId.asWebUIConfigFlow }
    val config by stableFlow.collectAsStateWithLifecycle(WebUIConfig(modId))

    fun slave(builderAction: MutableConfig<Any?>.(WebUIConfig) -> Unit) {
        coroutineScope.launch {
            config.save(builderAction)
        }
    }

    when (item.type) {
        WebUIConfigAdditionalConfigType.SWITCH -> {
            SwitchItem(
                checked = item.value.toBooleanOrNull() ?: false,
                onChange = { state ->
                    slave {
                        val updated = it.additionalConfig.toMutableList().apply {
                            val old = this[index].asMutableMap()
                            old["value"] = state.toJsonBoolean()
                            this[index] = old.toDataClass<WebUIConfigAdditionalConfig>()
                        }

                        "additionalConfig" change updated
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