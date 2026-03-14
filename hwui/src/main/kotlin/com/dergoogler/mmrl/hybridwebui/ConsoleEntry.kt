package com.dergoogler.mmrl.hybridwebui

import android.webkit.ConsoleMessage

data class ConsoleEntry(
    val level: ConsoleMessage.MessageLevel,
    val args: List<ResultNode>,
    val source: String,
    val line: Int,
    val timestamp: Long = System.currentTimeMillis(),
) {
    companion object {
        fun ConsoleMessage.toConsoleEntry(): ConsoleEntry {
            return ConsoleEntry(
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