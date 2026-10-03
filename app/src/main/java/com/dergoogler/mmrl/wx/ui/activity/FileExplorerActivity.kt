package com.dergoogler.mmrl.wx.ui.activity

import android.os.Bundle
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.Crossfade
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavBackStackEntry
import com.dergoogler.mmrl.ui.providable.LocalNavController
import com.dergoogler.mmrl.wx.datastore.model.WorkingMode
import com.dergoogler.mmrl.wx.datastore.model.WorkingMode.Companion.isSetup
import com.dergoogler.mmrl.wx.ui.screens.fileexplorer.FileExplorerScreen
import com.dergoogler.mmrl.wx.util.BaseActivity
import com.dergoogler.mmrl.wx.util.setBaseContent
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.animations.NavHostAnimatedDestinationStyle
import com.ramcosta.composedestinations.generated.NavGraphs
import kotlinx.coroutines.launch

class FileExplorerActivity: BaseActivity() {
    private var isLoading by mutableStateOf(true)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        super.onCreate(savedInstanceState)

        splashScreen.setKeepOnScreenCondition { isLoading }

        setBaseContent {
            val userPreferences by userPreferencesRepository.data
                .collectAsStateWithLifecycle(initialValue = null)

            val preferences = if (userPreferences == null) {
                return@setBaseContent
            } else {
                isLoading = false
                checkNotNull(userPreferences)
            }

            Crossfade(
                targetState = preferences.workingMode.isSetup,
                label = "MainActivity"
            ) { isSetup ->
                if (isSetup) {
                    SetupScreen(::setWorkingMode)
                } else {
                    DestinationsNavHost(
                        navGraph = NavGraphs.fileExplorer,
                        navController = LocalNavController.current,
                        defaultTransitions = FileExplorerTransitions
                    )
                }
            }
        }
    }

    object FileExplorerTransitions : NavHostAnimatedDestinationStyle() {
        override val enterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition
            get() = { fadeIn(animationSpec = tween(340)) }

        override val exitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition
            get() = { fadeOut(animationSpec = tween(340)) }
    }


    private fun setWorkingMode(value: WorkingMode) {
        lifecycleScope.launch {
            userPreferencesRepository.setWorkingMode(value)
        }
    }
}