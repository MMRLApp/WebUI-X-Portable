package com.dergoogler.mmrl.wx.ui.webui.pathHandlers.ksu

import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import com.dergoogler.mmrl.wx.ui.webui.util.packages
import dev.mmrlx.webui.RouteRegistry
import dev.mmrlx.webui.WebResourceResponse
import dev.mmrlx.webui.WebUI

fun RouteRegistry.iconPathHandler() {
    base = "ksu://icon/".toUri()

    route("/") {
        val path = request.path
        requestIcon(path.removePrefix("/"))
    }
}

private fun WebUI.requestIcon(packageName: String): WebResourceResponse {
    val appInfo = packages
        .find { it.packageName == packageName }
        ?.applicationInfo
    if (appInfo != null) {
        val drawable = appInfo.loadIcon(kontext.packageManager)
        val bitmap = drawable.toBitmap()
        val stream = java.io.ByteArrayOutputStream()
        bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream)
        return WebResourceResponse(
            "image/png", "UTF-8", 200, "OK",
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
