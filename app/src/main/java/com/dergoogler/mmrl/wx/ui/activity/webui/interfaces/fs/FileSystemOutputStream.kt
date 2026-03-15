package com.dergoogler.mmrl.wx.ui.activity.webui.interfaces.fs

import android.annotation.SuppressLint
import android.graphics.Bitmap
import androidx.annotation.UiThread
import androidx.webkit.WebMessageCompat
import com.dergoogler.mmrl.hybridwebui.HybridWebUI
import com.dergoogler.mmrl.hybridwebui.HybridWebUIEvent
import com.dergoogler.mmrl.platform.file.SuFile
import com.dergoogler.mmrl.webui.interfaces.WXInterface
import com.dergoogler.mmrl.webui.interfaces.WXOptions
import com.dergoogler.mmrl.wx.util.iife
import com.dergoogler.mmrl.wx.util.scrambleClassName

class FileSystemOutputStream(wxOptions: WXOptions) : WXInterface(wxOptions) {
    private val objectName = scrambleClassName<FileSystemOutputStream>()
    override var name = "__wx--$objectName"

    init {
        view.addEventListener(objectName, FileSystemOutputStreamEvent())
    }

    override fun onDestroy() {
        view.removeEventListener(objectName)
        super.onDestroy()
    }

    private class FileSystemOutputStreamEvent : HybridWebUI.EventListener() {
        private var currentPath: String? = null

        @UiThread
        @SuppressLint("RequiresFeature")
        override fun listen(view: HybridWebUI, event: HybridWebUIEvent) = with(event) {
            when (message.type) {
                WebMessageCompat.TYPE_STRING -> {
                    // Initialize the file path
                    currentPath = message.data
                    if (currentPath == null) {
                        reply.postMessage("Failed! Path was null.")
                        return
                    }

                    val file = SuFile(currentPath!!)
                    if (!file.exists()) {
                        try {
                            file.createNewFile()
                        } catch (e: Exception) {
                            reply.postMessage("Failed to create file: ${e.message}")
                            return
                        }
                    }

                    reply.postMessage("Path set")
                }

                WebMessageCompat.TYPE_ARRAY_BUFFER -> {
                    if (currentPath == null) {
                        reply.postMessage("Failed! Path not set before sending chunk.")
                        return
                    }

                    try {
                        val bytes = message.arrayBuffer
                        val file = SuFile(currentPath!!)
                        file.newOutputStream(false).use { it.write(bytes) }
                        reply.postMessage("Chunk written: ${bytes.size} bytes")
                    } catch (e: Exception) {
                        reply.postMessage("Failed to write chunk: ${e.message}")
                    }
                }

                else -> {
                    reply.postMessage("Failed! Unsupported message type: ${message.type}")
                }
            }
        }
    }

    override fun onPageStarted(view: HybridWebUI, url: String, favicon: Bitmap?) {
        runJs("""window.fs = window.fs || {};
            
const interfaceRef = window["$objectName"]
delete window["$objectName"]
delete window["__wx--$objectName"]
            
window.fs.newOutputStream = async function(path) {
  if (typeof path !== "string") {
    throw new TypeError("'path' must be a string")
  }

  if (path.trim() === "") {
    throw new Error("'path' cannot be empty")
  }

  if (
    !interfaceRef ||
    typeof interfaceRef.postMessage !== "function"
  ) {
    throw new Error(
      `Unable to find 'window["$objectName"]'. The required filesystem permission may not be granted.`
    )
  }

  let pathSet = false
  let isAborted = false

  const postMessageWithReply = message => {
    return new Promise((resolve, reject) => {
      if (isAborted) {
        reject(new DOMException("Stream has been aborted", "AbortError"))
        return
      }

      const handler = event => {
        const data = event.data

        if (typeof data === "string") {
          if (data.startsWith("Failed")) {
            reject(new Error(data))
          } else {
            resolve(data)
          }
        } else {
          reject(new Error("Received non-string response"))
        }
      }

      interfaceRef.addEventListener("message", handler)

      try {
        interfaceRef.postMessage(message)
      } catch (error) {
        reject(new Error(`Failed to send message: ${'$'}{error}`))
      }
    })
  }

  return new WritableStream({
    async start() {
      const reply = await postMessageWithReply(path)

      if (reply !== "Path set") {
        throw new Error(`Failed to set path: ${'$'}{reply}`)
      }

      pathSet = true
    },

    async write(chunk) {
      if (isAborted) {
        throw new DOMException("Stream has been aborted", "AbortError")
      }

      if (!(chunk instanceof Uint8Array)) {
        throw new TypeError("Chunk must be Uint8Array")
      }

      if (!pathSet) {
        throw new Error("Path not set before writing chunk")
      }

      await postMessageWithReply(chunk.buffer)
    },

    async close() {},

    abort(reason) {
      isAborted = true
      console.warn("WritableStream aborted:", reason)
    }
  })
};""".trimIndent().iife)
        super.onPageStarted(view, url, favicon)
    }
}