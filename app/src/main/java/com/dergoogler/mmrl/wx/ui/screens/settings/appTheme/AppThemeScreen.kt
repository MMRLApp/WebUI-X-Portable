package com.dergoogler.mmrl.wx.ui.screens.settings.appTheme

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import com.dergoogler.mmrl.wx.datastore.providable.LocalUserPreferences
import com.dergoogler.mmrl.ext.none
import com.dergoogler.mmrl.ui.component.NavigateUpTopBar
import com.dergoogler.mmrl.ui.providable.LocalNavController
import com.dergoogler.mmrl.wx.R
import com.dergoogler.mmrl.wx.ui.screens.settings.appTheme.items.DarkModeItem
import com.dergoogler.mmrl.wx.ui.screens.settings.appTheme.items.ExampleItem
import com.dergoogler.mmrl.wx.ui.screens.settings.appTheme.items.ThemePaletteItem
import com.dergoogler.mmrl.wx.ui.screens.settings.appTheme.items.TitleItem
import com.dergoogler.mmrl.wx.viewmodel.LocalSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppThemeScreen() {
    val userPreferences = LocalUserPreferences.current
    val viewModel = LocalSettings.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val navController = LocalNavController.current

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            NavigateUpTopBar(
                title = stringResource(R.string.settings_app_theme),
                scrollBehavior = scrollBehavior,
                navController = navController,
            )
        },
        contentWindowInsets = WindowInsets.none
    ) {
        Column(
            modifier = Modifier
                .padding(it)
                .verticalScroll(rememberScrollState()),
        ) {
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