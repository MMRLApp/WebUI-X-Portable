package com.dergoogler.mmrl.wx.ui.component.devtools

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.dergoogler.mmrl.webui.view.WXView

@Composable
fun DevTools(
    isOpen: Boolean,
    onDismissRequest: () -> Unit,
    webview: WXView,
) {
    val pagerState = rememberPagerState { 3 }

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
            state = pagerState, modifier = Modifier.fillMaxSize(), userScrollEnabled = false
        ) { pageIndex ->
            when (pageIndex) {
                0 -> DomTab(webview)
                1 -> ConsoleTab(webview)
                2 -> NetworkTab()
            }
        }
    }
}

