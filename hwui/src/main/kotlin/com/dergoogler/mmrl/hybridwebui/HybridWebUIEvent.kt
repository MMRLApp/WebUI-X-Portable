package com.dergoogler.mmrl.hybridwebui

import android.net.Uri
import android.webkit.WebView
import androidx.webkit.JavaScriptReplyProxy
import androidx.webkit.WebMessageCompat

data class HybridWebUIEvent(
    val view: WebView,
    val message: WebMessageCompat,
    val reply: JavaScriptReplyProxy,
    val sourceOrigin: Uri,
    val isMainFrame: Boolean,
)