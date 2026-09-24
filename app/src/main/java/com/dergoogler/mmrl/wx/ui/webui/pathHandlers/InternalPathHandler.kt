package com.dergoogler.mmrl.wx.ui.webui.pathHandlers

import android.webkit.WebResourceResponse
import androidx.compose.material3.ColorScheme
import com.dergoogler.mmrl.webui.model.WebColors
import dev.mmrlx.webui.WebUI
import dev.mmrlx.webui.WebUIResourceRequest
import java.io.IOException

open class InternalPathHandler(
    webui: WebUI,
    colorScheme: ColorScheme,
) : KsuPathHandler(webui) {
    override val id = "/internal/"

    val webColors = WebColors(colorScheme)
    val assetsPathHandler = AssetsPathHandler(this)

    override fun handle(
        request: WebUIResourceRequest,
    ): WebResourceResponse {
        val path = request.path

        try {
            if (path.matches(Regex("insets\\.css"))) {
                return insets.css.asStyleResponse()
            }

            if (path.matches(Regex("colors\\.css"))) {
                return webColors.allCssColors.asStyleResponse()
            }

            if (path.matches(Regex("^assets(/.*)?$"))) {
                return assetsPathHandler.handle(
                    WebUIResourceRequest(
                        method = request.method,
                        isForMainFrame = request.isForMainFrame(),
                        url = request.url,
                        path = path.removePrefix("assets/"),
                        requestHeaders = request.getRequestHeaders(),
                        isRedirect = request.isRedirect(),
                        hasGesture = request.hasGesture()
                    )
                )
            }

            return notFoundResponse
        } catch (e: IOException) {
            console.debugError("Error opening internal asset path: $path", e)
            return notFoundResponse
        }
    }
}
