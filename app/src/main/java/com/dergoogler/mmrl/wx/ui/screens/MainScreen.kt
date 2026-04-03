package com.dergoogler.mmrl.wx.ui.screens

import android.util.Log
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.WindowInsets
/* remove */ import androidx.compose.material3.SnackbarHost
/* remove */import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavBackStackEntry
import com.dergoogler.mmrl.ext.none
import com.dergoogler.mmrl.ui.providable.LocalNavController
import com.dergoogler.mmrl.wx.App.Companion.TAG
import com.dergoogler.mmrl.wx.datastore.providable.LocalUserPreferences
import com.dergoogler.mmrl.wx.service.PlatformService
import com.dergoogler.mmrl.wx.ui.navigation.MainDestination
import com.dergoogler.mmrl.wx.ui.providable.LocalDestinationsNavigator
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.animations.NavHostAnimatedDestinationStyle
import com.ramcosta.composedestinations.generated.NavGraphs
import com.ramcosta.composedestinations.utils.isRouteOnBackStackAsState
import dev.mmrlx.compose.ui.Icon
import dev.mmrlx.compose.ui.bottomnavigation.BottomNavigation
import dev.mmrlx.compose.ui.bottomnavigation.TabButton
import dev.mmrlx.compose.ui.scaffold.ProvideLocalScaffoldScope
import dev.mmrlx.compose.ui.scaffold.Scaffold

@Composable
fun MainScreen() {
    val userPreferences = LocalUserPreferences.current
    val context = LocalContext.current

    val navController = LocalNavController.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        val platform = userPreferences.workingMode.toPlatform()

        if (!PlatformService.isActive) {
            try {
                PlatformService.start(context, platform)
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e(TAG, "onCreate: $e")
            }
        }
    }

    Scaffold(
        bottomBar = {
            BottomNav()
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets.none
    ) {
        ProvideLocalScaffoldScope(this) {
            DestinationsNavHost(
                modifier = Modifier.scaffoldHazeSource("mainScreen"),
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

@Composable
private fun BottomNav() {
    val navController = LocalNavController.current
    val navigator = LocalDestinationsNavigator.current

    BottomNavigation {
        MainDestination.entries.forEach { screen ->
            val isSelected by navController.isRouteOnBackStackAsState(screen.direction)

            TabButton(
                selected = isSelected,
                onClick = {
                    if (isSelected) {
                        navigator.popBackStack(screen.direction, false)
                    }

                    navigator.navigate(screen.direction) {
                        popUpTo(NavGraphs.root) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
            ) {
                Icon(
                    painter = painterResource(if (isSelected) screen.iconFilled else screen.icon),
                    contentDescription = null
                )
            }
        }
    }
}