package com.dergoogler.mmrl.wx.ui.component

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import com.dergoogler.mmrl.ui.providable.LocalNavController
import com.dergoogler.mmrl.wx.datastore.model.UserPreferences
import com.dergoogler.mmrl.wx.datastore.providable.LocalUserPreferences
import com.dergoogler.mmrl.wx.viewmodel.LocalSettings
import dev.mmrlx.compose.ui.list.List
import dev.mmrlx.compose.ui.list.ListScope
import dev.mmrlx.compose.ui.scaffold.LocalScaffoldScope
import dev.mmrlx.compose.ui.scaffold.Scaffold
import dev.mmrlx.compose.ui.scaffold.ScaffoldScope
import dev.mmrlx.compose.ui.toolbar.ProvideScrollBehavior
import dev.mmrlx.compose.ui.toolbar.Toolbar
import dev.mmrlx.compose.ui.toolbar.ToolbarDefaults
import dev.mmrlx.compose.ui.toolbar.ToolbarTitle


@Composable
fun SettingsPage(
    @StringRes resId: Int,
    isStartPage: Boolean = false,
    scrollable: Boolean = true,
    content: @Composable SettingsPageScope.(UserPreferences, (UserPreferences.() -> UserPreferences) -> Unit) -> Unit,
) = SettingsPage(
    title = stringResource(resId),
    isStartPage = isStartPage,
    scrollable = scrollable,
    content = content,
)

interface SettingsPageScope : ScaffoldScope, ListScope

class SettingsPageScopeImpl(
    val scaffoldScope: ScaffoldScope,
    val listScope: ListScope,
) : SettingsPageScope, ScaffoldScope by scaffoldScope, ListScope by listScope

@Composable
fun SettingsPage(
    title: String,
    isStartPage: Boolean = false,
    scrollable: Boolean = true,
    content: @Composable SettingsPageScope.(UserPreferences, (UserPreferences.() -> UserPreferences) -> Unit) -> Unit,
) {
    val navController = LocalNavController.current
    val scrollBehavior = ToolbarDefaults.fadePinnedScrollBehavior()
    val preferences = LocalUserPreferences.current
    val settingsViewModel = LocalSettings.current

    val scrollModifier =
        if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier

    ProvideScrollBehavior(scrollBehavior) {
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            toolbar = {
                if (isStartPage) {
                    Toolbar(
                        title = {
                            ToolbarTitle(title = title)
                        },
                        scrollBehavior = scrollBehavior,
                    )
                    return@Scaffold
                }

                NavigateUpToolbar(
                    title = title,
                    scrollBehavior = scrollBehavior,
                    navController = navController,
                )
            },

            bottomBar = {
                val scope = LocalScaffoldScope.current
                if (!isStartPage && scope.hasRail) return@Scaffold
                BottomNavigation()
            },
            rail = {
                NavigationRail(

                )
            }
        ) {
            ResponsiveLayout {
                List(
                    modifier = Modifier
                        .then(scrollModifier)
                        .scaffoldPadding()
                        .fillMaxWidth()
                        .scaffoldHazeSource()
                ) {
                    val scope = SettingsPageScopeImpl(this@Scaffold, this)
                    scope.content(preferences, settingsViewModel::update)
                }
            }
        }
    }
}
