package com.dergoogler.mmrl.wx.ui.screens.modules.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dergoogler.mmrl.ext.none
import com.dergoogler.mmrl.ext.shareText
import com.dergoogler.mmrl.platform.content.LocalModule
import com.dergoogler.mmrl.ui.component.BottomSheet
import com.dergoogler.mmrl.ui.component.NavigateUpTopBar
import com.dergoogler.mmrl.ui.component.dialog.RadioOptionItem
import com.dergoogler.mmrl.ui.component.listItem.ListButtonItem
import com.dergoogler.mmrl.ui.component.listItem.ListEditTextItem
import com.dergoogler.mmrl.ui.component.listItem.ListEditTextSwitchItem
import com.dergoogler.mmrl.ui.component.listItem.ListHeader
import com.dergoogler.mmrl.ui.component.listItem.ListItemDefaults
import com.dergoogler.mmrl.ui.component.listItem.ListRadioCheckItem
import com.dergoogler.mmrl.ui.component.listItem.ListSwitchItem
import com.dergoogler.mmrl.webui.model.MutableConfig
import com.dergoogler.mmrl.webui.model.WebUIConfig
import com.dergoogler.mmrl.webui.model.WebUIConfig.Companion.asWebUIConfigFlow
import com.dergoogler.mmrl.wx.R
import com.dergoogler.mmrl.wx.ui.providable.LocalDestinationsNavigator
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.PluginsScreenDestination
import kotlinx.coroutines.launch


private val Context.interceptorList: List<RadioOptionItem<String?>>
    get() = listOf(
        RadioOptionItem(
            value = "native",
            title = getString(R.string.controlled_by_native)
        ),
        RadioOptionItem(
            value = "javascript",
            title = getString(R.string.controlled_by_javascript)
        ),
    )

