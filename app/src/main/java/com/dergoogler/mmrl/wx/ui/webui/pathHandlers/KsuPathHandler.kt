package com.dergoogler.mmrl.wx.ui.webui.pathHandlers

import android.net.Uri
import android.webkit.WebResourceResponse
import androidx.core.net.toUri
import dev.mmrlx.webui.PathHandler
import dev.mmrlx.webui.WebUI
import dev.mmrlx.webui.WebUIResourceRequest

open class KsuPathHandler(webui: WebUI) : PathHandler(webui) {
    override val url: Uri = "https://mui.kernelsu.org".toUri()

    override fun handle(request: WebUIResourceRequest): WebResourceResponse? {
        TODO("Not yet implemented")
    }

}