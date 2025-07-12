package com.dergoogler.mmrl.wx.util

import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.dergoogler.mmrl.wx.datastore.UserPreferencesRepository
import com.dergoogler.mmrl.wx.datastore.model.UserPreferences
import com.dergoogler.mmrl.wx.datastore.providable.LocalUserPreferences
import com.dergoogler.mmrl.platform.Platform.Companion.getPlatform
import com.dergoogler.mmrl.ui.providable.LocalNavController
import com.dergoogler.mmrl.ui.theme.MMRLAppTheme
import com.dergoogler.mmrl.wx.App.Companion.TAG
import com.dergoogler.mmrl.wx.service.PlatformService
import com.dergoogler.mmrl.wx.viewmodel.LocalSettings
import com.dergoogler.mmrl.wx.viewmodel.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
open class BaseActivity : ComponentActivity() {
    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
    }

    inline fun <reified A : ComponentActivity> setActivityEnabled(enable: Boolean) {
        val component = ComponentName(
            this, A::class.java
        )

        val state = if (enable) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        }

        packageManager.setComponentEnabledSetting(
            component,
            state,
            PackageManager.DONT_KILL_APP
        )
    }
}

fun BaseActivity.setBaseContent(
    parent: CompositionContext? = null,
    content: @Composable () -> Unit,
) = this.setContent(
    parent = parent,
) {
    val userPreferences by userPreferencesRepository.data.collectAsStateWithLifecycle(
        initialValue = null
    )

    val settings = hiltViewModel<SettingsViewModel>()

    val navController = rememberNavController()

    val preferences = if (userPreferences == null) {
        return@setContent
    } else {
        checkNotNull(userPreferences)
    }

    MMRLAppTheme(
        darkMode = preferences.isDarkMode(),
        navController = navController,
        themeColor = preferences.themeColor,
        providerValues = arrayOf(
            LocalUserPreferences provides preferences,
            LocalNavController provides navController,
            LocalSettings provides settings
        ),
        content = content
    )
}

fun ComponentActivity.initPlatform(userPreferences: UserPreferences) {
    val platform = intent.getPlatform() ?: userPreferences.workingMode.toPlatform()

    if (!PlatformService.isActive) {
        try {
            PlatformService.start(baseContext, platform)
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(TAG, "onCreate: $e")
        }
    }
}