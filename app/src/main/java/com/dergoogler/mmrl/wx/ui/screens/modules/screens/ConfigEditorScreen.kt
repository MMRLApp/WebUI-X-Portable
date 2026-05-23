package com.dergoogler.mmrl.wx.ui.screens.modules.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import com.dergoogler.mmrl.ext.none
import com.dergoogler.mmrl.ui.component.LabelItem
import com.dergoogler.mmrl.ui.component.NavigateUpTopBar
import com.dergoogler.mmrl.ui.component.dialog.RadioOptionItem
import com.dergoogler.mmrl.ui.component.listItem.ListEditTextItem
import com.dergoogler.mmrl.ui.component.listItem.ListEditTextSwitchItem
import com.dergoogler.mmrl.ui.component.listItem.ListHeader
import com.dergoogler.mmrl.ui.component.listItem.ListItemDefaults
import com.dergoogler.mmrl.ui.component.listItem.ListRadioCheckItem
import com.dergoogler.mmrl.ui.component.listItem.ListSwitchItem
import com.dergoogler.mmrl.wx.R
import com.dergoogler.mmrl.wx.datastore.providable.LocalUserPreferences
import com.dergoogler.mmrl.wx.model.module.Module
import com.dergoogler.mmrl.wx.model.module.ModuleUIState
import com.dergoogler.mmrl.wx.model.module.autoStatusBarsStyle
import com.dergoogler.mmrl.wx.model.module.backHandler
import com.dergoogler.mmrl.wx.model.module.backInterceptor
import com.dergoogler.mmrl.wx.model.module.caching
import com.dergoogler.mmrl.wx.model.module.cachingMaxAge
import com.dergoogler.mmrl.wx.model.module.contentSecurityPolicy
import com.dergoogler.mmrl.wx.model.module.exitConfirm
import com.dergoogler.mmrl.wx.model.module.historyFallback
import com.dergoogler.mmrl.wx.model.module.historyFallbackFile
import com.dergoogler.mmrl.wx.model.module.icon
import com.dergoogler.mmrl.wx.model.module.killShellWhenBackground
import com.dergoogler.mmrl.wx.model.module.pullToRefresh
import com.dergoogler.mmrl.wx.model.module.refreshInterceptor
import com.dergoogler.mmrl.wx.model.module.title
import com.dergoogler.mmrl.wx.model.module.windowResize
import com.dergoogler.mmrl.wx.ui.component.ErrorContent
import com.dergoogler.mmrl.wx.ui.component.LoadingContent
import com.dergoogler.mmrl.wx.ui.providable.LocalDestinationsNavigator
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph


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
        RadioOptionItem(
            value = "javascript-full",
            title = getString(R.string.controlled_by_javascript_full)
        ),
    )

@Destination<RootGraph>()
@Composable
fun ConfigEditorScreen(moduleId: String) {
    val state by Module.rememberCreate(moduleId)

    when (state) {
        ModuleUIState.Loading -> {
            ContentWrapper("Loading...") {
                LoadingContent()
            }
        }

        is ModuleUIState.Error -> {
            ContentWrapper("ERROR") {
                val msg = (state as ModuleUIState.Error).message
                ErrorContent(msg)
            }
        }

        is ModuleUIState.Ready -> {
            val module = (state as ModuleUIState.Ready).module
            ContentWrapper(module.name) {
                ConfigEditorContent(
                    modifier = Modifier.padding(it),
                    module = module,
                )
            }
        }
    }
}

@Composable
fun ConfigEditorContent(
    modifier: Modifier = Modifier,
    module: Module,
) {
    val userPrefs = LocalUserPreferences.current
    val context = LocalContext.current

    val config = remember { module.webrootConfig }

    Column(
        modifier = modifier
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
                config.set("title", it)
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
                config.set("icon", it)
            }
        )

