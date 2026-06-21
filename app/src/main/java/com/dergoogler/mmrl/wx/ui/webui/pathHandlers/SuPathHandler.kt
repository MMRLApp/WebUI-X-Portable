package com.dergoogler.mmrl.wx.ui.webui.pathHandlers

import android.net.Uri
import android.webkit.WebResourceResponse
import com.dergoogler.mmrl.platform.file.SuFile
import com.dergoogler.mmrl.webui.asResponse
import dev.mmrlx.webui.PathHandler
import dev.mmrlx.webui.ResponseStatus
import dev.mmrlx.webui.WebUI
import dev.mmrlx.webui.WebUIResourceRequest
import java.io.IOException

class SuPathHandler(
    webui: WebUI,
    override val id: String,
    override val url: Uri,
    private val directory: String,
) : PathHandler(webui) {

    override fun handle(
        request: WebUIResourceRequest,
    ): WebResourceResponse {
        val path = request.path

        return try {
            SuFile(directory, path).asResponse()
        } catch (e: IOException) {
            console.debugError("Error opening su path: $path", e)
            return response(
                status = ResponseStatus.BAD_REQUEST,
                data = null
            )
        }
    }
}