package com.dergoogler.mmrl.wx.ui.webui.devtools

import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
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

    DevToolsContainer(
        isVisible = isOpen,
        onDismissRequest = onDismissRequest,
        dragHandle = {
            ViewTab(
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