//            ListButtonItem(
//                title = stringResource(R.string.plugins),
//                desc = stringResource(R.string.plugins_desc),
//                onClick = {
//                    navigator.navigate(PluginsScreenDestination(module))
//                }
//            )
//
//            if (config.additionalConfig.isNotNullOrEmpty()) {
//                ListButtonItem(
//                    title = stringResource(R.string.webui_additional_config),
//                    desc = stringResource(R.string.webui_additional_config_desc),
//                    onClick = {
//                        navigator.navigate(AdditionalConfigEditorScreenDestination(module))
//                    }
//                )
//            }

        val hasNoJsBackInterceptor = config.backInterceptor != "javascript"

        ListSwitchItem(
            enabled = hasNoJsBackInterceptor && !userPrefs.disableGlobalExitConfirm,
            title = stringResource(R.string.webui_config_exit_confirm_title),
            desc = stringResource(R.string.webui_config_exit_confirm_desc),
            checked = hasNoJsBackInterceptor && config.exitConfirm,
            onChange = { isChecked ->
                config.set("exitConfirm", isChecked)
            },
            base = {
                if (userPrefs.disableGlobalExitConfirm) {
                    labels = listOf { LabelItem(stringResource(R.string.globally_disabled)) }
                }
            }
        )

        val backHandler = config.backHandler ?: true

        ListSwitchItem(
            title = stringResource(R.string.webui_config_back_handler_title),
            desc = stringResource(R.string.webui_config_back_handler_desc),
            checked = backHandler,
            onChange = { isChecked ->
                config.set("backHandler", isChecked)
            },
        )

        ListRadioCheckItem(
            enabled = backHandler,
            title = stringResource(R.string.webui_config_back_interceptor_title),
            desc = stringResource(R.string.webui_config_back_interceptor_desc),
            value = config.backInterceptor,
            options = context.interceptorList,
            onConfirm = {
                if (it.value == null) {
                    Toast.makeText(context, "Please select an option", Toast.LENGTH_SHORT)
                        .show()
                    return@ListRadioCheckItem
                }

                config.set("backInterceptor", it.value)
            }
        )

        val pullToRefresh = config.pullToRefresh

        ListSwitchItem(
            title = stringResource(R.string.webui_config_pull_to_refresh_title),
            desc = stringResource(R.string.webui_config_pull_to_refresh_desc),
            checked = pullToRefresh,
            onChange = { isChecked ->
                config.set("pullToRefresh", isChecked)
            }
        )

//            val useNativeRefreshInterceptor = config.refreshInterceptor == "native"
//
//            ListSwitchItem(
//                enabled = pullToRefresh && useNativeRefreshInterceptor,
//                title = stringResource(R.string.webui_config_pull_to_refresh_helper_title),
//                desc = stringResource(R.string.webui_config_pull_to_refresh_helper_desc),
//                checked = useNativeRefreshInterceptor,
//                onChange = { isChecked ->
//                    saveconfig {
//                        "pullToRefreshHelper" change isChecked
//                    }
//                }
//            )

        ListRadioCheckItem(
            enabled = pullToRefresh,
            title = stringResource(R.string.webui_config_refresh_interceptor_title),
            desc = stringResource(R.string.webui_config_refresh_interceptor_desc),
            value = config.refreshInterceptor,
            options = context.interceptorList,
            onConfirm = { item ->
                if (item.value == null) {
                    Toast.makeText(context, "Please select an option", Toast.LENGTH_SHORT)
                        .show()
                    return@ListRadioCheckItem
                }

                config.set("refreshInterceptor", item.value)
            }
        )

        ListSwitchItem(
            title = stringResource(R.string.webui_config_window_resize_title),
            desc = stringResource(R.string.webui_config_window_resize_desc),
            checked = config.windowResize,
            onChange = { isChecked ->
                config.set("windowResize", isChecked)
            }
        )

        ListSwitchItem(
            title = stringResource(R.string.webui_config_auto_style_statusbars_title),
            desc = stringResource(R.string.webui_config_auto_style_statusbars_desc),
            checked = config.autoStatusBarsStyle,
            onChange = { isChecked ->
                config.set("autoStatusBarsStyle", isChecked)
            }
        )

        ListSwitchItem(
            title = stringResource(R.string.webui_config_kill_shell_when_background),
            desc = stringResource(R.string.webui_config_kill_shell_when_background_desc),
            checked = config.killShellWhenBackground,
            onChange = { isChecked ->
                config.set("killShellWhenBackground", isChecked)
            }
        )

        ListEditTextSwitchItem(
            title = stringResource(R.string.webui_config_history_fallback_title),
            desc = stringResource(R.string.webui_config_history_fallback_desc),
            value = config.historyFallbackFile,
            checked = config.historyFallback,
            onChange = { isChecked ->
                config.set("historyFallback", isChecked)
            },
            onConfirm = {
                config.set("historyFallbackFile", it)
            }
        )

        ListEditTextItem(
            title = stringResource(R.string.webui_config_content_security_policy_title),
            desc = stringResource(R.string.webui_config_content_security_policy_desc),
            value = config.contentSecurityPolicy,
            onConfirm = {
                config.set("contentSecurityPolicy", it)
            }
        )

        ListSwitchItem(
            title = stringResource(R.string.webui_config_caching_title),
            desc = stringResource(R.string.webui_config_caching_desc),
            checked = config.caching,
            onChange = { isChecked ->
                config.set("caching", isChecked)
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
                config.set("cachingMaxAge", it.toInt())
            }
        )
    }
}

@Composable
private fun ContentWrapper(
    subtitle: String,
    content: @Composable (PaddingValues) -> Unit,
) {
    val navigator = LocalDestinationsNavigator.current
    Scaffold(
        topBar = {
            NavigateUpTopBar(
                title = "Config",
                subtitle = subtitle,
                onBack = { navigator.popBackStack() },
            )
        },
        contentWindowInsets = WindowInsets.none,
        content = content
    )
}
