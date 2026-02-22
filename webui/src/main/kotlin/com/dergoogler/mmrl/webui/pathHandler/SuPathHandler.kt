package com.dergoogler.mmrl.webui.pathHandler

import android.util.Log
import android.webkit.WebResourceResponse
import com.dergoogler.mmrl.hybridwebui.HybridWebUI
import com.dergoogler.mmrl.hybridwebui.HybridWebUIResourceRequest
import com.dergoogler.mmrl.platform.file.SuFile
import com.dergoogler.mmrl.webui.asResponse
import java.io.IOException

class SuPathHandler(
    private val directory: SuFile,
) : HybridWebUI.BasePathHandler() {
    override fun handle(request: HybridWebUIResourceRequest): WebResourceResponse {
        val path = request.path ?: return notFoundResponse

        return try {
            SuFile(directory, path).asResponse()
        } catch (e: IOException) {
            Log.e("suPathHandler", "Error opening webroot path: $path", e)
            return response(
                status = Companion.ResponseStatus.BAD_REQUEST,
                data = null
            )
        }
    }
}