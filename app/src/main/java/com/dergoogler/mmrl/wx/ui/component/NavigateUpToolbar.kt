package com.dergoogler.mmrl.wx.ui.component

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavController
import com.dergoogler.mmrl.ext.takeTrue
import com.dergoogler.mmrl.ui.R
import dev.mmrlx.compose.ui.icon.Icon
import dev.mmrlx.compose.ui.icon.IconButton
import dev.mmrlx.compose.ui.toolbar.Toolbar
import dev.mmrlx.compose.ui.toolbar.ToolbarColors
import dev.mmrlx.compose.ui.toolbar.ToolbarDefaults
import dev.mmrlx.compose.ui.toolbar.ToolbarScrollBehavior
import dev.mmrlx.compose.ui.toolbar.ToolbarTitle

@Composable
fun NavigateUpToolbar(
    title: String,
    navController: NavController,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    enable: Boolean = true,
    actions: @Composable RowScope.() -> Unit = {},
    windowInsets: WindowInsets = ToolbarDefaults.windowInsets,
    colors: ToolbarColors = ToolbarDefaults.colors(),
    scrollBehavior: ToolbarScrollBehavior? = null,
) = NavigateUpToolbar(
    modifier = modifier,
    title = title,
    subtitle = subtitle,
    onBack = { navController.popBackStack() },
    actions = actions,
    windowInsets = windowInsets,
    colors = colors,
    scrollBehavior = scrollBehavior,
    enable = enable,
)

@Composable
fun NavigateUpToolbar(
    title: String,
    modifier: Modifier = Modifier,
    context: Context = LocalContext.current,
    subtitle: String? = null,
    enable: Boolean = true,
    actions: @Composable RowScope.() -> Unit = {},
    windowInsets: WindowInsets = ToolbarDefaults.windowInsets,
    colors: ToolbarColors = ToolbarDefaults.colors(),
    scrollBehavior: ToolbarScrollBehavior? = null,
) = NavigateUpToolbar(
    modifier = modifier,
    title = title,
    subtitle = subtitle,
    onBack = { (context as ComponentActivity).finish() },
    actions = actions,
    windowInsets = windowInsets,
    colors = colors,
    scrollBehavior = scrollBehavior,
    enable = enable,
)

@Composable
fun NavigateUpToolbar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    enable: Boolean = true,
    actions: @Composable RowScope.() -> Unit = {},
    windowInsets: WindowInsets = ToolbarDefaults.windowInsets,
    colors: ToolbarColors = ToolbarDefaults.colors(),
    scrollBehavior: ToolbarScrollBehavior? = null,
) = NavigateUpToolbar(
    modifier = modifier,
    title = {
        ToolbarTitle(title = title, subtitle = subtitle)
    },
    onBack = onBack,
    actions = actions,
    windowInsets = windowInsets,
    colors = colors,
    scrollBehavior = scrollBehavior,
    enable = enable,
)

@Composable
fun NavigateUpToolbar(
    title: @Composable () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    enable: Boolean = true,
    actions: @Composable RowScope.() -> Unit = {},
    windowInsets: WindowInsets = ToolbarDefaults.windowInsets,
    colors: ToolbarColors = ToolbarDefaults.colors(),
    scrollBehavior: ToolbarScrollBehavior? = null,
) = Toolbar(
    title = title,
    modifier = modifier,
    navigationIcon = {
        enable.takeTrue {
            IconButton(
                onClick = { if (it) onBack() },
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.arrow_left),
                    contentDescription = null,
                )
            }
        }
    },
    actions = actions,
    windowInsets = windowInsets,
    colors = colors,
    scrollBehavior = scrollBehavior,
)
