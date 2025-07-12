package com.dergoogler.mmrl.wx.datastore.providable

import androidx.compose.runtime.staticCompositionLocalOf
import com.dergoogler.mmrl.wx.datastore.model.UserPreferences

val LocalUserPreferences = staticCompositionLocalOf { UserPreferences() }
