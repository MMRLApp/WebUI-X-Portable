package com.dergoogler.mmrl.wx.ui.screens.settings

import androidx.compose.runtime.Composable
import com.dergoogler.mmrl.wx.R
import com.dergoogler.mmrl.wx.ui.component.SettingsPage
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import dev.mmrlx.compose.ui.list.component.SwitchItem
import dev.mmrlx.compose.ui.list.component.item.Description
import dev.mmrlx.compose.ui.list.component.item.Title

@Composable
@Destination<RootGraph>()
fun SecurityScreen() = SettingsPage(R.string.settings_security) { prefs, update ->
    SwitchItem(
        checked = prefs.hideFingerprintInHome,
        onChange = { update { copy(hideFingerprintInHome = it) } }
    ) {
        Title(R.string.settings_hide_fingerprint)
        Description(R.string.settings_hide_fingerprint_desc)
    }

    SwitchItem(
        checked = prefs.hideUnixNameInHome,
        onChange = { update { copy(hideUnixNameInHome = it) } }
    ) {
        Title(R.string.settings_hide_unix_name)
        Description(R.string.settings_hide_unix_name_desc)
    }
}
