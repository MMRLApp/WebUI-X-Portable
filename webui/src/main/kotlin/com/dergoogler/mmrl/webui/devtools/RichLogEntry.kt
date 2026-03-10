package com.dergoogler.mmrl.webui.devtools

import android.webkit.ConsoleMessage

data class RichLogEntry(
    val level: ConsoleMessage.MessageLevel,
    val args: List<ResultNode>,
    val source: String,
    val line: Int,
    val timestamp: Long = System.currentTimeMillis(),
)