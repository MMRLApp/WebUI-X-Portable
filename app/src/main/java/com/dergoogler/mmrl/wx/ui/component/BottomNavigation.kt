package com.dergoogler.mmrl.wx.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.painterResource
import com.dergoogler.mmrl.ui.providable.LocalNavController
import com.dergoogler.mmrl.wx.ui.navigation.MainDestination
import com.dergoogler.mmrl.wx.ui.providable.LocalDestinationsNavigator
import com.ramcosta.composedestinations.utils.isRouteOnBackStackAsState
import com.ramcosta.composedestinations.generated.NavGraphs
import dev.mmrlx.compose.ui.Icon
import dev.mmrlx.compose.ui.navigationbar.NavigationBar
import dev.mmrlx.compose.ui.navigationbar.NavigationBarItem

@Composable
fun BottomNavigation() {
    val navController = LocalNavController.current
    val navigator = LocalDestinationsNavigator.current

    NavigationBar() {
        MainDestination.entries.forEach { screen ->
            val isSelected by navController.isRouteOnBackStackAsState(screen.direction)

            NavigationBarItem(
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
                icon = {
                    Icon(
                        painter = painterResource(if (isSelected) screen.iconFilled else screen.icon),
                        contentDescription = null
                    )
                }
            )
        }
    }
}