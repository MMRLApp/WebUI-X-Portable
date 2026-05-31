package com.dergoogler.mmrl.wx.ui.webui.devtools

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import dev.mmrlx.compose.ui.HorizontalDivider
import dev.mmrlx.compose.ui.Surface
import dev.mmrlx.compose.ui.theme.MMRLXTheme

@Composable
fun DevToolsTabRow(
    selectedTabIndex: Int,
    modifier: Modifier = Modifier,
    containerColor: Color = MMRLXTheme.colors.card,
    contentColor: Color = MMRLXTheme.colors.cardForeground,
    indicator: @Composable (tabPositions: List<TabPosition>) -> Unit =
        { tabPositions ->
            if (selectedTabIndex < tabPositions.size) {
                Box(
                    Modifier
                        .tabIndicatorOffset(tabPositions[selectedTabIndex])
                        .fillMaxWidth()
                        .height(3.0.dp)
                        .background(color = MMRLXTheme.colors.primary)
                )
            }
        },
    divider: @Composable () -> Unit = { HorizontalDivider() },
    endContent: (@Composable () -> Unit)? = null,
    tabs: @Composable () -> Unit,
) {
    TabRowWithSubcomposeImpl(
        modifier,
        containerColor,
        contentColor,
        indicator,
        divider,
        tabs,
        endContent
    )
}

private enum class TabSlots {
    Tabs,
    EndContent,
    Divider,
    Indicator
}

@Immutable
class TabPosition internal constructor(
    val left: Dp,
    val width: Dp,
    val contentWidth: Dp,
) {
    val right: Dp get() = left + width
}

@Composable
private fun TabRowWithSubcomposeImpl(
    modifier: Modifier,
    containerColor: Color,
    contentColor: Color,
    indicator: @Composable (tabPositions: List<TabPosition>) -> Unit,
    divider: @Composable () -> Unit,
    tabs: @Composable () -> Unit,
    endContent: (@Composable () -> Unit)? = null,
) {
    Surface(
        modifier = modifier.selectableGroup(),
        color = containerColor,
        contentColor = contentColor
    ) {
        SubcomposeLayout(Modifier.fillMaxWidth()) { constraints ->

            val paddingPx = 8.dp.roundToPx()

            // Measure end content first
            val endPlaceables = endContent?.let {
                subcompose(TabSlots.EndContent, it).map {
                    it.measure(constraints.copy(minWidth = 0))
                }
            }.orEmpty()

            val endWidth = endPlaceables.sumOf { it.width }

            // Measure tabs
            val tabMeasurables = subcompose(TabSlots.Tabs, tabs)

            val tabPlaceables = tabMeasurables.fastMap {
                it.measure(
                    constraints.copy(
                        minWidth = 0,
                        maxWidth = Constraints.Infinity
                    )
                )
            }

            val tabRowHeight =
                maxOf(
                    tabPlaceables.maxOfOrNull { it.height } ?: 0,
                    endPlaceables.maxOfOrNull { it.height } ?: 0
                )

            var xPosition = 0

            val tabPositions = tabPlaceables.fastMap { placeable ->

                val paddedWidth = placeable.width + paddingPx * 2

                val position = TabPosition(
                    left = xPosition.toDp(),
                    width = paddedWidth.toDp(),
                    contentWidth = placeable.width.toDp()
                )

                xPosition += paddedWidth
                position
            }

            val layoutWidth = constraints.maxWidth

            layout(layoutWidth, tabRowHeight) {

                // Tabs from start
                var x = 0

                tabPlaceables.fastForEach { placeable ->
                    placeable.placeRelative(
                        x + paddingPx,
                        0
                    )
                    x += placeable.width + paddingPx * 2
                }

                // End content from right
                var endX = layoutWidth - endWidth

                endPlaceables.forEach { placeable ->
                    placeable.placeRelative(
                        endX,
                        (tabRowHeight - placeable.height) / 2
                    )
                    endX += placeable.width
                }

                subcompose(TabSlots.Divider, divider).fastForEach {
                    val placeable = it.measure(
                        constraints.copy(minHeight = 0)
                    )
                    placeable.placeRelative(
                        0,
                        tabRowHeight - placeable.height
                    )
                }

                subcompose(TabSlots.Indicator) {
                    indicator(tabPositions)
                }.fastForEach {
                    it.measure(
                        Constraints.fixed(layoutWidth, tabRowHeight)
                    ).placeRelative(0, 0)
                }
            }
        }
    }
}

fun Modifier.tabIndicatorOffset(currentTabPosition: TabPosition): Modifier =
    composed(
        inspectorInfo =
            debugInspectorInfo {
                name = "tabIndicatorOffset"
                value = currentTabPosition
            }
    ) {

        val currentTabWidth by animateDpAsState(
            targetValue = currentTabPosition.width,
            animationSpec = TabRowIndicatorSpec
        )

        val indicatorOffset by animateDpAsState(
            targetValue = currentTabPosition.left,
            animationSpec = TabRowIndicatorSpec
        )

        fillMaxWidth()
            .wrapContentSize(Alignment.BottomStart)
            .offset { IntOffset(indicatorOffset.roundToPx(), 0) }
            .width(currentTabWidth)
    }

private val TabRowIndicatorSpec: AnimationSpec<Dp> =
    tween(
        durationMillis = 250,
        easing = FastOutSlowInEasing
    )