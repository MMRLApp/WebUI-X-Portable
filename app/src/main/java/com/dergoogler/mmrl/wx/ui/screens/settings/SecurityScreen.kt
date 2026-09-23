package com.dergoogler.mmrl.wx.ui.screens.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import com.dergoogler.mmrl.ui.providable.LocalNavController
import com.dergoogler.mmrl.wx.R
import com.dergoogler.mmrl.wx.datastore.providable.LocalUserPreferences
import com.dergoogler.mmrl.wx.ui.component.NavigateUpToolbar
import com.dergoogler.mmrl.wx.viewmodel.LocalSettings
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import dev.mmrlx.compose.ui.list.List
import dev.mmrlx.compose.ui.list.component.SwitchItem
import dev.mmrlx.compose.ui.list.component.item.Description
import dev.mmrlx.compose.ui.list.component.item.Title
import dev.mmrlx.compose.ui.scaffold.Scaffold
import dev.mmrlx.compose.ui.toolbar.ToolbarDefaults

@Composable
@Destination<RootGraph>()
fun SecurityScreen() {
    val userPreferences = LocalUserPreferences.current
    val navController = LocalNavController.current
    val viewModel = LocalSettings.current
    val scrollBehavior = ToolbarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        toolbar = {
            NavigateUpToolbar(
                title = stringResource(R.string.settings_security),
                scrollBehavior = scrollBehavior,
                navController = navController,
            )
        },
    ) {
        List(
            modifier = Modifier
                .scaffoldHazeSource()
                .scaffoldPadding()
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            SwitchItem(
                checked = ,
                onChange =
            ) {
                Title(R.string.settings_hide_fingerprint)
                Description(R.string.settings_hide_fingerprint_desc)
            }

            SwitchItem(
                checked = ,
                onChange =
            ) {
                Title(R.string.settings_hide_unix_name)
                Description(R.string.settings_hide_unix_name_desc)
            }
        }
    }
}