package com.dergoogler.mmrl.wx.ui.screens.settings

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.dergoogler.mmrl.ext.none
import com.dergoogler.mmrl.ext.nullable
import com.dergoogler.mmrl.ext.toFormattedDateSafely
import com.dergoogler.mmrl.ui.component.listItem.dsl.List
import com.dergoogler.mmrl.ui.component.listItem.dsl.component.RadioDialogItem
import com.dergoogler.mmrl.ui.component.listItem.dsl.component.Section
import com.dergoogler.mmrl.ui.component.listItem.dsl.component.TextEditDialogItem
import com.dergoogler.mmrl.ui.component.listItem.dsl.component.item.Description
import com.dergoogler.mmrl.ui.component.listItem.dsl.component.item.DialogDescription
import com.dergoogler.mmrl.ui.component.listItem.dsl.component.item.Icon
import com.dergoogler.mmrl.ui.component.listItem.dsl.component.item.Title
import com.dergoogler.mmrl.ui.component.toolbar.Toolbar
import com.dergoogler.mmrl.ui.component.toolbar.ToolbarTitle
import com.dergoogler.mmrl.wx.R
import com.dergoogler.mmrl.wx.datastore.model.WebUIEngine
import com.dergoogler.mmrl.wx.datastore.providable.LocalUserPreferences
import com.dergoogler.mmrl.wx.model.FeaturedManager
import com.dergoogler.mmrl.wx.ui.component.LinkButton
import com.dergoogler.mmrl.wx.ui.component.NavButton
import com.dergoogler.mmrl.wx.viewmodel.LocalSettings
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.AppThemeScreenDestination
import com.ramcosta.composedestinations.generated.destinations.DeveloperScreenDestination
import com.ramcosta.composedestinations.generated.destinations.LicensesScreenDestination

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>()
@Composable
fun SettingsScreen() {
    val userPreferences = LocalUserPreferences.current
    val viewModel = LocalSettings.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            Toolbar(
                title = {
                    ToolbarTitle(title = stringResource(id = R.string.settings))
                },
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
                    route = AppThemeScreenDestination,
                    icon = R.drawable.color_swatch,
                    title = R.string.settings_app_theme,
                    desc = R.string.settings_app_theme_desc
                )

                val manager: FeaturedManager? =
                    FeaturedManager.managers.find { userPreferences.workingMode == it.workingMode }

                manager.nullable { mng ->
                    RadioDialogItem(
                        selection = mng.workingMode,
                        options = FeaturedManager.managers.map { it.toRadioDialogItem() },
                        onConfirm = {
                            viewModel.setWorkingMode(it.value)
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

                NavButton(
                    route = DeveloperScreenDestination,
                    icon = R.drawable.bug,
                    title = R.string.developer,
                )
            }

            Section(
                title = stringResource(com.dergoogler.mmrl.ui.R.string.learn_more),
                divider = false
            ) {
                LinkButton(
                    uri = "https://mmrl.dev/guide/webuix",
                    title = R.string.settings_documentation,
                    icon = R.drawable.api
                )

                NavButton(
                    route = LicensesScreenDestination,
                    icon = R.drawable.license,
                    title = R.string.setting_licenses,
                    desc = R.string.setting_licenses_desc
                )
            }
        }
    }
}