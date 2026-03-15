package com.dergoogler.mmrl.wx.ui.activity.webui.interfaces.fs

import android.annotation.SuppressLint
import android.graphics.Bitmap
import androidx.annotation.UiThread
import com.dergoogler.mmrl.hybridwebui.HybridWebUI
import com.dergoogler.mmrl.hybridwebui.HybridWebUIEvent
import com.dergoogler.mmrl.platform.file.SuFile
import com.dergoogler.mmrl.webui.interfaces.WXInterface
import com.dergoogler.mmrl.webui.interfaces.WXOptions
import com.dergoogler.mmrl.wx.util.iife
import com.dergoogler.mmrl.wx.util.scrambleClassName
import org.json.JSONObject

class FileSystemStat(wxOptions: WXOptions) : WXInterface(wxOptions) {
    private val objectName = scrambleClassName<FileSystemStat>()
    override var name = "__wx--$objectName"

    init {
        view.addEventListener(objectName, FileSystemStatEvent())
    }

    override fun onDestroy() {
        view.removeEventListener(objectName)
        super.onDestroy()
    }

    private class FileSystemStatEvent : HybridWebUI.EventListener() {
        @UiThread
        @SuppressLint("RequiresFeature")
        override fun listen(view: HybridWebUI, event: HybridWebUIEvent) = with(event) {
            val data: String? = message.data

            if (data == null) {
                reply.postMessage("Failed! Data was null.")
                return
            }

            val file = SuFile(data)

            if (!file.exists()) {
                reply.postMessage("Failed! File does not exist.")
                return
            }

            try {
                val stat = JSONObject().apply {
                    put("size", file.length())
                    put("lastModified", file.lastModified())
                    put("isFile", file.isFile)
                    put("isDirectory", file.isDirectory)
                    // ExtendedFile from libsu does not expose symlink info directly,
                    // so we use the canonical vs absolute path heuristic as a best-effort check
                    put("isSymbolicLink", file.canonicalPath != file.absolutePath)
                }
                reply.postMessage(stat.toString())
            } catch (e: Exception) {
                reply.postMessage("Failed! ${e.message}")
            }
        }
    }

    override fun onPageStarted(view: HybridWebUI, url: String, favicon: Bitmap?) {
        runJs("""window.fs = window.fs || {};

const interfaceRef = window["$objectName"]
delete window["$objectName"]
delete window["__wx--$objectName"]
            
window.fs.stat = async function(path) {
  if (typeof path !== "string") {
    throw new TypeError("'path' must be a string")
  }

  if (path.trim() === "") {
    throw new Error("'path' cannot be empty")
  }

  if (!interfaceRef || typeof interfaceRef.postMessage !== "function") {
    throw new Error(
      `Unable to find 'window["$objectName"]'. The required filesystem permission may not be granted.`
    )
  }

  return new Promise((resolve, reject) => {
    let messageHandler = null

    const cleanup = () => {
      if (messageHandler && interfaceRef) {
        interfaceRef.removeEventListener("message", messageHandler)
        interfaceRef.onmessage = null
      }
    }

    messageHandler = event => {
      const msg = event.data

      if (typeof msg !== "string") {
        cleanup()
        reject(new Error("Received unexpected message type"))
        return
      }

      if (msg.startsWith("Failed!")) {
        cleanup()
        reject(new Error(msg))
        return
      }

      try {
        const raw = JSON.parse(msg)
        cleanup()

        resolve({
          size: raw.size,
          lastModified: raw.lastModified,
          isFile: () => raw.isFile,
          isDirectory: () => raw.isDirectory,
          isSymbolicLink: () => raw.isSymbolicLink
        })
      } catch {
        cleanup()
        reject(new Error("Failed to parse stat response"))
      }
    }

    interfaceRef.addEventListener("message", messageHandler)

    try {
      interfaceRef.postMessage(path)
    } catch (error) {
      cleanup()
      reject(new Error(`Failed to send message to FileSystemStat: ${'$'}{error}`))
    }
  })
};""".trimIndent().iife)
        super.onPageStarted(view, url, favicon)
    }
}