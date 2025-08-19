package com.dergoogler.mmrl.wx.ui.screens

import android.util.Log
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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
    ) { paddingValues ->
        DestinationsNavHost(
            modifier = Modifier.padding(bottom = paddingValues.calculateBottomPadding()),
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

@Composable
private fun BottomNav() {
    val navController = LocalNavController.current
    val navigator = LocalDestinationsNavigator.current

    NavigationBar(
        modifier = Modifier
            .imePadding()
            .clip(
                RoundedCornerShape(
                    topStart = 20.dp,
                    topEnd = 20.dp
                )
            )
    ) {
        MainDestination.entries.forEach { screen ->
            val isSelected by navController.isRouteOnBackStackAsState(screen.direction)

            NavigationBarItem(
                icon = {
                    Icon(
                        painter = painterResource(
                            id = if (isSelected) {
                                screen.iconFilled
                            } else {
                                screen.icon
                            }
                        ),
                        contentDescription = null,
                    )
                },
                label = {
                    Text(
                        text = stringResource(screen.label),
                        style = MaterialTheme.typography.labelLarge
                    )
                },
                alwaysShowLabel = true,
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
                }
            )
        }
    }
}