@Destination<RootGraph>()
@Composable
fun ConfigEditorScreen(module: LocalModule) {
    val navigator = LocalDestinationsNavigator.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val modId = module.id

    val stableFlow = remember(modId) { modId.asWebUIConfigFlow }
    val config by stableFlow.collectAsStateWithLifecycle(WebUIConfig(modId))

//
//    val moduleConfigFile: SuFile = remember {
//        var mfile = modId.moduleDir.fromPaths("config.module.json")
//        if (mfile == null) {
//            mfile = SuFile(modId.moduleDir, "config.module.json")
//            mfile.writeText("{}")
//        }
//        mfile
//    }

    var exportBottomSheet by remember { mutableStateOf(false) }
    if (exportBottomSheet) ExportBottomSheet(
        onClose = { exportBottomSheet = false },
        onModuleExport = {
//            context.shareText(moduleConfigFile.readText())
        },
        onConfigExport = {
            context.shareText(config.toJson())
        }
    )

//    var moduleConfigMap by remember { mutableStateOf<Map<String, Any>?>(null) }


//    LaunchedEffect(moduleConfigFile) {
//        coroutineScope.launch {
//            moduleConfigMap = try {
//                mapAdapter.fromJson(moduleConfigFile.readText()) ?: emptyMap()
//            } catch (e: Exception) {
//                emptyMap()
//            }
//        }
//    }

    fun slave(builderAction: MutableConfig<Any?>.() -> Unit) {
        coroutineScope.launch {
            config.save(builderAction)
        }
    }

//    fun module(key: String, value: Any) {
//        val currentConfig = moduleConfigMap ?: return
//
//        val updatedConfig = currentConfig.toMutableMap().apply {
//            this[key] = value
//        }
//
//        moduleConfigMap = updatedConfig
//
//        coroutineScope.launch {
//            val json = mapAdapter.toJson(updatedConfig)
//            moduleConfigFile.writeText(json)
//        }
//    }

    Scaffold(
        topBar = {
            NavigateUpTopBar(
                title = "Config",
                subtitle = module.name,
                onBack = { navigator.popBackStack() },
                actions = {
                    IconButton(
                        onClick = {
                            exportBottomSheet = true
                        }
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.file_export),
                            contentDescription = null
                        )
                    }
                }
            )
        },
        contentWindowInsets = WindowInsets.none
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            ListHeader(title = stringResource(R.string.webui_config))

            ListEditTextItem(
                title = stringResource(R.string.webui_config_title_title),
                desc = config.title ?: stringResource(R.string.webui_config_title_desc),
                itemTextStyle = ListItemDefaults.itemStyle.apply {
                    if (config.title == null) {
                        copy(
                            descTextStyle = MaterialTheme.typography.bodyMedium.copy(
                                fontStyle = FontStyle.Italic
                            )
                        )
                    }
                },
                value = config.title ?: "",
                onConfirm = {
                    slave {
                        "title" change it
                    }
                }
            )

            ListEditTextItem(
                title = stringResource(R.string.webui_config_icon_title),
                desc = config.icon ?: stringResource(R.string.webui_config_icon_desc),
                itemTextStyle = ListItemDefaults.itemStyle.apply {
                    if (config.icon == null) {
                        copy(
                            descTextStyle = MaterialTheme.typography.bodyMedium.copy(
                                fontStyle = FontStyle.Italic
                            )
                        )
                    }
                },
                value = config.icon ?: "",
                onConfirm = {
                    slave {
                        "icon" change it
                    }
                }
            )

            ListButtonItem(
                title = stringResource(R.string.plugins),
                desc = stringResource(R.string.plugins_desc),
                onClick = {
                    navigator.navigate(PluginsScreenDestination(module))
                }
            )

            val hasNoJsBackInterceptor = config.backInterceptor != "javascript"

            ListSwitchItem(
                enabled = hasNoJsBackInterceptor,
                title = stringResource(R.string.webui_config_exit_confirm_title),
                desc = stringResource(R.string.webui_config_exit_confirm_desc),
                checked = hasNoJsBackInterceptor && config.exitConfirm,
                onChange = { isChecked ->
                    slave {
                        "exitConfirm" change isChecked
                    }
                }
            )

            val backHandler = config.backHandler ?: true

            ListSwitchItem(
                title = stringResource(R.string.webui_config_back_handler_title),
                desc = stringResource(R.string.webui_config_back_handler_desc),
                checked = backHandler,
                onChange = { isChecked ->
                    slave {
                        "backHandler" change isChecked
                    }
                },
            )

            ListRadioCheckItem(
                enabled = backHandler,
                title = stringResource(R.string.webui_config_back_interceptor_title),
                desc = stringResource(R.string.webui_config_back_interceptor_desc),
                value = config.backInterceptor as String?,
                options = context.interceptorList,
                onConfirm = {
                    if (it.value == null) {
                        Toast.makeText(context, "Please select an option", Toast.LENGTH_SHORT)
                            .show()
                        return@ListRadioCheckItem
                    }

                    slave {
                        "backInterceptor" change it.value
                    }
                }
            )

            val pullToRefresh = config.pullToRefresh

            ListSwitchItem(
                title = stringResource(R.string.webui_config_pull_to_refresh_title),
                desc = stringResource(R.string.webui_config_pull_to_refresh_desc),
                checked = pullToRefresh,
                onChange = { isChecked ->
                    slave {
                        "pullToRefresh" change isChecked
                    }
                }
            )

            ListRadioCheckItem(
                enabled = pullToRefresh,
                title = stringResource(R.string.webui_config_refresh_interceptor_title),
                desc = stringResource(R.string.webui_config_refresh_interceptor_desc),
                value = config.refreshInterceptor,
                options = context.interceptorList,
                onConfirm = {
                    if (it.value == null) {
                        Toast.makeText(context, "Please select an option", Toast.LENGTH_SHORT)
                            .show()
                        return@ListRadioCheckItem
                    }

                    slave {
                        "refreshInterceptor" change it.value
                    }
                }
            )

            ListSwitchItem(
                title = stringResource(R.string.webui_config_window_resize_title),
                desc = stringResource(R.string.webui_config_window_resize_desc),
                checked = config.windowResize,
                onChange = { isChecked ->
                    slave {
                        "windowResize" change isChecked
                    }
                }
            )

            ListSwitchItem(
                title = stringResource(R.string.webui_config_auto_style_statusbars_title),
                desc = stringResource(R.string.webui_config_auto_style_statusbars_desc),
                checked = config.autoStatusBarsStyle,
                onChange = { isChecked ->
                    slave {
                        "autoStatusBarsStyle" change isChecked
                    }
                }
            )

            ListSwitchItem(
                title = stringResource(R.string.webui_config_kill_shell_when_background),
                desc = stringResource(R.string.webui_config_kill_shell_when_background_desc),
                checked = config.killShellWhenBackground,
                onChange = { isChecked ->
                    slave {
                        "killShellWhenBackground" change isChecked
                    }
                }
            )

            ListEditTextSwitchItem(
                title = stringResource(R.string.webui_config_history_fallback_title),
                desc = stringResource(R.string.webui_config_history_fallback_desc),
                value = config.historyFallbackFile,
                checked = config.historyFallback,
                onChange = { isChecked ->
                    slave {
                        "historyFallback" change isChecked
                    }
                },
                onConfirm = {
                    slave {
                        "historyFallbackFile" change it
                    }
                }
            )

            ListEditTextItem(
                title = stringResource(R.string.webui_config_content_security_policy_title),
                desc = stringResource(R.string.webui_config_content_security_policy_desc),
                value = config.contentSecurityPolicy,
                onConfirm = {
                    slave {
                        "contentSecurityPolicy" change it
                    }
                }
            )

            ListSwitchItem(
                title = stringResource(R.string.webui_config_caching_title),
                desc = stringResource(R.string.webui_config_caching_desc),
                checked = config.caching,
                onChange = { isChecked ->
                    slave {
                        "caching" change isChecked
                    }
                }
            )

            ListEditTextItem(
                enabled = config.caching,
                title = stringResource(R.string.webui_config_caching_max_age_title),
                desc = stringResource(R.string.webui_config_caching_max_age_desc),
                value = config.cachingMaxAge.toString(),
                onValid = {
                    !Regex("^[0-9]+$").matches(it)
                },
                onConfirm = {
                    slave {
                        "cachingMaxAge" change it.toInt()
                    }
                }
            )

