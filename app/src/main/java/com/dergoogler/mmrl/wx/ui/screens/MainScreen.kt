package com.dergoogler.mmrl.wx.ui.screens

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import com.dergoogler.mmrl.ext.none
import com.dergoogler.mmrl.ui.providable.LocalNavController
import com.dergoogler.mmrl.wx.ui.providable.LocalModulesViewModel
import com.dergoogler.mmrl.wx.viewmodel.ModulesViewModel
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.animations.NavHostAnimatedDestinationStyle
import com.ramcosta.composedestinations.generated.NavGraphs
import dev.mmrlx.compose.nio.SuFileComposition
import dev.mmrlx.compose.ui.scaffold.Scaffold
import dev.mmrlx.nio.SuFile

@Composable
fun MainScreen() {
    val context = LocalContext.current
    val navController = LocalNavController.current
    val snackbarHostState = remember { SnackbarHostState() }

    val modulesViewModel: ModulesViewModel = hiltViewModel()

    LaunchedEffect(Unit) {
        SuFile.AutoInit(context)
    }

    SuFileComposition {
        CompositionLocalProvider(
            LocalModulesViewModel provides modulesViewModel
        ) {
            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                contentWindowInsets = WindowInsets.none
            ) {
                DestinationsNavHost(
                    navGraph = NavGraphs.root,
                    navController = navController,
                    defaultTransitions = object : NavHostAnimatedDestinationStyle() {
                        override val enterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition
                            get() = { fadeIn(animationSpec = tween(340)) }
                        override val exitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition
                            get() = { fadeOut(animationSpec = tween(340)) }
                    }
                )
            }
        }
    }
}