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
import com.dergoogler.mmrl.wx.ui.component.BottomNavigation
import com.dergoogler.mmrl.wx.ui.component.DeveloperSwitch
import com.dergoogler.mmrl.wx.ui.component.LinkButton
import com.dergoogler.mmrl.wx.ui.component.NavigateUpToolbar
import com.dergoogler.mmrl.wx.ui.component.SettingsPage
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
fun DeveloperScreen() = SettingsPage { prefs, update ->
    val navController = LocalNavController.current
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
                    checked = prefs.developerMode,
                    onChange = { update { copy(developerMode = it) } }
                ) {
                    Title(R.string.settings_developer_mode)
                    Description(R.string.settings_developer_mode_desc)
                }

                DeveloperSwitch(
                    checked = prefs.enableDevTools,
                    onChange = { update { copy(enableDevTools = it) } }
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

                InputDialogItem(
                    enabled = prefs.developerMode,
                    value = prefs.webUiDevUrl,
                    onConfirm = {
                        update { copy(webUiDevUrl = it.value) }
                    },
                    onValid = { it.isLocalWifiUrl() },
                ) {
                    Title(R.string.settings_webui_remote_url)
                    Description(R.string.settings_webui_remote_url_desc)

                    VerticalDividerSwitch(
                        checked = prefs.useWebUiDevUrl,
                        onChange = { update { copy(useWebUiDevUrl = it) } },
                        enabled = prefs.developerMode
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
                val commitId = BuildConfig.LATEST_COMMIT_ID
                LinkButton(
                    uri = "${BuildConfig.ORIGIN}/commit/$commitId",
                    title = stringResource(R.string.commit_id_and_branch),
                    desc = "$commitId, ${BuildConfig.LATEST_BRANCH}"
                )
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