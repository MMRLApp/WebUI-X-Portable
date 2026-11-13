package com.dergoogler.mmrl.wx.ui.screens.settings.appTheme

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dergoogler.mmrl.wx.R
import com.dergoogler.mmrl.wx.ui.component.InfoAlert
import com.dergoogler.mmrl.wx.ui.component.SettingsPage
import com.dergoogler.mmrl.wx.ui.screens.settings.appTheme.items.DarkModeItem
import com.dergoogler.mmrl.wx.ui.screens.settings.appTheme.items.ExampleItem
import com.dergoogler.mmrl.wx.ui.screens.settings.appTheme.items.ThemePaletteItem
import com.dergoogler.mmrl.wx.ui.screens.settings.appTheme.items.TitleItem
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph

@Destination<RootGraph>()
@Composable
fun AppThemeScreen() = SettingsPage(R.string.settings_app_theme) { prefs, update ->
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
        themeColor = prefs.themeColor,
        isDarkMode = prefs.isDarkMode(),
        onChange = { update { copy(themeColor = it) } }
    )

    TitleItem(text = stringResource(id = R.string.app_theme_dark_theme))
    DarkModeItem(
        darkMode = prefs.darkMode,
        onChange = { update { copy(darkMode = it) } }
    )
}