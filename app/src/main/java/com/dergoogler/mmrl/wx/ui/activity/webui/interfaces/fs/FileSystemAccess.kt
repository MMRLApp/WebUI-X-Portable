package com.dergoogler.mmrl.wx.ui.activity.webui.interfaces.fs

import android.R.attr.path
import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.system.OsConstants.F_OK
import androidx.annotation.UiThread
import dev.mmrlx.hybridwebui.HybridWebUI
import dev.mmrlx.hybridwebui.HybridWebUIEvent
import dev.mmrlx.hybridwebui.iife
import com.dergoogler.mmrl.platform.file.SuFile
import com.dergoogler.mmrl.webui.interfaces.WXInterface
import com.dergoogler.mmrl.webui.interfaces.WXOptions
import com.dergoogler.mmrl.wx.util.scrambleClassName
import com.topjohnwu.superuser.internal.UiThreadHandler.handler
import com.topjohnwu.superuser.nio.FileSystemManager
import org.json.JSONObject

class FileSystemAccess(wxOptions: WXOptions) : WXInterface(wxOptions) {
    private val objectName = scrambleClassName<FileSystemAccess>()
    override var name = "__wx--$objectName"

    init {
        view.addEventListener(objectName, FileSystemAccessEvent())
    }

    override fun onDestroy() {
        view.removeEventListener(objectName)
        super.onDestroy()
    }


    class FileSystemAccessEvent : HybridWebUI.EventListener() {
        @UiThread
        @SuppressLint("RequiresFeature")
        override fun listen(view: HybridWebUI, event: HybridWebUIEvent) = with(event) {
            val data: String? = message.data

            if (data == null) {
                reply.postMessage("Failed! Data was null.")
                return
            }

            val file = SuFile(data)

            try {
                val result = JSONObject().apply {
                    put("exists", file.exists())
                    put("canRead", file.canRead())
                    put("canWrite", file.canWrite())
                    put("canExecute", file.canExecute())
                    put("isHidden", file.isHidden)
                }
                reply.postMessage(result.toString())
            } catch (e: Exception) {
                reply.postMessage("Failed! ${e.message}")
            }
        }
    }

    override fun onPageStarted(view: HybridWebUI, url: String, favicon: Bitmap?) {
        runJs(
            """window.fs = window.fs || {};
window.fs.F_OK = 0 // File exists
window.fs.R_OK = 1 // File is readable
window.fs.W_OK = 2 // File is writable
window.fs.X_OK = 4 // File is executable

const interfaceRef = window["$objectName"]
delete window["$objectName"]
delete window["$name"]

window.fs.access = async function(path, mode = window.fs.F_OK) {
  const result = await window.fs.accessInfo(path)

  if (mode === window.fs.F_OK && !result.exists) {
    throw new Error(`ENOENT: no such file or directory, access '${'$'}{path}'`)
  }

  if ((mode & window.fs.R_OK) !== 0 && !result.canRead) {
    throw new Error(`EACCES: permission denied (read), access '${'$'}{path}'`)
  }

  if ((mode & window.fs.W_OK) !== 0 && !result.canWrite) {
    throw new Error(`EACCES: permission denied (write), access '${'$'}{path}'`)
  }

  if ((mode & window.fs.X_OK) !== 0 && !result.canExecute) {
    throw new Error(`EACCES: permission denied (execute), access '${'$'}{path}'`)
  }
}

window.fs.accessInfo = async function(path) {
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
        resolve(raw)
      } catch {
        reject(new Error("Failed to parse access response"))
      }
    }

    interfaceRef.addEventListener("message", handler)

    try {
      interfaceRef.postMessage(path)
    } catch (error) {
      reject(new Error(`Failed to send message to FileSystemAccess: ${'$'}{error}`))
    }
  })
};""".trimIndent().iife
        )
        super.onPageStarted(view, url, favicon)
    }
}