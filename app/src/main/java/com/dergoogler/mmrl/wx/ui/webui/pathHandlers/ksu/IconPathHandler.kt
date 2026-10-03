package com.dergoogler.mmrl.wx.ui.webui.pathHandlers.ksu

import android.webkit.WebResourceResponse
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import com.dergoogler.mmrl.wx.ui.webui.util.packages
import dev.mmrlx.webui.PathHandler
import dev.mmrlx.webui.WebUI
import dev.mmrlx.webui.WebUIResourceRequest

class IconPathHandler(
    webui: WebUI,
) : PathHandler(webui) {
    override val id = "/"
    override val url = "ksu://icon".toUri()

    override fun handle(request: WebUIResourceRequest): WebResourceResponse {
        val path = request.path
        return requestIcon(path)
    }

    private fun requestIcon(packageName: String): WebResourceResponse {
        val appInfo = packages
            .find { it.packageName == packageName }
            ?.applicationInfo
        if (appInfo != null) {
            val drawable = appInfo.loadIcon(kontext.packageManager)
            val bitmap = drawable.toBitmap()
            val stream = java.io.ByteArrayOutputStream()
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream)
            return WebResourceResponse(
                "image/png", null, 200, "OK",
                mapOf("Access-Control-Allow-Origin" to "*"),
                java.io.ByteArrayInputStream(stream.toByteArray())
            )
        } else {
            val errorMsg = "No such package"
            val errorStream =
                java.io.ByteArrayInputStream(errorMsg.toByteArray(Charsets.UTF_8))
            return WebResourceResponse(
                "text/plain",
                "utf-8",
                404,
                "Not Found",
                mapOf("Access-Control-Allow-Origin" to "*"),
                errorStream
            )
        }
    }
}