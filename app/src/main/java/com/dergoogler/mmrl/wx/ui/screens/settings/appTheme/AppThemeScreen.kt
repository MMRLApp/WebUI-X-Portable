package com.dergoogler.mmrl.wx.ui.screens.settings.appTheme

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dergoogler.mmrl.ui.providable.LocalNavController
import com.dergoogler.mmrl.wx.R
import com.dergoogler.mmrl.wx.datastore.providable.LocalUserPreferences
import com.dergoogler.mmrl.wx.ui.component.InfoAlert
import com.dergoogler.mmrl.wx.ui.component.NavigateUpToolbar
import com.dergoogler.mmrl.wx.ui.screens.settings.appTheme.items.DarkModeItem
import com.dergoogler.mmrl.wx.ui.screens.settings.appTheme.items.ExampleItem
import com.dergoogler.mmrl.wx.ui.screens.settings.appTheme.items.ThemePaletteItem
import com.dergoogler.mmrl.wx.ui.screens.settings.appTheme.items.TitleItem
import com.dergoogler.mmrl.wx.viewmodel.LocalSettings
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import dev.mmrlx.compose.ui.scaffold.Scaffold
import dev.mmrlx.compose.ui.toolbar.ToolbarDefaults

@Destination<RootGraph>()
@Composable
fun AppThemeScreen() {
    val userPreferences = LocalUserPreferences.current
    val viewModel = LocalSettings.current
    val scrollBehavior = ToolbarDefaults.pinnedScrollBehavior()
    val navController = LocalNavController.current

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        toolbar = {
            NavigateUpToolbar(
                title = stringResource(R.string.settings_app_theme),
                scrollBehavior = scrollBehavior,
                navController = navController,
            )
        },
        contentWindowInsets = WindowInsets.systemBars,
    ) {
        Column(
            modifier = Modifier
                .scaffoldHazeSource()
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .scaffoldPadding()
        ) {
            InfoAlert(
                modifier = Modifier.padding(16.dp),
                title = "Accent Theme",
                message = "This page is now only used to configure the application's accent colors and define the WebUI theme."
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ExampleItem()
            }

            TitleItem(text = stringResource(id = R.string.app_theme_palette))
            ThemePaletteItem(
                themeColor = userPreferences.themeColor,
                isDarkMode = userPreferences.isDarkMode(),
                onChange = viewModel::setThemeColor
            )

            TitleItem(text = stringResource(id = R.string.app_theme_dark_theme))
            DarkModeItem(
                darkMode = userPreferences.darkMode,
                onChange = viewModel::setDarkTheme
            )
        }
    }
}