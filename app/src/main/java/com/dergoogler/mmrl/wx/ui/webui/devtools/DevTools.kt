package com.dergoogler.mmrl.wx.ui.webui.devtools

import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import com.dergoogler.mmrl.wx.ui.webui.devtools.tabs.ConsoleTab
import com.dergoogler.mmrl.wx.ui.webui.devtools.tabs.DomTab
import com.dergoogler.mmrl.wx.ui.webui.devtools.tabs.NetworkTab
import com.dergoogler.mmrl.wx.ui.webui.devtools.tabs.SnippetsTab
import dev.mmrlx.webui.WebUI

val LocalWebUI = compositionLocalOf<WebUI> { error("No WebUI provided") }

@Composable
fun DevTools(
    isOpen: Boolean,
    onDismissRequest: () -> Unit,
) {
    val pagerState = rememberPagerState(1) { 4 }

    BoxWithConstraints {
        val density = LocalDensity.current
        val maxHeightPx = with(density) { maxHeight.toPx() }
        val minHeightRatio = 0.3f
        val maxHeightRatio = 0.95f
        var sheetHeightRatio by rememberSaveable { mutableStateOf(0.55f) }
        val sheetHeight = remember(maxHeight, sheetHeightRatio) {
            maxHeight * sheetHeightRatio
        }

        val dragModifier = Modifier.pointerInput(maxHeightPx) {
            detectVerticalDragGestures { change, dragAmount ->
                change.consume()
                if (maxHeightPx <= 0f) return@detectVerticalDragGestures

                val ratioDelta = dragAmount / maxHeightPx
                sheetHeightRatio = (sheetHeightRatio - ratioDelta)
                    .coerceIn(minHeightRatio, maxHeightRatio)
            }
        }

        DevToolsContainer(
            isVisible = isOpen,
            panelHeight = sheetHeight,
            onDismissRequest = onDismissRequest,
            dragHandle = { modifier ->
                ViewTab(
                    modifier = modifier.then(dragModifier),
                    state = pagerState,
                    onDismissRequest = onDismissRequest
                )
            }
        ) {
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = false
            ) { pageIndex ->
                when (pageIndex) {
                    0 -> DomTab()
                    1 -> ConsoleTab()
                    2 -> NetworkTab()
                    3 -> SnippetsTab()
                }
            }
        }
    }
}

