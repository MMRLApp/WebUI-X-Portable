package com.dergoogler.mmrl.wx.ui.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import dev.mmrlx.compose.layout.outlinedCard
import dev.mmrlx.compose.ui.Text
import dev.mmrlx.compose.ui.icon.Icon
import dev.mmrlx.compose.ui.theme.MMRLXTheme

private val LocalPopupMenuDismiss = staticCompositionLocalOf<() -> Unit> { {} }

/** Holds the open state of a [PopupMenu]. */
@Stable
class PopupMenuState internal constructor(initial: Boolean) {
    var expanded by mutableStateOf(initial)
        private set

    fun open() {
        expanded = true
    }

    fun close() {
        expanded = false
    }

    fun toggle() {
        expanded = !expanded
    }
}

@Composable
fun rememberPopupMenuState(initial: Boolean = false): PopupMenuState =
    remember { PopupMenuState(initial) }

/**
 * A small popup menu anchored below (or above, if there is no room) its parent, aligned to
 * the parent's end edge. Place it inside the composable that acts as its anchor,
 * e.g. an `IconButton`, and open it using [PopupMenuState.open].
 *
 * Items should be [PopupMenuItem]s; they dismiss the menu automatically when clicked.
 */
@Composable
fun PopupMenu(
    state: PopupMenuState,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset(0.dp, 4.dp),
    minWidth: Dp = 120.dp,
    maxWidth: Dp = 160.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (!state.expanded) return

    val density = LocalDensity.current
    val offsetPx = with(density) { IntOffset(offset.x.roundToPx(), offset.y.roundToPx()) }
    val positionProvider = remember(offsetPx) { PopupMenuPositionProvider(offsetPx) }

    Popup(
        popupPositionProvider = positionProvider,
        onDismissRequest = state::close,
        properties = PopupProperties(
            focusable = true,
            dismissOnClickOutside = true,
            dismissOnBackPress = true,
        ),
    ) {
        CompositionLocalProvider(LocalPopupMenuDismiss provides state::close) {
            Column(
                modifier = modifier
                    .outlinedCard()
                    .widthIn(min = minWidth, max = maxWidth)
                    .padding(6.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                content = content,
            )
        }
    }
}

@Composable
fun PopupMenuItem(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    @DrawableRes icon: Int? = null,
) {
    val dismiss = LocalPopupMenuDismiss.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MMRLXTheme.shapes.medium)
            .clickable(enabled = enabled) {
                dismiss()
                onClick()
            }
            .alpha(if (enabled) 1f else 0.38f)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (icon != null) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
        }

        Text(
            text = text,
            style = MMRLXTheme.typography.labelLarge,
        )
    }
}

private class PopupMenuPositionProvider(
    private val offset: IntOffset,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val x = (anchorBounds.right - popupContentSize.width - offset.x)
            .coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0))

        val below = anchorBounds.bottom + offset.y
        val y = if (below + popupContentSize.height <= windowSize.height) {
            below
        } else {
            (anchorBounds.top - popupContentSize.height - offset.y).coerceAtLeast(0)
        }

        return IntOffset(x, y)
    }
}
