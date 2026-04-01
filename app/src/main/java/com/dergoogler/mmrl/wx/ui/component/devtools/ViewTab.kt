package com.dergoogler.mmrl.wx.ui.component.devtools

import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dergoogler.mmrl.wx.R
import kotlinx.coroutines.launch

@Composable
fun ViewTab(
    state: PagerState,
    onDismissRequest: () -> Unit,
) {
    val statusBarHeight =
        WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val scope = rememberCoroutineScope()

    val pages = remember {
        listOf(
            R.string.dom,
            R.string.console,
            R.string.network
        )
    }

    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            DevToolsTabRow(
                modifier = Modifier
                    .padding(top = statusBarHeight)
                    .weight(1f),
                selectedTabIndex = state.currentPage,
                indicator = { tabPositions ->
                    AnimatedIndicator(
                        tabPositions = tabPositions,
                        selectedTabIndex = state.currentPage
                    )
                },
                divider = {}
            ) {
                pages.forEachIndexed { index, text ->
                    Tab(
                        modifier = Modifier.padding(vertical = 12.dp),
                        selected = state.currentPage == index,
                        onClick = {
                            scope.launch {
                                state.animateScrollToPage(index)
                            }
                        },
                        selectedContentColor = MaterialTheme.colorScheme.primary,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ) {
                        Text(
                            text = stringResource(text),
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            IconButton(onClick = onDismissRequest) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close DevTools",
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        HorizontalDivider(
            thickness = 0.3.dp,
        )
    }
}

@Composable
private fun Indicator(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier = modifier.defaultMinSize(minHeight = 3.dp),
    ) {
        val width = size.width / 4

        drawLine(
            color = color,
            start = Offset(width, size.height),
            end = Offset(width * 3, size.height),
            strokeWidth = size.height * 2,
            cap = StrokeCap.Round
        )
    }
}

@Composable
private fun AnimatedIndicator(tabPositions: List<TabPosition>, selectedTabIndex: Int) {
    val transition = updateTransition(selectedTabIndex, label = "Indicator")
    val indicatorStart by transition.animateDp(
        transitionSpec = {
            if (initialState < targetState) {
                spring(dampingRatio = 1f, stiffness = 50f)
            } else {
                spring(dampingRatio = 1f, stiffness = 1000f)
            }
        },
        label = "Indicator"
    ) {
        tabPositions[it].left
    }

    val indicatorEnd by transition.animateDp(
        transitionSpec = {
            if (initialState < targetState) {
                spring(dampingRatio = 1f, stiffness = 1000f)
            } else {
                spring(dampingRatio = 1f, stiffness = 50f)
            }
        },
        label = "Indicator"
    ) {
        tabPositions[it].right
    }

    Indicator(
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .wrapContentSize(align = Alignment.BottomStart)
            .offset(x = indicatorStart)
            .width(indicatorEnd - indicatorStart)
            .height(3.dp)
    )
}