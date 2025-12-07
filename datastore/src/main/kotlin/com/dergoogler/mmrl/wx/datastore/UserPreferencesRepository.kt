package com.dergoogler.mmrl.wx.datastore

import com.dergoogler.mmrl.datastore.model.DarkMode
import com.dergoogler.mmrl.datastore.model.ModulesMenu
import com.dergoogler.mmrl.datastore.model.WorkingMode
import com.dergoogler.mmrl.wx.datastore.model.WebUIEngine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferencesRepository @Inject constructor(
    private val userPreferencesDataSource: UserPreferencesDataSource,
) {
    val data get() = userPreferencesDataSource.data

    suspend fun setModulesMenu(value: ModulesMenu) =
        userPreferencesDataSource.setModulesMenu(value)

    suspend fun setWorkingMode(value: WorkingMode) = userPreferencesDataSource.setWorkingMode(value)

    suspend fun setDarkTheme(value: DarkMode) = userPreferencesDataSource.setDarkTheme(value)

    suspend fun setThemeColor(value: Int) = userPreferencesDataSource.setThemeColor(value)

    suspend fun setDatePattern(value: String) = userPreferencesDataSource.setDatePattern(value)

    suspend fun setWebUiDevUrl(value: String) =
        userPreferencesDataSource.setWebUiDevUrl(value)

    suspend fun setDeveloperMode(value: Boolean) =
        userPreferencesDataSource.setDeveloperMode(value)

    suspend fun setUseWebUiDevUrl(value: Boolean) =
        userPreferencesDataSource.setUseWebUiDevUrl(value)

    suspend fun setEnableEruda(value: Boolean) =
        userPreferencesDataSource.setEnableEruda(value)

    suspend fun setEnableAutoOpenEruda(value: Boolean) =
        userPreferencesDataSource.setEnableAutoOpenEruda(value)

    suspend fun setForceKillWebUIProcess(value: Boolean) =
        userPreferencesDataSource.setForceKillWebUIProcess(value)

    suspend fun setDisableGlobalExitConfirm(value: Boolean) =
        userPreferencesDataSource.setDisableGlobalExitConfirm(value)

    suspend fun setWebUIEngine(value: WebUIEngine) =
        userPreferencesDataSource.setWebUIEngine(value)
}