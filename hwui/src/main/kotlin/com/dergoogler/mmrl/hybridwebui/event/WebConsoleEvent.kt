package com.dergoogler.mmrl.hybridwebui.event

import android.webkit.ConsoleMessage
import com.dergoogler.mmrl.hybridwebui.ConsoleEntry
import com.dergoogler.mmrl.hybridwebui.HybridWebUI
import com.dergoogler.mmrl.hybridwebui.HybridWebUIEvent
import com.dergoogler.mmrl.hybridwebui.ResultNode
import com.dergoogler.mmrl.hybridwebui.store.error
import org.json.JSONObject

class WebConsoleEvent : HybridWebUI.EventListener() {
    private fun getParsedLevel(level: String?): ConsoleMessage.MessageLevel = when (level) {
        "log" -> ConsoleMessage.MessageLevel.LOG
        "error" -> ConsoleMessage.MessageLevel.ERROR
        "warn" -> ConsoleMessage.MessageLevel.WARNING
        "info" -> ConsoleMessage.MessageLevel.LOG
        "debug" -> ConsoleMessage.MessageLevel.DEBUG
        "tip" -> ConsoleMessage.MessageLevel.TIP
        "verbose" -> ConsoleMessage.MessageLevel.DEBUG
        else -> ConsoleMessage.MessageLevel.LOG
    }

    override fun listen(view: HybridWebUI, event: HybridWebUIEvent) {
        val console = view.consoleLogs
        val data = event.message.data ?: return

        try {
            // JSON arrives as the raw string passed from JS — e.g. '{"v":[...]}'
            // No extra JSONObject wrapping needed here unlike evaluateJavascript callbacks
            val outer = JSONObject(data)

            val rawLevel = outer.getString("l")
            val arr = outer.getJSONArray("v")
            val args = (0 until arr.length()).map { i ->
                ResultNode.parse(arr.getJSONObject(i), key = null, depth = 0)
            }

            val level = getParsedLevel(rawLevel)

            console.add(
                ConsoleEntry(level = level, args = args, source = "", line = 0)
            )
        } catch (e: Exception) {
            console.error(e)
        }
    }
}