package com.dergoogler.mmrl.wx.ui.webui.interfaces.legacy

import android.webkit.JavascriptInterface
import com.dergoogler.mmrl.wx.ui.webui.module
import com.dergoogler.mmrl.wx.ui.webui.sanitizedIdWithFileInputStream
import dev.mmrlx.utilities.json.toJSONArray
import dev.mmrlx.webui.JavaScriptInterface
import dev.mmrlx.webui.WebUI
import dev.mmrlx.webui.javascript.annotation.ExportMethod
import org.json.JSONArray
import java.io.BufferedInputStream
import java.io.InputStream

class FileInputInterface(
    webui: WebUI,
) : JavaScriptInterface(webui) {
    override val prototypeClass = "FileInputInterface"
    override val propertyName = module.sanitizedIdWithFileInputStream

    @ExportMethod
    fun open(path: String): JSObject? =
        try {
            val stream = inputStream(path)
            FileInputInterfaceStream(this, stream)
        } catch (e: Exception) {
            console.error(e)
            null
        }
}

class FileInputInterfaceStream(
    webui: WebUI,
    inputStream: InputStream,
) : JavaScriptInterface.JSObject, WebUI by webui {
    private val bufferedInputStream = BufferedInputStream(inputStream)

    fun getStream(): InputStream = bufferedInputStream

    @ExportMethod
    fun read(): Int {
        return try {
            bufferedInputStream.read()
        } catch (e: Exception) {
            console.error("Failed to read byte", e)
            -1
        }
    }

    @ExportMethod
    fun readChunk(chunkSize: Int): JSONArray? {
        val buffer = ByteArray(chunkSize)
        val bytesRead = bufferedInputStream.read(buffer)
        return if (bytesRead > 0) {
            buffer.copyOf(bytesRead).toJSONArray()
        } else {
            null
        }
    }

    @JavascriptInterface
    fun close() {
        try {
            bufferedInputStream.close()
        } catch (e: Exception) {
            console.error("Failed to close stream", e)
        }
    }

    @JavascriptInterface
    fun skip(n: Long): Long {
        return try {
            bufferedInputStream.skip(n)
        } catch (e: Exception) {
            console.error("Failed to skip bytes", e)
            -1
        }
    }
}