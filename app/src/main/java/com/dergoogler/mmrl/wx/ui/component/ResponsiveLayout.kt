package com.dergoogler.mmrl.wx.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.dp
import dev.mmrlx.compose.ui.scaffold.ScaffoldScope

@Composable
fun ScaffoldScope.ResponsiveLayout(
    modifier: Modifier = Modifier,
    maxWidth: Dp = 600.dp,
    content: @Composable ScaffoldScope.() -> Unit,
) {
    Layout(
        modifier = modifier,
        content = { content() },
    ) { measurables, constraints ->
        val contentWidth = minOf(constraints.maxWidth, maxWidth.roundToPx())

        val childConstraints = constraints.copy(
            minWidth = contentWidth,
            maxWidth = contentWidth,
        )

        val placeables = measurables.map { it.measure(childConstraints) }

        val layoutWidth = if (constraints.hasBoundedWidth) constraints.maxWidth else contentWidth
        val layoutHeight = constraints.constrainHeight(placeables.maxOfOrNull { it.height } ?: 0)

        layout(layoutWidth, layoutHeight) {
            placeables.forEach { placeable ->
                placeable.placeRelative(
                    x = (layoutWidth - placeable.width) / 2,
                    y = 0,
                )
            }
        }
    }
}