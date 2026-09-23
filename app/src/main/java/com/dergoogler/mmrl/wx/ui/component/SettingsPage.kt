package com.dergoogler.mmrl.wx.ui.component

import androidx.compose.runtime.Composable
import com.dergoogler.mmrl.wx.datastore.model.UserPreferences
import com.dergoogler.mmrl.wx.datastore.providable.LocalUserPreferences
import com.dergoogler.mmrl.wx.viewmodel.LocalSettings

@Composable
fun SettingsPage(
    content: @Composable (UserPreferences, (UserPreferences.() -> UserPreferences) -> Unit) -> Unit,
) {
    val preferences = LocalUserPreferences.current
    val settingsViewModel = LocalSettings.current
    content(preferences, settingsViewModel::update)
}