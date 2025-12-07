package com.dergoogler.mmrl.wx.ui.screens.settings

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import com.dergoogler.mmrl.ext.none
import com.dergoogler.mmrl.ui.component.NavigateUpTopBar
import com.dergoogler.mmrl.ui.component.listItem.dsl.List
import com.dergoogler.mmrl.ui.component.listItem.dsl.component.SwitchItem
import com.dergoogler.mmrl.ui.component.listItem.dsl.component.item.Description
import com.dergoogler.mmrl.ui.component.listItem.dsl.component.item.Title
import com.dergoogler.mmrl.ui.component.scaffold.Scaffold
import com.dergoogler.mmrl.ui.providable.LocalNavController
import com.dergoogler.mmrl.wx.R
import com.dergoogler.mmrl.wx.datastore.providable.LocalUserPreferences
import com.dergoogler.mmrl.wx.viewmodel.LocalSettings
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph

@Composable
@Destination<RootGraph>()
fun BehaviorScreen() {
    val userPreferences = LocalUserPreferences.current
    val navController = LocalNavController.current
    val viewModel = LocalSettings.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            NavigateUpTopBar(
                title = stringResource(R.string.behavior),
                scrollBehavior = scrollBehavior,
                navController = navController,
            )
        },
        contentWindowInsets = WindowInsets.none
    ) { innerPadding ->
        List(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            SwitchItem(
                checked = userPreferences.disableGlobalExitConfirm,
                onChange = viewModel::setDisableGlobalExitConfirm
            ) {
                Title(R.string.settings_global_exit_confirm)
                Description(R.string.settings_global_exit_confirm_desc)
            }

        }
    }
}