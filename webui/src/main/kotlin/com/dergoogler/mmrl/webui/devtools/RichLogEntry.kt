package com.dergoogler.mmrl.webui.devtools

import android.webkit.ConsoleMessage

data class RichLogEntry(
    val level: ConsoleMessage.MessageLevel,
    val args: List<ResultNode>,
    val source: String,
    val line: Int,
    val timestamp: Long = System.currentTimeMillis(),
) {
    companion object {
        fun ConsoleMessage.toRichLogEntry(): RichLogEntry {
            return RichLogEntry(
                level = messageLevel(),
                args = listOf(
                    ResultNode.Primitive(
                        key = null,
                        value = message(),
                        kind = PrimitiveKind.STRING,
                        depth = 0
                    )
                ),
                source = sourceId(),
                line = lineNumber(),
            )
        }
    }
}