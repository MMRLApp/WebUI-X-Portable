package com.dergoogler.mmrl.wx.ui.screens.crash.components

import dev.mmrlx.webui.RouteRegistry

fun RouteRegistry.markdownPathHandler(content: String) {
    route("readme.md") {
        htmlResponse(content)
    }
}