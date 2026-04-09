package com.dergoogler.mmrl.wx.ui.component

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.dergoogler.mmrl.ext.nullable
import com.dergoogler.mmrl.wx.R
import com.dergoogler.mmrl.wx.datastore.providable.LocalUserPreferences
import com.dergoogler.mmrl.wx.ui.providable.LocalDestinationsNavigator
import com.ramcosta.composedestinations.spec.Direction
import dev.mmrlx.compose.ui.list.ListItemScope
import dev.mmrlx.compose.ui.list.ListItemSlot
import dev.mmrlx.compose.ui.list.ListScope
import dev.mmrlx.compose.ui.list.component.RawItem
import dev.mmrlx.compose.ui.list.component.SwitchItem
import dev.mmrlx.compose.ui.list.component.item.Description
import dev.mmrlx.compose.ui.list.component.item.Icon
import dev.mmrlx.compose.ui.list.component.item.Title

@Composable
internal fun <T : Direction> ListScope.NavButton(
    route: T,
    @DrawableRes icon: Int? = null,
    @StringRes title: Int,
    @StringRes desc: Int? = null,
) {
    val navigator = LocalDestinationsNavigator.current

    RawItem(
        modifier = Modifier
            .onClick {
                navigator.navigate(route)
            }
            .contentPadding()
    ) {
        icon?.let {
            Icon(
                painter = painterResource(it)
            )
        }
        Title(title)
        desc?.let {
            Description(it)
        }
    }
}

@Composable
internal fun ListScope.LinkButton(
    uri: String,
    @DrawableRes icon: Int,
    @StringRes title: Int,
    @StringRes desc: Int? = null,
) {
    val browser = LocalUriHandler.current

    RawItem(
        modifier = Modifier
            .contentPadding()
            .onClick {
                browser.openUri(uri)
            }
    ) {
        Icon(
            painter = painterResource(icon)
        )
        Title(title)
        desc.nullable {
            Description(it)
        }
        Icon(
            slot = ListItemSlot.End,
            size = 12.dp,
            painter = painterResource(R.drawable.external_link)
        )
    }
}

@Composable
internal fun ListScope.DeveloperSwitch(
    enabled: Boolean = true,
    onChange: (Boolean) -> Unit,
    checked: Boolean,
    content: @Composable ListItemScope.() -> Unit,
) {
    val userPrefs = LocalUserPreferences.current

    SwitchItem(
        checked = userPrefs.developerMode && checked,
        onChange = onChange,
        enabled = userPrefs.developerMode && enabled,
        content = content
    )
}