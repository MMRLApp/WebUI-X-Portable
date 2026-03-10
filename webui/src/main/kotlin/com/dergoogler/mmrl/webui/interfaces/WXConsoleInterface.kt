@file:Suppress("unused")

package com.dergoogler.mmrl.webui.interfaces

import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import com.dergoogler.mmrl.webui.devtools.PrimitiveKind
import com.dergoogler.mmrl.webui.devtools.ResultNode
import com.dergoogler.mmrl.webui.devtools.RichLogEntry
import org.json.JSONObject

class WXConsoleInterface(wxOptions: WXOptions) : WXInterface(wxOptions) {
    override var name = "__wxConsole"

    @JavascriptInterface
    fun log(json: String) = push(ConsoleMessage.MessageLevel.LOG, json)

    @JavascriptInterface
    fun warn(json: String) = push(ConsoleMessage.MessageLevel.WARNING, json)

    @JavascriptInterface
    fun error(json: String) = push(ConsoleMessage.MessageLevel.ERROR, json)

    @JavascriptInterface
    fun info(json: String) = push(ConsoleMessage.MessageLevel.LOG, json)

    @JavascriptInterface
    fun debug(json: String) = push(ConsoleMessage.MessageLevel.DEBUG, json)

    @JavascriptInterface
    fun tip(json: String) = push(ConsoleMessage.MessageLevel.TIP, json)

    private fun push(level: ConsoleMessage.MessageLevel, json: String) {
        try {
            // json arrives as the raw string passed from JS — e.g. '{"v":[...]}'
            // No extra JSONObject wrapping needed here unlike evaluateJavascript callbacks
            val outer = JSONObject(json)
            val arr = outer.getJSONArray("v")
            val args = (0 until arr.length()).map { i ->
                ResultNode.parse(arr.getJSONObject(i), key = null, depth = 0)
            }
            richLogs.add(
                RichLogEntry(level = level, args = args, source = "", line = 0)
            )
        } catch (e: Exception) {
            richLogs.add(
                RichLogEntry(
                    level = level,
                    args = listOf(
                        ResultNode.Primitive(null, json, PrimitiveKind.OTHER, 0)
                    ),
                    source = "",
                    line = 0
                )
            )
        }
    }
}