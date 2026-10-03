@file:Suppress("FunctionName", "unused")

package com.dergoogler.mmrl.wx.ui.webui.interfaces.ksu.io

import android.webkit.JavascriptInterface
import dev.mmrlx.webui.PureJavaScriptInterface
import dev.mmrlx.webui.WebUI
import java.io.OutputStream
import java.util.concurrent.ConcurrentHashMap

const val TAG = "KsuIO"

class KsuIO(webui: WebUI) : PureJavaScriptInterface(webui) {
    internal companion object {
        val openInputStreams = ConcurrentHashMap<String, ManagedInputStream>()
        val openOutputStreams = ConcurrentHashMap<String, OutputStream>()
    }

    private val fileOutputStream = FileOutputStreamInterface(this)
    private val fileInputStream = FileInputStreamInterface(this)
    private val randomAccessFile = RandomAccessFileInterface(this)

    @JavascriptInterface
    fun File(path: String): FileInterface = FileInterface(this, path)

    @JavascriptInterface
    fun FileInputStream(): FileInputStreamInterface = fileInputStream

    @JavascriptInterface
    fun FileOutputStream(): FileOutputStreamInterface = fileOutputStream

    @JavascriptInterface
    fun RandomAccessFile(): RandomAccessFileInterface = randomAccessFile

    init {
        onDestroy {
            fileInputStream.closeAll()
            fileOutputStream.closeAll()
            randomAccessFile.closeAll()
        }
    }
}
