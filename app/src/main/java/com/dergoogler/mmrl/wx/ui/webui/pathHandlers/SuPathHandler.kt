package com.dergoogler.mmrl.wx.ui.webui.pathHandlers

import com.dergoogler.mmrl.wx.ui.webui.sufile
import com.dergoogler.mmrl.wx.ui.webui.util.asResponse
import dev.mmrlx.webui.ResponseStatus
import dev.mmrlx.webui.RouteRegistry
import java.io.IOException

fun RouteRegistry.suRoute(name: String, directory: String) {
    route(name) {
        try {
            sufile(directory, request.path).asResponse()
        } catch (e: IOException) {
            console.debugError("Error opening su path: ${request.path}", e)
            response(
                status = ResponseStatus.BAD_REQUEST,
                data = null
            )
        }
    }
}
