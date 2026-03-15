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
import org.json.JSONArray
import org.json.JSONObject

class FileSystemReadDirectory(wxOptions: WXOptions) : WXInterface(wxOptions) {
    private val objectName = scrambleClassName<FileSystemReadDirectory>()
    override var name = "__wx--$objectName"

    init {
        view.addEventListener(objectName, FileSystemReadDirectoryEvent())
    }

    override fun onDestroy() {
        view.removeEventListener(objectName)
        super.onDestroy()
    }


    private class FileSystemReadDirectoryEvent: HybridWebUI.EventListener() {
        @UiThread
        @SuppressLint("RequiresFeature")
        override fun listen(view: HybridWebUI, event: HybridWebUIEvent) = with(event) {
            val data: String? = message.data

            if (data == null) {
                reply.postMessage("Failed! Data was null.")
                return
            }

            val dir = SuFile(data)

            if (!dir.exists()) {
                reply.postMessage("Failed! Directory does not exist.")
                return
            }

            if (!dir.isDirectory) {
                reply.postMessage("Failed! Path is not a directory.")
                return
            }

            try {
                val files = dir.listFiles()

                if (files == null) {
                    reply.postMessage("Failed! Could not list directory contents.")
                    return
                }

                val result = JSONArray()

                for (file in files) {
                    val entry = JSONObject().apply {
                        put("name", file.name)
                        put("path", file.absolutePath)
                        put("isFile", file.isFile)
                        put("isDirectory", file.isDirectory)
                        put("isSymbolicLink", file.canonicalPath != file.absolutePath)
                        put("size", file.length())
                        put("lastModified", file.lastModified())
                    }
                    result.put(entry)
                }

                reply.postMessage(result.toString())
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
            
window.fs.readdir = async function(path) {
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

  return new Promise((resolve, reject) => {
    const handler = event => {
      const msg = event.data

      if (typeof msg !== "string") {
        reject(new Error("Received unexpected message type"))
        return
      }

      if (msg.startsWith("Failed!")) {
        reject(new Error(msg))
        return
      }

      try {
        const raw = JSON.parse(msg)

        resolve(
          raw.map(entry => ({
            name: entry.name,
            path: entry.path,
            size: entry.size,
            lastModified: entry.lastModified,
            isFile: () => entry.isFile,
            isDirectory: () => entry.isDirectory,
            isSymbolicLink: () => entry.isSymbolicLink
          }))
        )
      } catch {
        reject(new Error("Failed to parse readdir response"))
      }
    }

    interfaceRef.addEventListener("message", handler, {
      once: true
    })

    try {
      interfaceRef.postMessage(path)
    } catch (error) {
      reject(new Error(`Failed to send message to FileSystemReadDirectory: ${'$'}{error}`))
    }
  })
};""".trimIndent().iife)
        super.onPageStarted(view, url, favicon)
    }
}