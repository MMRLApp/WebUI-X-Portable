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
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import com.dergoogler.mmrl.ext.none
import com.dergoogler.mmrl.ui.providable.LocalNavController
import com.dergoogler.mmrl.wx.datastore.providable.LocalUserPreferences
import com.dergoogler.mmrl.wx.ui.component.events.SnowfallOverlay
import com.dergoogler.mmrl.wx.ui.providable.LocalModulesViewModel
import com.dergoogler.mmrl.wx.viewmodel.ModulesViewModel
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.animations.NavHostAnimatedDestinationStyle
import com.ramcosta.composedestinations.generated.NavGraphs
import dev.mmrlx.compose.ui.scaffold.Scaffold
import java.time.LocalDate
import java.time.Month

@Composable
fun MainScreen() {
    val prefs = LocalUserPreferences.current
    val navController = LocalNavController.current
    val snackbarHostState = remember { SnackbarHostState() }

    val modulesViewModel: ModulesViewModel = hiltViewModel()

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

    if (!prefs.optOutAppEvents) {
        with(LocalDate.now()) {
//            if (month == Month.OCTOBER) {
//                BloodDripOverlay(
//                    streakCount = dayOfMonth * 4,
//                )
//
//                return@with
//            }

            if (month == Month.DECEMBER) {
                if (dayOfMonth in 24..26) {
                    SnowfallOverlay(
                        flakeCount = 200,
                    )

                    return@with
                }

                SnowfallOverlay(
                    flakeCount = dayOfMonth * 5,
                )

                return@with
            }

            if (month == Month.JANUARY && dayOfMonth <= 15) {
                val progress = (dayOfMonth - 1) / 14f
                val easedProgress = 1f - (1f - progress) * (1f - progress)
                val flakeCount = (155 * (1f - easedProgress)).toInt()

                if (flakeCount > 0) {
                    SnowfallOverlay(
                        flakeCount = flakeCount,
                    )

                    return@with
                }
            }
        }
    }
}