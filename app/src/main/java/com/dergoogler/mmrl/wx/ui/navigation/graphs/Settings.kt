package com.dergoogler.mmrl.wx.ui.navigation.graphs

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.dergoogler.mmrl.wx.ui.navigation.MainRoute
import com.dergoogler.mmrl.wx.ui.screens.settings.DeveloperScreen
import com.dergoogler.mmrl.wx.ui.screens.settings.LicensesScreen
import com.dergoogler.mmrl.wx.ui.screens.settings.SettingsScreen
import com.dergoogler.mmrl.wx.ui.screens.settings.appTheme.AppThemeScreen
import kotlinx.serialization.Serializable

sealed interface SettingsRoute {
    @Serializable
    data object Home : SettingsRoute
    @Serializable
    data object AppTheme : SettingsRoute
    @Serializable
    data object Licenses : SettingsRoute
    @Serializable
    data object Developer : SettingsRoute
}

fun NavGraphBuilder.settingsRoute() = navigation<MainRoute.Settings>(
    startDestination = SettingsRoute.Home,
) {
    composable<SettingsRoute.Home>(
        enterTransition = { fadeIn() },
        exitTransition = { fadeOut() }
    ) {
        SettingsScreen()
    }

    composable<SettingsRoute.AppTheme>(
        enterTransition = { fadeIn() },
        exitTransition = { fadeOut() }
    ) {
        AppThemeScreen()
    }

    composable<SettingsRoute.Licenses>(
        enterTransition = { fadeIn() },
        exitTransition = { fadeOut() }
    ) {
        LicensesScreen()
    }

    composable<SettingsRoute.Developer>(
        enterTransition = { fadeIn() },
        exitTransition = { fadeOut() }
    ) {
        DeveloperScreen()
    }
}