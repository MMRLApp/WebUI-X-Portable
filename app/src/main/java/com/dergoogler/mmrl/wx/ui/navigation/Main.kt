package com.dergoogler.mmrl.wx.ui.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.dergoogler.mmrl.wx.R
import kotlinx.serialization.Serializable

@Serializable
sealed class MainRoute(
    @StringRes val label: Int,
    @DrawableRes val icon: Int,
    @DrawableRes val iconFilled: Int,
) {
    @Serializable
    data object Modules : MainRoute(
        label = R.string.modules,
        icon = R.drawable.stack_2,
        iconFilled = R.drawable.stack_2_filled
    )

    @Serializable
    data object Settings : MainRoute(
        label = R.string.settings,
        icon = R.drawable.settings,
        iconFilled = R.drawable.settings_filled
    )
}
