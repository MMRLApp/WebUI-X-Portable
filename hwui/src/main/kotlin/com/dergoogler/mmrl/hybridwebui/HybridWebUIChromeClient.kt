package com.dergoogler.mmrl.hybridwebui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.webkit.ConsoleMessage
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import com.dergoogler.mmrl.hybridwebui.ConsoleEntry.Companion.toConsoleEntry

open class HybridWebUIChromeClient(
    protected val view: HybridWebUI,
) : WebChromeClient() {
    protected val store get() = view.store
    protected val console get() = store.consoleStore
    protected val network get() = store.networkStore

    override fun onShowFileChooser(
        view: WebView?,
        filePathCallback: ValueCallback<Array<Uri>>?,
        fileChooserParams: FileChooserParams?,
    ): Boolean {
        if (view !is HybridWebUI) {
            return false
        }

        store.filePathCallback?.onReceiveValue(null)
        store.filePathCallback = filePathCallback
        val intent = fileChooserParams?.createIntent() ?: Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
        }
        if (fileChooserParams?.mode == FileChooserParams.MODE_OPEN_MULTIPLE) {
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        try {
            store.fileChooserLauncher?.launch(intent)
        } catch (_: ActivityNotFoundException) {
            store.filePathCallback?.onReceiveValue(null)
            store.filePathCallback = null
            return false
        }

        return true
    }

    override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
        console.add(consoleMessage.toConsoleEntry())
        return super.onConsoleMessage(consoleMessage)
    }
}