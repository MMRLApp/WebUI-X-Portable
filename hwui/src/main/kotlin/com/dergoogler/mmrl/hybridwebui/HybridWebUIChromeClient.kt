package com.dergoogler.mmrl.hybridwebui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView

open class HybridWebUIChromeClient : WebChromeClient() {
    override fun onShowFileChooser(
        view: WebView?,
        filePathCallback: ValueCallback<Array<Uri>>?,
        fileChooserParams: FileChooserParams?,
    ): Boolean {
        if (view !is HybridWebUI) {
            return false
        }

        HybridWebUIState.filePathCallback?.onReceiveValue(null)
        HybridWebUIState.filePathCallback = filePathCallback
        val intent = fileChooserParams?.createIntent() ?: Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
        }
        if (fileChooserParams?.mode == FileChooserParams.MODE_OPEN_MULTIPLE) {
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        try {
            HybridWebUIState.fileChooserLauncher?.launch(intent)
        } catch (_: ActivityNotFoundException) {
            HybridWebUIState.filePathCallback?.onReceiveValue(null)
            HybridWebUIState.filePathCallback = null
            return false
        }

        return true
    }
}