//            ListHeader(title = stringResource(R.string.module_config))

//            moduleConfigMap.nullable { config ->
//                ListRadioCheckItem(
//                    title = stringResource(R.string.settings_webui_engine),
//                    desc = stringResource(R.string.settings_webui_engine_desc),
//                    value = config.getProp("webui-engine", "wx"),
//                    options = listOf(
//                        RadioOptionItem(
//                            value = "wx",
//                            title = stringResource(R.string.settings_webui_engine_wx)
//                        ),
//                        RadioOptionItem(
//                            value = "ksu",
//                            title = stringResource(R.string.settings_webui_engine_ksu)
//                        ),
//                    ),
//                    onConfirm = {
//                        module("webui-engine", it.value)
//                    }
//                )
//            }
        }
    }
}

@Composable
private fun ExportBottomSheet(
    onClose: () -> Unit,
    onModuleExport: () -> Unit,
    onConfigExport: () -> Unit,
) = BottomSheet(
    onDismissRequest = onClose
) {
    Text(
        modifier = Modifier.padding(vertical = 16.dp, horizontal = 25.dp),
        text = stringResource(R.string.export_config),
        style = MaterialTheme.typography.headlineSmall.copy(color = MaterialTheme.colorScheme.primary)
    )

    ListButtonItem(
        title = stringResource(R.string.export_module_config_json),
        onClick = onModuleExport
    )

    ListButtonItem(
        title = stringResource(R.string.export_webui_config_json),
        onClick = onConfigExport
    )

    Spacer(Modifier.height(16.dp))
}