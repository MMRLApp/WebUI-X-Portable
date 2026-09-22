package com.dergoogler.mmrl.wx.ui.providable

import androidx.compose.runtime.staticCompositionLocalOf
import com.dergoogler.mmrl.wx.viewmodel.ModulesViewModel

val LocalModulesViewModel = staticCompositionLocalOf<ModulesViewModel> {
    error("CompositionLocal ModulesViewModel not present")
}