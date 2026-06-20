package com.dergoogler.mmrl.wx.ui.screens.modules.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import com.dergoogler.mmrl.wx.R
import com.dergoogler.mmrl.wx.datastore.providable.LocalUserPreferences
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
import com.dergoogler.mmrl.wx.ui.component.LocalModule
import com.dergoogler.mmrl.wx.ui.component.ModuleScope
import com.dergoogler.mmrl.wx.ui.component.NavigateUpToolbar
import com.dergoogler.mmrl.wx.ui.providable.LocalDestinationsNavigator
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import dev.mmrlx.compose.ui.Badge
import dev.mmrlx.compose.ui.BadgeVariant
import dev.mmrlx.compose.ui.list.List
import dev.mmrlx.compose.ui.list.component.InputDialogItem
import dev.mmrlx.compose.ui.list.component.RadioDialogItem
import dev.mmrlx.compose.ui.list.component.RadioDialogOption
import dev.mmrlx.compose.ui.list.component.SwitchItem
import dev.mmrlx.compose.ui.list.component.item.Description
import dev.mmrlx.compose.ui.list.component.item.Supporting
import dev.mmrlx.compose.ui.list.component.item.Title
import dev.mmrlx.compose.ui.list.component.item.VerticalDividerSwitch
import dev.mmrlx.compose.ui.scaffold.Scaffold
import dev.mmrlx.compose.ui.toolbar.ToolbarTitle


private val Context.interceptorList: List<RadioDialogOption<String?>>
    get() = listOf(
        RadioDialogOption(
            value = "native",
            title = getString(R.string.controlled_by_native),
            desc = getString(R.string.controlled_by_native_desc)
        ),
        RadioDialogOption(
            value = "javascript",
            title = getString(R.string.controlled_by_javascript),
            desc = getString(R.string.controlled_by_javascript_desc)
        ),
        RadioDialogOption(
            value = "javascript-full",
            title = getString(R.string.controlled_by_javascript_full),
            desc = getString(R.string.controlled_by_javascript_full_desc)
        ),
    )

@Destination<RootGraph>()
@Composable
fun ConfigEditorScreen(moduleId: String) {
    ModuleScope(moduleId) {
        ConfigEditorContent()
    }
}

