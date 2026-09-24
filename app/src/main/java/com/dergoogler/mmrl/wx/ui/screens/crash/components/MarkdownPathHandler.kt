package com.dergoogler.mmrl.wx.ui.screens.crash.components

import android.net.Uri
import android.webkit.WebResourceResponse
import androidx.compose.material3.ColorScheme
import androidx.core.net.toUri
import com.dergoogler.mmrl.wx.ui.webui.pathHandlers.InternalPathHandler
import dev.mmrlx.webui.WebUI
import dev.mmrlx.webui.WebUIResourceRequest

class MarkdownPathHandler(
    webui: WebUI,
    private val content: String,
    colorScheme: ColorScheme,
) : InternalPathHandler(webui, colorScheme) {
    override val url: Uri = "https://desc.mmrl.dev".toUri()

    override fun handle(request: WebUIResourceRequest): WebResourceResponse {
        val path = request.path

        if (path.matches(Regex("readme\\.md"))) {
            return htmlResponse(content)
        }

        return super.handle(request)
    }
}