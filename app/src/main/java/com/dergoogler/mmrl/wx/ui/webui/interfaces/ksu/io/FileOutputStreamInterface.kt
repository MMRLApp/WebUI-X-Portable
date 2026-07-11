@file:Suppress("unused")

package com.dergoogler.mmrl.wx.ui.webui.interfaces.ksu.io

import android.util.Base64
import android.util.Log
import android.webkit.JavascriptInterface
import dev.mmrlx.webui.PureJavaScriptInterface
import dev.mmrlx.webui.WebUI
import java.io.BufferedOutputStream
import java.util.UUID

class FileOutputStreamInterface(webui: WebUI): PureJavaScriptInterface(webui) {
    @JavascriptInterface
    fun open(path: String, append: Boolean): String = runCatching {
        val f = file(path)
        val fos = outputStream(f, append)
        val bos = BufferedOutputStream(fos, 64 * 1024)
        val id = UUID.randomUUID().toString()
        KsuIO.openOutputStreams[id] = bos
        id
    }.onFailure { Log.e(TAG, "FileOutputStream open failed", it) }.getOrElse { "" }

    @JavascriptInterface
    fun open(path: String): String = open(path, false)

    @JavascriptInterface
    fun writeByte(id: String, b: Int): Boolean = runCatching {
        val bos = KsuIO.openOutputStreams[id] ?: return false
        synchronized(bos) { bos.write(b) }
        true
    }.onFailure { Log.e(TAG, "writeByte failed", it) }.getOrElse { false }

    @JavascriptInterface
    fun write(id: String, base64: String): Boolean = runCatching {
        val bos = KsuIO.openOutputStreams[id] ?: return false
        val data = Base64.decode(base64, Base64.NO_WRAP)
        synchronized(bos) { bos.write(data) }
        true
    }.onFailure { Log.e(TAG, "write failed", it) }.getOrElse { false }

    @JavascriptInterface
    fun flush(id: String): Boolean = runCatching {
        val bos = KsuIO.openOutputStreams[id] ?: return false
        synchronized(bos) { bos.flush() }
        true
    }.onFailure { Log.e(TAG, "flush failed", it) }.getOrElse { false }

    @JavascriptInterface
    fun close(id: String): Boolean {
        val bos = KsuIO.openOutputStreams.remove(id) ?: return false
        return runCatching {
            synchronized(bos) { bos.close() }
            true
        }.onFailure { Log.e(TAG, "close failed", it) }.getOrElse { false }
    }

    fun closeAll() {
        while (true) {
            val entry = KsuIO.openOutputStreams.entries.firstOrNull() ?: break
            val id = entry.key
            val bos = entry.value
            if (!KsuIO.openOutputStreams.remove(id, bos)) {
                continue
            }
            runCatching {
                synchronized(bos) { bos.close() }
            }.onFailure { Log.e(TAG, "closeAll failed for $id", it) }
        }
    }
}
