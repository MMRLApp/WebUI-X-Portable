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
import com.dergoogler.mmrl.wx.ui.component.NavigateUpToolbar
import com.dergoogler.mmrl.wx.ui.component.SettingsPage
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import dev.mmrlx.compose.ui.list.List
import dev.mmrlx.compose.ui.list.component.InputDialogItem
import dev.mmrlx.compose.ui.list.component.SwitchItem
import dev.mmrlx.compose.ui.list.component.item.Description
import dev.mmrlx.compose.ui.list.component.item.DialogDescription
import dev.mmrlx.compose.ui.list.component.item.Title
import dev.mmrlx.compose.ui.scaffold.Scaffold
import dev.mmrlx.compose.ui.toolbar.ToolbarDefaults

@Composable
@Destination<RootGraph>()
fun BehaviorScreen() = SettingsPage { prefs, update ->
    val navController = LocalNavController.current
    val scrollBehavior = ToolbarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        toolbar = {
            NavigateUpToolbar(
                title = stringResource(R.string.behavior),
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
                checked = prefs.disableGlobalExitConfirm,
                onChange = { update { copy(disableGlobalExitConfirm = it) } }

            ) {
                Title(R.string.settings_global_exit_confirm)
                Description(R.string.settings_global_exit_confirm_desc)
            }

            SwitchItem(
                checked = prefs.forceKillWebUIProcess,
                onChange = { update { copy(forceKillWebUIProcess = it) } }
            ) {
                Title(R.string.settings_force_kill_webui_process)
                Description(R.string.settings_force_kill_webui_process_desc)
            }

            InputDialogItem(
                value = prefs.adbPath,
                onConfirm = {
                    update { copy(adbPath = it.value) }
                },
            ) {
                Title(R.string.settings_custom_adb_path)
                Description(R.string.settings_custom_adb_path_desc)
                DialogDescription(R.string.settings_custom_adb_path_dialog_desc)
            }
        }
    }
}