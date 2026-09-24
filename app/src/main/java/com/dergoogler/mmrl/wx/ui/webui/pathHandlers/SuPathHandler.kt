package com.dergoogler.mmrl.wx.ui.webui.pathHandlers

import android.webkit.WebResourceResponse
import com.dergoogler.mmrl.wx.ui.webui.sufile
import com.dergoogler.mmrl.wx.ui.webui.util.asResponse
import dev.mmrlx.webui.ResponseStatus
import dev.mmrlx.webui.WebUI
import dev.mmrlx.webui.WebUIResourceRequest
import java.io.IOException

class SuPathHandler(
    webui: WebUI,
    override val id: String,
    private val directory: String,
) : KsuPathHandler(webui) {

    override fun handle(
        request: WebUIResourceRequest,
    ): WebResourceResponse {
        val path = request.path

        return try {
            sufile(directory, path).asResponse()
        } catch (e: IOException) {
            console.debugError("Error opening su path: $path", e)
            return response(
                status = ResponseStatus.BAD_REQUEST,
                data = null
            )
        }
    }
}