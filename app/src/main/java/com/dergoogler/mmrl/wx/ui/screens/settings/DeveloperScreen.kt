package com.dergoogler.mmrl.wx.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import com.dergoogler.mmrl.ext.isLocalWifiUrl
import com.dergoogler.mmrl.ext.none
import com.dergoogler.mmrl.ui.providable.LocalNavController
import com.dergoogler.mmrl.wx.BuildConfig
import com.dergoogler.mmrl.wx.R
import com.dergoogler.mmrl.wx.datastore.providable.LocalUserPreferences
import com.dergoogler.mmrl.wx.ui.component.BottomNavigation
import com.dergoogler.mmrl.wx.ui.component.DeveloperSwitch
import com.dergoogler.mmrl.wx.ui.component.NavigateUpToolbar
import com.dergoogler.mmrl.wx.viewmodel.LocalSettings
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import dev.mmrlx.compose.ui.Badge
import dev.mmrlx.compose.ui.BadgeVariant
import dev.mmrlx.compose.ui.Text
import dev.mmrlx.compose.ui.button.Button
import dev.mmrlx.compose.ui.dialog.Content
import dev.mmrlx.compose.ui.dialog.Footer
import dev.mmrlx.compose.ui.dialog.Title
import dev.mmrlx.compose.ui.dialog.rememberDialog
import dev.mmrlx.compose.ui.list.List
import dev.mmrlx.compose.ui.list.component.InputDialogItem
import dev.mmrlx.compose.ui.list.component.Item
import dev.mmrlx.compose.ui.list.component.Section
import dev.mmrlx.compose.ui.list.component.SwitchItem
import dev.mmrlx.compose.ui.list.component.item.Description
import dev.mmrlx.compose.ui.list.component.item.DialogSupportingText
import dev.mmrlx.compose.ui.list.component.item.Supporting
import dev.mmrlx.compose.ui.list.component.item.Title
import dev.mmrlx.compose.ui.list.component.item.VerticalDividerSwitch
import dev.mmrlx.compose.ui.scaffold.Scaffold
import dev.mmrlx.compose.ui.text.FormatText
import dev.mmrlx.compose.ui.theme.MMRLXTheme
import dev.mmrlx.compose.ui.toolbar.ToolbarDefaults

@Destination<RootGraph>()
@Composable
fun DeveloperScreen() {
    val userPreferences = LocalUserPreferences.current
    val navController = LocalNavController.current
    val viewModel = LocalSettings.current
    val scrollBehavior = ToolbarDefaults.pinnedScrollBehavior()

    val remoteDomainDialog = rememberDialog()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        toolbar = {
            NavigateUpToolbar(
                title = stringResource(R.string.developer),
                scrollBehavior = scrollBehavior,
                navController = navController,
            )
        },
        bottomBar = {
            BottomNavigation()
        },
        contentWindowInsets = WindowInsets.none
    ) {
        List(
            modifier = Modifier
                .scaffoldHazeSource()
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .scaffoldPadding()
        ) {
            Section {
                SwitchItem(
                    checked = userPreferences.developerMode,
                    onChange = viewModel::setDeveloperMode
                ) {
                    Title(R.string.settings_developer_mode)
                    Description(R.string.settings_developer_mode_desc)
                }

                DeveloperSwitch(
                    enabled = !userPreferences.useWebUiDevUrl,
                    checked = userPreferences.enableErudaConsole && !userPreferences.useWebUiDevUrl,
                    onChange = viewModel::setEnableEruda
                ) {
                    // TODO: deprecate eruda
                    Title(R.string.settings_security_inject_eruda)
                    Description(R.string.settings_security_inject_eruda_desc)
                }

                DeveloperSwitch(
                    enabled = userPreferences.enableErudaConsole,
                    checked = userPreferences.enableErudaConsole && userPreferences.enableAutoOpenEruda,
                    onChange = viewModel::setEnableAutoOpenEruda
                ) {
                    // TODO: deprecate eruda
                    Title(R.string.settings_security_auto_open_eruda)
                    Description(R.string.settings_security_auto_open_eruda_desc)
                }

                DeveloperSwitch(
                    checked = userPreferences.enableDevTools,
                    onChange = viewModel::setEnableDevTools
                ) {
                    Title {
                        FormatText(stringResource(R.string.settings_security_enable_devtools) + " %y") {
                            composable {
                                Badge(
                                    text = stringResource(R.string.beta),
                                    variant = BadgeVariant.Secondary,
                                )
                            }
                        }
                    }
                    Description(R.string.settings_security_enable_devtools_desc)
                }

                DeveloperSwitch(
                    checked = userPreferences.disableConsoleInterceptor,
                    onChange = viewModel::setDisableConsoleInterceptor
                ) {
                    Title {
                        FormatText(stringResource(R.string.settings_disable_console_interceptor) + " %y") {
                            composable {
                                Badge(
                                    text = stringResource(R.string.beta),
                                    variant = BadgeVariant.Secondary,
                                )
                            }
                        }
                    }
                    Description(R.string.settings_disable_console_interceptor_desc)
                }

                InputDialogItem(
                    enabled = userPreferences.developerMode,
                    value = userPreferences.webUiDevUrl,
                    onConfirm = {
                        viewModel.setWebUiDevUrl(it.value)
                    },
                    onValid = { it.isLocalWifiUrl() },
                ) {
                    Title(R.string.settings_webui_remote_url)
                    Description(R.string.settings_webui_remote_url_desc)

                    VerticalDividerSwitch(
                        checked = userPreferences.useWebUiDevUrl,
                        onChange = viewModel::setUseWebUiDevUrl,
                        enabled = userPreferences.developerMode
                    )

                    Supporting {
                        Text(
                            modifier =
                                Modifier.clickable(
                                    onClick = {
                                        remoteDomainDialog.open()
                                    },
                                ),
                            text = stringResource(R.string.learn_more),
                        )

                    }

                    if (it.isError) {
                        DialogSupportingText {
                            Text(
                                text = stringResource(R.string.invalid_ip),
                                color = MMRLXTheme.colors.destructive,
                                style = MMRLXTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }

            Section(
                divider = false
            ) {
                Item {
                    Title(stringResource(R.string.latest_commit_id))
                    Description(BuildConfig.LATEST_COMMIT_ID)
                }
                Item {
                    Title(stringResource(R.string.build_tools_version))
                    Description(BuildConfig.BUILD_TOOLS_VERSION)
                }
                Item {
                    Title(stringResource(R.string.compile_sdk))
                    Description(BuildConfig.COMPILE_SDK)
                }
            }
        }
    }

    remoteDomainDialog {
        Title {
            Text(stringResource(R.string.settings_webui_remote_url))
        }

        Content {
            Text(stringResource(R.string.settings_webui_remote_url_alert_desc))
        }

        Footer {
            Button(
                onClick = {
                    remoteDomainDialog.close()
                },
            ) {
                Text(stringResource(android.R.string.ok))
            }
        }
    }
}