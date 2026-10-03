@file:Suppress("UnusedReceiverParameter")

package com.dergoogler.mmrl.wx.ui.component

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.dergoogler.mmrl.ext.nullable
import com.dergoogler.mmrl.wx.R
import com.dergoogler.mmrl.wx.datastore.providable.LocalUserPreferences
import com.dergoogler.mmrl.wx.ui.providable.LocalBrowser
import com.dergoogler.mmrl.wx.ui.providable.LocalDestinationsNavigator
import com.ramcosta.composedestinations.spec.Direction
import dev.mmrlx.compose.ui.LocalTextStyle
import dev.mmrlx.compose.ui.Skeleton
import dev.mmrlx.compose.ui.Text
import dev.mmrlx.compose.ui.icon.Icon
import dev.mmrlx.compose.ui.list.ListItemScope
import dev.mmrlx.compose.ui.list.ListItemSlot
import dev.mmrlx.compose.ui.list.ListItemSlotScope
import dev.mmrlx.compose.ui.list.ListScope
import dev.mmrlx.compose.ui.list.component.RawItem
import dev.mmrlx.compose.ui.list.component.SwitchItem
import dev.mmrlx.compose.ui.list.component.item.Description
import dev.mmrlx.compose.ui.list.component.item.Icon
import dev.mmrlx.compose.ui.list.component.item.Title
import dev.mmrlx.compose.ui.text.MutableFormatTextList
import dev.mmrlx.compose.ui.theme.LocalContentColor

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
    @DrawableRes icon: Int? = null,
    title: String,
    desc: String? = null,
) {
    val browser = LocalBrowser.current

    RawItem(
        modifier = Modifier
            .onClick {
                browser.open(uri)
            }
            .contentPadding()
    ) {
        icon.nullable {
            Icon(
                painter = painterResource(it)
            )
        }
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
internal fun ListScope.LinkButton(
    uri: String,
    @DrawableRes icon: Int? = null,
    @StringRes title: Int,
    @StringRes desc: Int? = null,
) = LinkButton(uri, icon, stringResource(title), desc?.let { stringResource(it) })

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

@Composable
internal fun ListItemScope.SensitiveDescription(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    styleTransform: @Composable ((TextStyle) -> TextStyle)? = null,
    content: @Composable (ListItemSlotScope.() -> Unit),
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    var isLongPressed by remember { mutableStateOf(false) }
    if (!isPressed) {
        isLongPressed = false
    }

    Description(
        modifier = Modifier
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {},
                onLongClick = { isLongPressed = true }
            )
            .animateContentSize()
            .then(modifier),
        styleTransform = styleTransform
    ) {
        val style = LocalTextStyle.current

        if (isLongPressed) {
            content()
            return@Description
        }

        if (enabled) {
            Skeleton(
                animate = false,
                modifier = Modifier
                    .fillMaxWidth(0.2851f)
                    .height(
                        style.fontSize.value.dp
                    )
            )

            return@Description
        }

        content()
    }
}

@Composable
internal fun ListItemScope.SensitiveDescription(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    styleTransform: @Composable ((TextStyle) -> TextStyle)? = null,
) = SensitiveDescription(modifier, enabled, styleTransform) {
    Text(text)
}

fun MutableFormatTextList.linkString(url: String) {
    val reg = "^https?://".toRegex(RegexOption.MULTILINE)
    linkString(url.replace(reg, ""), url)
}

/**
 * Placed with `%y` inside a string resource.
 */
fun MutableFormatTextList.linkString(text: String, uri: String) = composable {
    val browser = LocalBrowser.current
    val color = LocalContentColor.current

    Row(
        modifier = Modifier.clickable {
            browser.open(uri)
        },
        horizontalArrangement = Arrangement.spacedBy(1.5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            textDecoration = TextDecoration.Underline,
            color = color
        )

        Icon(
            modifier = Modifier.size(fontSize.dp),
            contentDescription = null,
            tint = color,
            painter = painterResource(R.drawable.external_link)
        )
    }
}
