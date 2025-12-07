package com.dergoogler.mmrl.wx.datastore

import androidx.datastore.core.DataStore
import com.dergoogler.mmrl.datastore.model.DarkMode
import com.dergoogler.mmrl.datastore.model.ModulesMenu
import com.dergoogler.mmrl.datastore.model.WorkingMode
import com.dergoogler.mmrl.wx.datastore.model.UserPreferences
import com.dergoogler.mmrl.wx.datastore.model.WebUIEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class UserPreferencesDataSource @Inject constructor(
    private val userPreferences: DataStore<UserPreferences>,
) {
    val data get() = userPreferences.data

    suspend fun setModulesMenu(value: ModulesMenu) = withContext(Dispatchers.IO) {
        userPreferences.updateData {
            it.copy(
                modulesMenu = value
            )
        }
    }

    suspend fun setWorkingMode(value: WorkingMode) = withContext(Dispatchers.IO) {
        userPreferences.updateData {
            it.copy(
                workingMode = value
            )
        }
    }

    suspend fun setDarkTheme(value: DarkMode) = withContext(Dispatchers.IO) {
        userPreferences.updateData {
            it.copy(
                darkMode = value
            )
        }
    }

    suspend fun setThemeColor(value: Int) = withContext(Dispatchers.IO) {
        userPreferences.updateData {
            it.copy(
                themeColor = value
            )
        }
    }

    suspend fun setDatePattern(value: String) = withContext(Dispatchers.IO) {
        userPreferences.updateData {
            it.copy(
                datePattern = value
            )
        }
    }

    suspend fun setWebUiDevUrl(value: String) = withContext(Dispatchers.IO) {
        userPreferences.updateData {
            it.copy(
                webUiDevUrl = value
            )
        }
    }

    suspend fun setDeveloperMode(value: Boolean) = withContext(Dispatchers.IO) {
        userPreferences.updateData {
            it.copy(
                developerMode = value
            )
        }
    }

    suspend fun setUseWebUiDevUrl(value: Boolean) = withContext(Dispatchers.IO) {
        userPreferences.updateData {
            it.copy(
                useWebUiDevUrl = value
            )
        }
    }

    suspend fun setEnableEruda(value: Boolean) = withContext(Dispatchers.IO) {
        userPreferences.updateData {
            it.copy(
                enableErudaConsole = value
            )
        }
    }

    suspend fun setEnableAutoOpenEruda(value: Boolean) = withContext(Dispatchers.IO) {
        userPreferences.updateData {
            it.copy(
                enableAutoOpenEruda = value
            )
        }
    }

    suspend fun setForceKillWebUIProcess(value: Boolean) = withContext(Dispatchers.IO) {
        userPreferences.updateData {
            it.copy(
                forceKillWebUIProcess = value
            )
        }
    }

    suspend fun setDisableGlobalExitConfirm(value: Boolean) = withContext(Dispatchers.IO) {
        userPreferences.updateData {
            it.copy(
                disableGlobalExitConfirm = value
            )
        }
    }

    suspend fun setWebUIEngine(value: WebUIEngine) = withContext(Dispatchers.IO) {
        userPreferences.updateData {
            it.copy(
                webuiEngine = value
            )
        }
    }
}