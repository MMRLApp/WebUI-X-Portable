package com.dergoogler.mmrl.wx.viewmodel

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.ViewModel
import com.dergoogler.mmrl.wx.datastore.UserPreferencesRepository
import androidx.lifecycle.viewModelScope
import com.dergoogler.mmrl.datastore.model.DarkMode
import com.dergoogler.mmrl.datastore.model.Homepage
import com.dergoogler.mmrl.wx.datastore.model.WebUIEngine
import com.dergoogler.mmrl.datastore.model.WorkingMode
import com.dergoogler.mmrl.platform.PlatformManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

val LocalSettings = staticCompositionLocalOf<SettingsViewModel> {
    error("CompositionLocal SettingsViewModel not present")
}

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

    fun setWorkingMode(value: WorkingMode) {
        viewModelScope.launch {
            userPreferencesRepository.setWorkingMode(value)
        }
    }

    fun setDarkTheme(value: DarkMode) {
        viewModelScope.launch {
            userPreferencesRepository.setDarkTheme(value)
        }
    }

    fun setThemeColor(value: Int) {
        viewModelScope.launch {
            userPreferencesRepository.setThemeColor(value)
        }
    }

    fun setDatePattern(value: String) {
        viewModelScope.launch {
            userPreferencesRepository.setDatePattern(value)
        }
    }

    fun setWebUiDevUrl(value: String) {
        viewModelScope.launch {
            userPreferencesRepository.setWebUiDevUrl(value)
        }
    }

    fun setDeveloperMode(value: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setDeveloperMode(value)
        }
    }

    fun setUseWebUiDevUrl(value: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setUseWebUiDevUrl(value)
        }
    }

    fun setEnableEruda(value: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setEnableEruda(value)
        }
    }

    fun setEnableAutoOpenEruda(value: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setEnableAutoOpenEruda(value)
        }
    }

    fun setWebUIEngine(value: WebUIEngine) {
        viewModelScope.launch {
            userPreferencesRepository.setWebUIEngine(value)
        }
    }
}