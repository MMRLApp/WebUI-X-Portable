package com.dergoogler.mmrl.webui.handler

import android.util.Log
import com.dergoogler.mmrl.webui.PathHandler
import com.dergoogler.mmrl.webui.asStyleResponse
import com.dergoogler.mmrl.webui.model.Insets
import com.dergoogler.mmrl.webui.model.WebColors
import com.dergoogler.mmrl.webui.notFoundResponse
import com.dergoogler.mmrl.webui.util.WebUIOptions
import java.io.IOException

fun internalPathHandler(
    options: WebUIOptions,
    insets: Insets
): PathHandler {
    val colorScheme = options.colorScheme
    val webColors = WebColors(colorScheme)

    val assetsHandler = assetsPathHandler(options)

    return handler@{ path ->
        try {
            if (path.matches(Regex("^assets(/.*)?$"))) {
                return@handler assetsHandler(path.removePrefix("assets/"))
            }

            if (path.matches(Regex("insets\\.css"))) {
                return@handler insets.cssResponse
            }

            if (path.matches(Regex("colors\\.css"))) {
                return@handler webColors.allCssColors.asStyleResponse()
            }

            return@handler notFoundResponse
        } catch (e: IOException) {
            Log.e("mmrlPathHandler", "Error opening mmrl asset path: $path", e)
            return@handler notFoundResponse
        }
    }
}