package com.dergoogler.mmrl.wx.ui.webui.pathHandlers

import android.webkit.WebResourceResponse
import com.dergoogler.mmrl.webui.MimeUtil
import dev.mmrlx.webui.WebUI
import dev.mmrlx.webui.WebUIResourceRequest
import dev.mmrlx.webui.path.PathHandler
import java.io.IOException

class AssetsPathHandler(
    webui: WebUI,
) : PathHandler(webui) {
    override val id = ""

    private val assets get() = context.assets

    override fun handle(
        request: WebUIResourceRequest,
    ): WebResourceResponse {
        val path = request.path

        try {
            val inputStream = assets.open(path.removePrefix("/"))
            val mimeType = MimeUtil.getMimeFromFileName(path)
            return WebResourceResponse(mimeType, null, inputStream)
        } catch (e: IOException) {
            console.debugError("Error opening asset path: $path", e)
            return notFoundResponse
        }
    }
}

