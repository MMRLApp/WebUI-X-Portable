package com.dergoogler.mmrl.wx.ui.webui.pathHandlers

import com.dergoogler.mmrl.wx.model.WebColors
import com.dergoogler.mmrl.wx.ui.webui.mdColorScheme
import com.dergoogler.mmrl.wx.util.MimeUtil
import dev.mmrlx.webui.RouteRegistry

fun RouteRegistry.internalPathHandler() {
    val webColors = WebColors(mdColorScheme)

    route("/internal/insets.css") {
        insets.css.asStyleResponse()
    }

    route("/internal/colors.css") {
        webColors.allCssColors.asStyleResponse()
    }

    route("/internal/assets/*asset") {
        val filePath = request.params["asset"] ?: return@route notFoundResponse

        try {
            val inputStream = kontext.assets.open(filePath)
            val mimeType = MimeUtil.getMimeFromFileName(filePath)
            response(mimeType = mimeType, data = inputStream)
        } catch (e: Exception) {
            console.error("Failed to open $filePath from the assets: $e")
            notFoundResponse
        }
    }
}

