package com.dergoogler.mmrl.wx.ui.screens

import android.util.Log
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import com.dergoogler.mmrl.datastore.model.WorkingMode.Companion.isRoot
import com.dergoogler.mmrl.wx.datastore.providable.LocalUserPreferences
import com.dergoogler.mmrl.ext.navigatePopUpTo
import com.dergoogler.mmrl.ext.none
import com.dergoogler.mmrl.platform.PlatformManager
import com.dergoogler.mmrl.ui.providable.LocalNavController
import com.dergoogler.mmrl.wx.App.Companion.TAG
import com.dergoogler.mmrl.wx.service.PlatformService
import com.dergoogler.mmrl.wx.ui.navigation.MainScreen
import com.dergoogler.mmrl.wx.ui.navigation.graphs.modulesScreen
import com.dergoogler.mmrl.wx.ui.navigation.graphs.settingsScreen

@Composable
fun MainScreen() {
    val userPreferences = LocalUserPreferences.current
    val context = LocalContext.current

    val navController = LocalNavController.current
    val snackbarHostState = remember { SnackbarHostState() }

    val isRoot = userPreferences.workingMode.isRoot && PlatformManager.isAlive

    val mainScreens by remember(isRoot) {
        derivedStateOf {
            return@derivedStateOf listOf(
                MainScreen.Modules,
                MainScreen.Settings
            )
        }
    }

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
            BottomNav(mainScreens)
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets.none
    ) { paddingValues ->
        NavHost(
            modifier = Modifier.padding(bottom = paddingValues.calculateBottomPadding()),
            navController = navController,
            startDestination = MainScreen.Modules.route
        ) {
            modulesScreen()
            settingsScreen()
        }
    }
}

@Composable
private fun BottomNav(
    mainScreens: List<MainScreen>,
) {
    val navController = LocalNavController.current
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

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
        mainScreens.forEach { screen ->
            val selected =
                currentDestination?.hierarchy?.any { it.route == screen.route } == true

            NavigationBarItem(
                icon = {
                    Icon(
                        painter = painterResource(
                            id = if (selected) {
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
                selected = selected,
                onClick = {
                    if (selected) return@NavigationBarItem

                    navController.navigatePopUpTo(
                        route = screen.route,
                    )
                }
            )
        }
    }
}