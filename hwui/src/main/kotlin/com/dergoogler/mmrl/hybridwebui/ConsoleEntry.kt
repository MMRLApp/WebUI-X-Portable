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

        fun info(vararg args: Any?, source: String, line: Int): ConsoleEntry {
            return logLevel(
                *args,
                source = source,
                line = line,
                level = ConsoleMessage.MessageLevel.LOG
            )
        }

        fun warn(vararg args: Any?, source: String, line: Int): ConsoleEntry {
            return logLevel(
                *args,
                source = source,
                line = line,
                level = ConsoleMessage.MessageLevel.WARNING
            )
        }

        fun error(vararg args: Any?, source: String, line: Int): ConsoleEntry {
            return logLevel(
                *args,
                source = source,
                line = line,
                level = ConsoleMessage.MessageLevel.ERROR
            )
        }

        fun debug(vararg args: Any?, source: String, line: Int): ConsoleEntry {
            return logLevel(
                *args,
                source = source,
                line = line,
                level = ConsoleMessage.MessageLevel.DEBUG
            )
        }

        fun trace(vararg args: Any?, source: String, line: Int): ConsoleEntry {
            return logLevel(
                *args,
                source = source,
                line = line,
                level = ConsoleMessage.MessageLevel.TIP
            )
        }

        private fun logLevel(
            vararg args: Any?,
            source: String,
            line: Int,
            level: ConsoleMessage.MessageLevel,
        ): ConsoleEntry {
            return ConsoleEntry(
                level = level,
                args = args.map {
                    ResultNode.Primitive(
                        key = null,
                        value = it.toString(),
                        kind = PrimitiveKind.parse(it),
                        depth = 0
                    )

                },
                source = source,
                line = line,
            )
        }
    }
}