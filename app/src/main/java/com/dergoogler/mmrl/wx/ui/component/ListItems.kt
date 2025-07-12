package com.dergoogler.mmrl.wx.ui.component

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.dergoogler.mmrl.wx.datastore.providable.LocalUserPreferences
import com.dergoogler.mmrl.ext.navigateSingleTopTo
import com.dergoogler.mmrl.ext.nullable
import com.dergoogler.mmrl.ui.component.listItem.dsl.ListItemScope
import com.dergoogler.mmrl.ui.component.listItem.dsl.ListItemSlot
import com.dergoogler.mmrl.ui.component.listItem.dsl.ListScope
import com.dergoogler.mmrl.ui.component.listItem.dsl.component.ButtonItem
import com.dergoogler.mmrl.ui.component.listItem.dsl.component.SwitchItem
import com.dergoogler.mmrl.ui.component.listItem.dsl.component.item.Description
import com.dergoogler.mmrl.ui.component.listItem.dsl.component.item.Icon
import com.dergoogler.mmrl.ui.component.listItem.dsl.component.item.Title
import com.dergoogler.mmrl.ui.providable.LocalNavController
import com.dergoogler.mmrl.wx.R

@Composable
internal fun ListScope.NavButton(
    route: String,
    @DrawableRes icon: Int? = null,
    @StringRes title: Int,
    @StringRes desc: Int? = null,
) {
    val navController = LocalNavController.current

    ButtonItem(
        onClick = {
            navController.navigateSingleTopTo(route)
        },
        content = {
            icon.nullable {
                Icon(
                    painter = painterResource(it)
                )
            }
            Title(title)
            desc.nullable {
                Description(it)
            }
        }
    )
}

@Composable
internal fun ListScope.LinkButton(
    uri: String,
    @DrawableRes icon: Int,
    @StringRes title: Int,
    @StringRes desc: Int? = null,
) {
    val browser = LocalUriHandler.current

    ButtonItem(
        onClick = {
            browser.openUri(uri)
        },
        content = {
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
    )
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