@Composable
fun ConfigEditorContent() {
    val module = LocalModule.current
    val userPrefs = LocalUserPreferences.current
    val context = LocalContext.current

    val navigator = LocalDestinationsNavigator.current
    val config = remember { module.webrootConfig }

    Scaffold(
        toolbar = {
            NavigateUpToolbar(
                title = {
                    ToolbarTitle(
                        title = "Config",
                        subtitle = module.name
                    )
                },
                onBack = { navigator.popBackStack() },
            )
        },
        contentWindowInsets = WindowInsets.systemBars,
    ) {
        List(
            modifier = Modifier
                .scaffoldHazeSource()
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .scaffoldPadding()
        ) {
            InputDialogItem(
                value = config.title ?: "",
                onConfirm = {
                    config.set("title", it)
                }
            ) {
                Title(R.string.webui_config_title_title)
                Description(config.title ?: stringResource(R.string.webui_config_title_desc)) {
                    if (config.title == null) {
                        it.copy(
                            fontStyle = FontStyle.Italic
                        )
                    } else it
                }
            }

            InputDialogItem(
                value = config.icon ?: "",
                onConfirm = {
                    config.set("icon", it)
                }
            ) {
                Title(R.string.webui_config_icon_title)
                Description(config.icon ?: stringResource(R.string.webui_config_icon_desc)) {
                    if (config.icon == null) {
                        it.copy(
                            fontStyle = FontStyle.Italic
                        )
                    } else it
                }
            }



            val hasNoJsBackInterceptor = !listOf("javascript", "javascript-full").contains(config.backInterceptor)

            SwitchItem(
                enabled = hasNoJsBackInterceptor && !userPrefs.disableGlobalExitConfirm,
                checked = hasNoJsBackInterceptor && config.exitConfirm,
                onChange = { isChecked ->
                    config.set("exitConfirm", isChecked)
                }
            ) {
                Title(R.string.webui_config_exit_confirm_title)
                Description(R.string.webui_config_exit_confirm_desc)

                Supporting {
                    if (userPrefs.disableGlobalExitConfirm) {
                        Badge(
                            text = stringResource(R.string.globally_disabled),
                            variant = BadgeVariant.Warning
                        )
                    }
                }
            }


            val backHandler = config.backHandler ?: true

            SwitchItem(
                checked = backHandler,
                onChange = { isChecked ->
                    config.set("backHandler", isChecked)
                }
            ) {
                Title(R.string.webui_config_back_handler_title)
                Description(R.string.webui_config_back_handler_desc)
            }

            RadioDialogItem(
                selection = config.backInterceptor,
                options = context.interceptorList,
                onConfirm = {
                    if (it.value == null) {
                        Toast.makeText(context, "Please select an option", Toast.LENGTH_SHORT)
                            .show()
                        return@RadioDialogItem
                    }

                    config.set("backInterceptor", it.value)
                }
            ) {
                Title(R.string.webui_config_back_interceptor_title)
                Description(R.string.webui_config_back_interceptor_desc)
            }

            val pullToRefresh = config.pullToRefresh

            SwitchItem(
                checked = pullToRefresh,
                onChange = { isChecked ->
                    config.set("pullToRefresh", isChecked)
                }
            ) {
                Title(R.string.webui_config_pull_to_refresh_title)
                Description(R.string.webui_config_pull_to_refresh_desc)
            }


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

            RadioDialogItem(
                selection = config.refreshInterceptor,
                options = context.interceptorList.filterIndexed { i, _ -> i != 2 },
                onConfirm = {
                    if (it.value == null) {
                        Toast.makeText(context, "Please select an option", Toast.LENGTH_SHORT)
                            .show()
                        return@RadioDialogItem
                    }

                    config.set("refreshInterceptor", it.value)
                }
            ) {
                Title(R.string.webui_config_refresh_interceptor_title)
                Description(R.string.webui_config_refresh_interceptor_desc)
            }

            SwitchItem(
                checked = config.windowResize,
                onChange = { isChecked ->
                    config.set("windowResize", isChecked)
                }
            ) {
                Title(R.string.webui_config_window_resize_title)
                Description(R.string.webui_config_window_resize_desc)
            }

            SwitchItem(
                checked = config.autoStatusBarsStyle,
                onChange = { isChecked ->
                    config.set("autoStatusBarsStyle", isChecked)
                }
            ) {
                Title(R.string.webui_config_auto_style_statusbars_title)
                Description(R.string.webui_config_auto_style_statusbars_desc)
            }

            SwitchItem(
                checked = config.killShellWhenBackground,
                onChange = { isChecked ->
                    config.set("killShellWhenBackground", isChecked)
                }
            ) {
                Title(R.string.webui_config_kill_shell_when_background)
                Description(R.string.webui_config_kill_shell_when_background_desc)
            }

            InputDialogItem(
                value = config.historyFallbackFile,
                onConfirm = {
                    config.set("historyFallbackFile", it.value)
                },
            ) {
                Title(R.string.webui_config_history_fallback_title)
                Description(R.string.webui_config_history_fallback_desc)

                VerticalDividerSwitch(
                    checked = config.historyFallback,
                    onChange = { isChecked ->
                        config.set("historyFallback", isChecked)
                    },
                )
            }

            InputDialogItem(
                value = config.contentSecurityPolicy,
                onConfirm = {
                    config.set("contentSecurityPolicy", it.value)
                },
            ) {
                Title(R.string.webui_config_content_security_policy_title)
                Description(R.string.webui_config_content_security_policy_desc)
            }

            SwitchItem(
                checked = config.caching,
                onChange = { isChecked ->
                    config.set("caching", isChecked)
                }
            ) {
                Title(R.string.webui_config_caching_title)
                Description(R.string.webui_config_caching_desc)
            }

            InputDialogItem(
                value = config.cachingMaxAge.toString(),
                onValid = {
                    !Regex("^[0-9]+$").matches(it)
                },
                onConfirm = {
                    config.set("cachingMaxAge", it.value.toInt())
                }
            ) {
                Title(R.string.webui_config_caching_max_age_title)
                Description(R.string.webui_config_caching_max_age_desc)
            }
        }
    }
}