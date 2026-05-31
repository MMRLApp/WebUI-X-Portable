package com.dergoogler.mmrl.wx.ui.webui.interfaces.legacy

import com.dergoogler.mmrl.wx.ui.webui.module
import com.dergoogler.mmrl.wx.ui.webui.sanitizedIdWithFileOutputStream
import dev.mmrlx.webui.WebUI
import dev.mmrlx.webui.interfaces.ExportMethod
import dev.mmrlx.webui.interfaces.JavaScriptInterface
import java.io.BufferedOutputStream
import java.io.OutputStream

class FileOutputInterface(
    webui: WebUI,
) : JavaScriptInterface(webui) {
    override val prototypeClass = "FileOutputInterface"
    override val propertyName = module.sanitizedIdWithFileOutputStream

    @ExportMethod
    fun open(path: String, append: Boolean): JSObject? =
        try {
            val stream = outputStream(path, append)
            FileOutputInterfaceStream(this, stream)
        } catch (e: Exception) {
            console.error(e)
            null
        }

    @ExportMethod
    fun open(path: String): JSObject? = open(path, false)
}

class FileOutputInterfaceStream(
    webui: WebUI,
    outputStream: OutputStream,
) : JavaScriptInterface.JSObject, WebUI by webui {
    private val bufferedOutputStream = BufferedOutputStream(outputStream)

    fun getStream(): OutputStream = bufferedOutputStream

    @ExportMethod
    fun write(b: Int) {
        try {
            bufferedOutputStream.write(b)
        } catch (e: Exception) {
            console.error("Failed to write byte", e)
        }
    }

    @ExportMethod
    fun flush() {
        try {
            bufferedOutputStream.flush()
        } catch (e: Exception) {
            console.error("Failed to flush stream", e)
        }
    }

    @ExportMethod
    fun close() {
        try {
            bufferedOutputStream.close()
        } catch (e: Exception) {
            console.error("Failed to close stream", e)
        }
    }
}