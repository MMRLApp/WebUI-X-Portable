package com.dergoogler.mmrl.webui.pathHandler

import android.util.Log
import android.webkit.WebResourceResponse
import com.dergoogler.mmrl.hybridwebui.HybridWebUI
import com.dergoogler.mmrl.hybridwebui.HybridWebUIResourceRequest
import com.dergoogler.mmrl.webui.MimeUtil
import com.dergoogler.mmrl.webui.util.WebUIOptions
import java.io.IOException

class AssetsPathHandler(
    private val options: WebUIOptions,
) : HybridWebUI.BasePathHandler() {

    private val assetHelper get() = options.context.assets

    override fun handle(request: HybridWebUIResourceRequest): WebResourceResponse {
        val path = request.path ?: return notFoundResponse

        try {
            val inputStream = assetHelper.open(path.removePrefix("/"))
            val mimeType = MimeUtil.getMimeFromFileName(path)
            return WebResourceResponse(mimeType, null, inputStream)
        } catch (e: IOException) {
            Log.e("assetsPathHandler", "Error opening asset path: $path", e)
            return com.dergoogler.mmrl.webui.notFoundResponse
        }
    }
}

