@file:Suppress("UnusedReceiverParameter")

package com.dergoogler.mmrl.wx.util

import androidx.compose.ui.graphics.Color
import dev.mmrlx.compose.ui.theme.Colors
import dev.mmrlx.compose.ui.theme.Oklch

val Colors.badgeErrorForeground: Color
    get() = if (isDark) {
        Oklch(0.71, 0.19, 13.43)
    } else {
        Oklch(0.59, 0.25, 17.59)
    }.toColor()

val Colors.badgeErrorBackground: Color
    get() = Oklch(0.65, 0.25, 16.44).mixWithTransparent(0.15f).toColor()

val Colors.badgeWarnBackground: Color
    get() = Oklch(0.77, 0.19, 70.08).mixWithTransparent(0.15f).toColor()

val Colors.badgeInfoBackground: Color
    get() = Oklch(0.68, 0.17, 237.32).mixWithTransparent(0.15f).toColor()

val Colors.badgeDebugBackground: Color
    get() = Oklch(0.61, 0.25, 292.72).mixWithTransparent(0.15f).toColor()

val Colors.badgeWarnForeground: Color
    get() = if (isDark) {
        Oklch(0.83, 0.19, 84.43)
    } else {
        Oklch(0.67, 0.18, 58.32)
    }.toColor()

val Colors.badgeDebugForeground: Color
    get() = if (isDark) {
        Oklch(0.7, 0.18, 293.54)
    } else {
        Oklch(0.54, 0.28, 293.01)
    }.toColor()

val Colors.badgeInfoForeground: Color
    get() = if (isDark) {
        Oklch(0.75, 0.16, 232.65)
    } else {
        Oklch(0.59, 0.16, 241.96)
    }.toColor()
