package com.dergoogler.mmrl.wx.viewmodel

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dergoogler.mmrl.platform.PlatformManager
import com.dergoogler.mmrl.wx.datastore.UserPreferencesRepository
import com.dergoogler.mmrl.wx.datastore.model.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

val LocalSettings = staticCompositionLocalOf<SettingsViewModel> {
    error("CompositionLocal SettingsViewModel not present")
}

// Obsolete?
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {
    val version
        get() = PlatformManager.get("") {
            with(moduleManager) { "$version (${versionCode})" }
        }

    val versionCode
        get() = PlatformManager.get(-1) {
            with(moduleManager) { versionCode }
        }

    fun update(transform: UserPreferences.() -> UserPreferences) {
        viewModelScope.launch {
            userPreferencesRepository.update(transform)
        }
    }
}