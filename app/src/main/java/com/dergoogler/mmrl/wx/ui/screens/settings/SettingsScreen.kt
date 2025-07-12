package com.dergoogler.mmrl.wx.ui.screens.settings

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.dergoogler.mmrl.wx.datastore.model.WebUIEngine
import com.dergoogler.mmrl.wx.datastore.providable.LocalUserPreferences
import com.dergoogler.mmrl.ext.isLocalWifiUrl
import com.dergoogler.mmrl.ext.none
import com.dergoogler.mmrl.ext.nullable
import com.dergoogler.mmrl.ext.takeTrue
import com.dergoogler.mmrl.ext.toFormattedDateSafely
import com.dergoogler.mmrl.ui.component.TopAppBar
import com.dergoogler.mmrl.ui.component.toolbar.ToolbarTitle
import com.dergoogler.mmrl.ui.component.listItem.dsl.List
import com.dergoogler.mmrl.ui.component.listItem.dsl.component.RadioDialogItem
import com.dergoogler.mmrl.ui.component.listItem.dsl.component.Section
import com.dergoogler.mmrl.ui.component.listItem.dsl.component.SwitchItem
import com.dergoogler.mmrl.ui.component.listItem.dsl.component.TextEditDialogItem
import com.dergoogler.mmrl.ui.component.listItem.dsl.component.item.Description
import com.dergoogler.mmrl.ui.component.listItem.dsl.component.item.DialogDescription
import com.dergoogler.mmrl.ui.component.listItem.dsl.component.item.DialogSupportingText
import com.dergoogler.mmrl.ui.component.listItem.dsl.component.item.End
import com.dergoogler.mmrl.ui.component.listItem.dsl.component.item.Icon
import com.dergoogler.mmrl.ui.component.listItem.dsl.component.item.LearnMore
import com.dergoogler.mmrl.ui.component.listItem.dsl.component.item.Title
import com.dergoogler.mmrl.ui.providable.LocalNavController
import com.dergoogler.mmrl.wx.R
import com.dergoogler.mmrl.wx.model.FeaturedManager
import com.dergoogler.mmrl.wx.model.managers
import com.dergoogler.mmrl.wx.ui.component.DeveloperSwitch
import com.dergoogler.mmrl.wx.ui.component.NavButton
import com.dergoogler.mmrl.wx.ui.navigation.graphs.SettingsScreen
import com.dergoogler.mmrl.wx.util.toWorkingMode
import com.dergoogler.mmrl.wx.viewmodel.LocalSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val userPreferences = LocalUserPreferences.current
    val viewModel = LocalSettings.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    ToolbarTitle(title = stringResource(id = R.string.settings))
                },
                scrollBehavior = scrollBehavior
            )
        },
        contentWindowInsets = WindowInsets.none
    ) { innerPadding ->
        List(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            Section(
                title = stringResource(R.string.general)
            ) {
                NavButton(
                    route = SettingsScreen.AppTheme.route,
                    icon = R.drawable.color_swatch,
                    title = R.string.settings_app_theme,
                    desc = R.string.settings_app_theme_desc
                )

                val manager: FeaturedManager? =
                    managers.find { userPreferences.workingMode.toPlatform() == it.platform }

                manager.nullable { mng ->
                    RadioDialogItem(
                        selection = mng.platform,
                        options = managers.map { it.toRadioOption() },
                        onConfirm = {
                            viewModel.setWorkingMode(it.value.toWorkingMode())
                        },
                    ) {
                        Icon(
                            painter = painterResource(mng.icon)
                        )
                        Title(R.string.platform)
                        Description(mng.name)
                    }
                }

                RadioDialogItem(
                    selection = userPreferences.webuiEngine,
                    options = listOf(
                        RadioDialogItem(
                            value = WebUIEngine.WX,
                            title = stringResource(R.string.settings_webui_engine_wx)
                        ),
                        RadioDialogItem(
                            value = WebUIEngine.KSU,
                            title = stringResource(R.string.settings_webui_engine_ksu)
                        ),
                        RadioDialogItem(
                            value = WebUIEngine.PREFER_MODULE,
                            title = stringResource(R.string.settings_webui_engine_prefer_module)
                        )
                    ),
                    onConfirm = {
                        viewModel.setWebUIEngine(it.value)
                    }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.engine)
                    )
                    Title(R.string.settings_webui_engine)
                    Description(R.string.settings_webui_engine_desc)
                }

                TextEditDialogItem(
                    value = userPreferences.datePattern,
                    onConfirm = {
                        viewModel.setDatePattern(it)
                    },
                    onValid = {
                        System.currentTimeMillis()
                            .toFormattedDateSafely(it) == "Invalid date format pattern"
                    }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.calendar_cog)
                    )
                    Title(R.string.settings_date_pattern)
                    Description(R.string.settings_date_pattern_desc)

                    val date = System.currentTimeMillis().toFormattedDateSafely(it.value)
                    DialogDescription(R.string.settings_date_pattern_dialog_desc, date)
                }
            }

            Section(
                title = stringResource(R.string.developer),
                divider = false
            ) {
                SwitchItem(
                    checked = userPreferences.developerMode,
                    onChange = viewModel::setDeveloperMode
                ) {
                    Icon(
                        painter = painterResource(R.drawable.device_tablet_code)
                    )
                    Title(R.string.settings_developer_mode)
                    Description(R.string.settings_developer_mode_desc)
                }

                var webuiRemoteUrlInfo by remember { mutableStateOf(false) }
                if (webuiRemoteUrlInfo) AlertDialog(
                    title = {
                        Text(text = stringResource(id = R.string.settings_webui_remote_url))
                    },
                    text = {
                        Text(text = stringResource(id = R.string.settings_webui_remote_url_alert_desc))
                    },
                    onDismissRequest = {
                        webuiRemoteUrlInfo = false
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                webuiRemoteUrlInfo = false
                            }
                        ) {
                            Text(text = stringResource(id = android.R.string.ok))
                        }
                    },
                )

                DeveloperSwitch(
                    enabled = !userPreferences.useWebUiDevUrl,
                    checked = userPreferences.enableErudaConsole && !userPreferences.useWebUiDevUrl,
                    onChange = viewModel::setEnableEruda
                ) {
                    Icon(
                        painter = painterResource(R.drawable.square_chevrons_left)
                    )
                    Title(R.string.settings_security_inject_eruda)
                    Description(R.string.settings_security_inject_eruda_desc)
                }

                DeveloperSwitch(
                    enabled = userPreferences.enableErudaConsole,
                    checked = userPreferences.enableErudaConsole && userPreferences.enableAutoOpenEruda,
                    onChange = viewModel::setEnableAutoOpenEruda
                ) {
                    Icon(
                        painter = painterResource(R.drawable.tools)
                    )
                    Title(R.string.settings_security_auto_open_eruda)
                    Description(R.string.settings_security_auto_open_eruda_desc)
                }

                TextEditDialogItem(
                    enabled = userPreferences.developerMode,
                    value = userPreferences.webUiDevUrl,
                    onConfirm = {
                        viewModel.setWebUiDevUrl(it)
                    },
                    onValid = { !it.isLocalWifiUrl() },
                ) {
                    Icon(
                        painter = painterResource(R.drawable.forms)
                    )

                    Title(R.string.settings_webui_remote_url)
                    Description(R.string.settings_webui_remote_url_desc)

                    End {
                        val interactionSource = remember { MutableInteractionSource() }

                        Layout(
                            content = {
                                VerticalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    thickness = 1.dp
                                )

                                Switch(
                                    modifier = Modifier
                                        .toggleable(
                                            value = userPreferences.useWebUiDevUrl,
                                            onValueChange = viewModel::setUseWebUiDevUrl,
                                            enabled = userPreferences.developerMode,
                                            role = Role.Switch,
                                            interactionSource = interactionSource,
                                            indication = null
                                        ),
                                    checked = userPreferences.useWebUiDevUrl,
                                    onCheckedChange = null,
                                    interactionSource = interactionSource
                                )
                            }
                        ) { measurables, constraints ->
                            val dividerMeasurable = measurables[0]
                            val switchMeasurable = measurables[1]

                            // Measure switch first
                            val switchPlaceable = switchMeasurable.measure(constraints)

                            // Define divider height = switch height + padding
                            val dividerHeight = switchPlaceable.height + 36
                            val dividerPlaceable = dividerMeasurable.measure(
                                constraints.copy(
                                    minHeight = dividerHeight,
                                    maxHeight = dividerHeight
                                )
                            )

                            val width = dividerPlaceable.width + switchPlaceable.width
                            val height = maxOf(dividerPlaceable.height, switchPlaceable.height)

                            layout(width, height) {
                                // Center divider vertically relative to the full layout
                                val dividerY = (height - dividerPlaceable.height) / 2
                                val switchY = (height - switchPlaceable.height) / 2

                                dividerPlaceable.place(0, dividerY)
                                switchPlaceable.place(dividerPlaceable.width, switchY)
                            }
                        }
                    }

                    LearnMore {
                        webuiRemoteUrlInfo = true
                    }

                    it.isError.takeTrue {
                        DialogSupportingText {
                            Text(
                                text = stringResource(R.string.invalid_ip),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        }
    }
}