package com.dergoogler.mmrl.webui.pathHandler

import android.util.Log
import android.webkit.WebResourceResponse
import com.dergoogler.mmrl.hybridwebui.HybridWebUI
import com.dergoogler.mmrl.hybridwebui.HybridWebUIInsets
import com.dergoogler.mmrl.hybridwebui.HybridWebUIResourceRequest
import com.dergoogler.mmrl.webui.model.WebColors
import com.dergoogler.mmrl.webui.util.WebUIOptions
import java.io.IOException

class InternalPathHandler(
    private val options: WebUIOptions,
    private val insets: HybridWebUIInsets,
) : HybridWebUI.BasePathHandler() {
    val colorScheme get() = options.colorScheme
    val webColors get() = WebColors(colorScheme)

    val assetsPathHandler = AssetsPathHandler(options)

    override fun handle(request: HybridWebUIResourceRequest): WebResourceResponse {
        val path = request.path ?: return notFoundResponse

        try {
            if (path.matches(Regex("^assets(/.*)?$"))) {
                return assetsPathHandler.handle(
                    HybridWebUIResourceRequest(
                        method = request.method,
                        isForMainFrame = request.isForMainFrame,
                        url = request.url,
                        path = path.removePrefix("assets/"),
                        requestHeaders = request.requestHeaders,
                        isRedirect = request.isRedirect,
                        hasGesture = request.hasGesture
                    )
                )
            }

            if (path.matches(Regex("insets\\.css"))) {
                return insets.css.asStyleResponse()
            }

            if (path.matches(Regex("colors\\.css"))) {
                return webColors.allCssColors.asStyleResponse()
            }

            return notFoundResponse
        } catch (e: IOException) {
            Log.e("InternalPathHandler", "Error opening mmrl asset path: $path", e)
            return notFoundResponse
        }
    }
}
