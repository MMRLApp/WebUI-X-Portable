package com.dergoogler.mmrl.wx.ui.component

import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.dergoogler.mmrl.ui.providable.LocalNavController
import com.dergoogler.mmrl.wx.R
import com.dergoogler.mmrl.wx.ui.navigation.MainDestination
import com.dergoogler.mmrl.wx.ui.providable.LocalDestinationsNavigator
import com.ramcosta.composedestinations.generated.NavGraphs
import com.ramcosta.composedestinations.utils.isRouteOnBackStackAsState
import dev.mmrlx.compose.ui.icon.Icon
import dev.mmrlx.compose.ui.navigationrail.NavigationRailItem
import dev.mmrlx.compose.ui.navigationrail.NavigationRail as Rail

@Composable
fun NavigationRail() {
    val navController = LocalNavController.current
    val navigator = LocalDestinationsNavigator.current

    Rail(
        header = {
            Icon(
                modifier = Modifier.size(30.dp),
                painter = painterResource(R.drawable.launcher_outline),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.surfaceTint,
            )
        }
    ) {
        MainDestination.entries.forEach { screen ->
            val isSelected by navController.isRouteOnBackStackAsState(screen.direction)

            NavigationRailItem(
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