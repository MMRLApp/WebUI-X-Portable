package com.dergoogler.mmrl.webui.client

import android.os.Build
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewRenderProcess
import android.webkit.WebViewRenderProcessClient
import androidx.annotation.RequiresApi
import com.dergoogler.mmrl.ui.component.dialog.ConfirmData
import com.dergoogler.mmrl.ui.component.dialog.confirm
import com.dergoogler.mmrl.webui.R
import com.dergoogler.mmrl.webui.util.WebUIOptions

@RequiresApi(Build.VERSION_CODES.Q)
open class WXRenderProcessClient(
    private val options: WebUIOptions,
) : WebViewRenderProcessClient() {
    override fun onRenderProcessUnresponsive(
        view: WebView,
        renderer: WebViewRenderProcess?,
    ) {
        options.context.confirm(
            ConfirmData(
                title = options.context.getString(R.string.says, options.modId.id),
                description = options.context.getString(R.string.renderer_crashed),
                onConfirm = {
                    renderer?.terminate()
                }
            ),
            options.colorScheme
        )
    }

    override fun onRenderProcessResponsive(
        view: WebView,
        renderer: WebViewRenderProcess?,
    ) {
        Log.d(TAG, "onRenderProcessResponsive")
    }

    private companion object {
        const val TAG = "WXRenderProcessClient"
    }
}
