package com.dergoogler.mmrl.wx.ui.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.dergoogler.mmrl.wx.R
import com.ramcosta.composedestinations.generated.destinations.ModulesScreenDestination
import com.ramcosta.composedestinations.generated.destinations.SettingsScreenDestination
import com.ramcosta.composedestinations.spec.DirectionDestinationSpec

enum class MainDestination(
    val direction: DirectionDestinationSpec,
    @StringRes val label: Int,
    @DrawableRes val icon: Int,
    @DrawableRes val iconFilled: Int,
) {
    Modules(
        direction = ModulesScreenDestination,
        label = R.string.modules,
        icon = R.drawable.stack_2,
        iconFilled = R.drawable.stack_2_filled
    ),
    Settings(
        direction = SettingsScreenDestination,
        label = R.string.settings,
        icon = R.drawable.settings,
        iconFilled = R.drawable.settings_filled
    ),
}
