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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.dergoogler.mmrl.ext.isNotNullOrEmpty
import com.dergoogler.mmrl.ext.none
import com.dergoogler.mmrl.ext.shareText
import com.dergoogler.mmrl.platform.compose.rememberConfigFile
import com.dergoogler.mmrl.platform.content.LocalModule
import com.dergoogler.mmrl.platform.model.ModuleConfig
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
import com.dergoogler.mmrl.webui.model.WebUIConfig
import com.dergoogler.mmrl.wx.R
import com.dergoogler.mmrl.wx.ui.providable.LocalDestinationsNavigator
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.AdditionalConfigEditorScreenDestination
import com.ramcosta.composedestinations.generated.destinations.PluginsScreenDestination


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
    val modId = module.id


    val (webUIConfig, saveWebUIConfig) = rememberConfigFile(modId.WebUIConfig)
    val (moduleConfig, saveModuleConfig) = rememberConfigFile(modId.ModuleConfig)

    var exportBottomSheet by remember { mutableStateOf(false) }
    if (exportBottomSheet) ExportBottomSheet(
        onClose = { exportBottomSheet = false },
        onModuleExport = {
            context.shareText(moduleConfig.getOverrideConfigFile(modId)?.readText() ?: "{}")
        },
        onConfigExport = {
            context.shareText(webUIConfig.getOverrideConfigFile(modId)?.readText() ?: "{}")
        }
    )

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
                desc = webUIConfig.title ?: stringResource(R.string.webui_config_title_desc),
                itemTextStyle = ListItemDefaults.itemStyle.apply {
                    if (webUIConfig.title == null) {
                        copy(
                            descTextStyle = MaterialTheme.typography.bodyMedium.copy(
                                fontStyle = FontStyle.Italic
                            )
                        )
                    }
                },
                value = webUIConfig.title ?: "",
                onConfirm = {
                    saveWebUIConfig { _ ->
                        "title" change it
                    }
                }
            )

            ListEditTextItem(
                title = stringResource(R.string.webui_config_icon_title),
                desc = webUIConfig.icon ?: stringResource(R.string.webui_config_icon_desc),
                itemTextStyle = ListItemDefaults.itemStyle.apply {
                    if (webUIConfig.icon == null) {
                        copy(
                            descTextStyle = MaterialTheme.typography.bodyMedium.copy(
                                fontStyle = FontStyle.Italic
                            )
                        )
                    }
                },
                value = webUIConfig.icon ?: "",
                onConfirm = {
                    saveWebUIConfig { _ ->
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

            if (webUIConfig.additionalConfig.isNotNullOrEmpty()) {
                ListButtonItem(
                    title = stringResource(R.string.webui_additional_config),
                    desc = stringResource(R.string.webui_additional_config_desc),
                    onClick = {
                        navigator.navigate(AdditionalConfigEditorScreenDestination(module))
                    }
                )
            }

            val hasNoJsBackInterceptor = webUIConfig.backInterceptor != "javascript"

            ListSwitchItem(
                enabled = hasNoJsBackInterceptor,
                title = stringResource(R.string.webui_config_exit_confirm_title),
                desc = stringResource(R.string.webui_config_exit_confirm_desc),
                checked = hasNoJsBackInterceptor && webUIConfig.exitConfirm,
                onChange = { isChecked ->
                    saveWebUIConfig {
                        "exitConfirm" change isChecked
                    }
                }
            )

            val backHandler = webUIConfig.backHandler ?: true

            ListSwitchItem(
                title = stringResource(R.string.webui_config_back_handler_title),
                desc = stringResource(R.string.webui_config_back_handler_desc),
                checked = backHandler,
                onChange = { isChecked ->
                    saveWebUIConfig {
                        "backHandler" change isChecked
                    }
                },
            )

            ListRadioCheckItem(
                enabled = backHandler,
                title = stringResource(R.string.webui_config_back_interceptor_title),
                desc = stringResource(R.string.webui_config_back_interceptor_desc),
                value = webUIConfig.backInterceptor as String?,
                options = context.interceptorList,
                onConfirm = {
                    if (it.value == null) {
                        Toast.makeText(context, "Please select an option", Toast.LENGTH_SHORT)
                            .show()
                        return@ListRadioCheckItem
                    }

                    saveWebUIConfig { _ ->
                        "backInterceptor" change it.value
                    }
                }
            )

            val pullToRefresh = webUIConfig.pullToRefresh

            ListSwitchItem(
                title = stringResource(R.string.webui_config_pull_to_refresh_title),
                desc = stringResource(R.string.webui_config_pull_to_refresh_desc),
                checked = pullToRefresh,
                onChange = { isChecked ->
                    saveWebUIConfig {
                        "pullToRefresh" change isChecked
                    }
                }
            )

            ListRadioCheckItem(
                enabled = pullToRefresh,
                title = stringResource(R.string.webui_config_refresh_interceptor_title),
                desc = stringResource(R.string.webui_config_refresh_interceptor_desc),
                value = webUIConfig.refreshInterceptor,
                options = context.interceptorList,
                onConfirm = { item ->
                    if (item.value == null) {
                        Toast.makeText(context, "Please select an option", Toast.LENGTH_SHORT)
                            .show()
                        return@ListRadioCheckItem
                    }

                    saveWebUIConfig { _ ->
                        "refreshInterceptor" change item.value
                    }
                }
            )

            ListSwitchItem(
                title = stringResource(R.string.webui_config_window_resize_title),
                desc = stringResource(R.string.webui_config_window_resize_desc),
                checked = webUIConfig.windowResize,
                onChange = { isChecked ->
                    saveWebUIConfig {
                        "windowResize" change isChecked
                    }
                }
            )

            ListSwitchItem(
                title = stringResource(R.string.webui_config_auto_style_statusbars_title),
                desc = stringResource(R.string.webui_config_auto_style_statusbars_desc),
                checked = webUIConfig.autoStatusBarsStyle,
                onChange = { isChecked ->
                    saveWebUIConfig {
                        "autoStatusBarsStyle" change isChecked
                    }
                }
            )

            ListSwitchItem(
                title = stringResource(R.string.webui_config_kill_shell_when_background),
                desc = stringResource(R.string.webui_config_kill_shell_when_background_desc),
                checked = webUIConfig.killShellWhenBackground,
                onChange = { isChecked ->
                    saveWebUIConfig {
                        "killShellWhenBackground" change isChecked
                    }
                }
            )

            ListEditTextSwitchItem(
                title = stringResource(R.string.webui_config_history_fallback_title),
                desc = stringResource(R.string.webui_config_history_fallback_desc),
                value = webUIConfig.historyFallbackFile,
                checked = webUIConfig.historyFallback,
                onChange = { isChecked ->
                    saveWebUIConfig {
                        "historyFallback" change isChecked
                    }
                },
                onConfirm = {
                    saveWebUIConfig { _ ->
                        "historyFallbackFile" change it
                    }
                }
            )

            ListEditTextItem(
                title = stringResource(R.string.webui_config_content_security_policy_title),
                desc = stringResource(R.string.webui_config_content_security_policy_desc),
                value = webUIConfig.contentSecurityPolicy,
                onConfirm = {
                    saveWebUIConfig { _ ->
                        "contentSecurityPolicy" change it
                    }
                }
            )

            ListSwitchItem(
                title = stringResource(R.string.webui_config_caching_title),
                desc = stringResource(R.string.webui_config_caching_desc),
                checked = webUIConfig.caching,
                onChange = { isChecked ->
                    saveWebUIConfig {
                        "caching" change isChecked
                    }
                }
            )

            ListEditTextItem(
                enabled = webUIConfig.caching,
                title = stringResource(R.string.webui_config_caching_max_age_title),
                desc = stringResource(R.string.webui_config_caching_max_age_desc),
                value = webUIConfig.cachingMaxAge.toString(),
                onValid = {
                    !Regex("^[0-9]+$").matches(it)
                },
                onConfirm = {
                    saveWebUIConfig { _ ->
                        "cachingMaxAge" change it.toInt()
                    }
                }
            )


            ListHeader(title = stringResource(R.string.module_config))

            val engine by remember(moduleConfig) {
                derivedStateOf {
                    moduleConfig.getWebuiEngine(context)
                }
            }

            ListRadioCheckItem(
                title = stringResource(R.string.settings_webui_engine),
                value = engine,
                options = listOf(
                    RadioOptionItem(
                        value = "wx",
                        title = stringResource(R.string.settings_webui_engine_wx)
                    ),
                    RadioOptionItem(
                        value = "ksu",
                        title = stringResource(R.string.settings_webui_engine_ksu)
                    ),
                    RadioOptionItem(
                        value = null,
                        title = stringResource(R.string.settings_webui_engine_undefined)
                    )
                ),
                onConfirm = {
                    saveModuleConfig { _ ->
                        "webui-engine" change it.value
                    }
                }
            )
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