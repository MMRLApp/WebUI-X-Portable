package com.dergoogler.mmrl.wx.ui.activity.webui.interfaces.fs

import android.annotation.SuppressLint
import android.graphics.Bitmap
import androidx.annotation.UiThread
import androidx.webkit.WebMessageCompat
import androidx.webkit.WebViewFeature
import com.dergoogler.mmrl.hybridwebui.HybridWebUI
import com.dergoogler.mmrl.hybridwebui.HybridWebUIEvent
import com.dergoogler.mmrl.hybridwebui.iife
import com.dergoogler.mmrl.platform.file.SuFile
import com.dergoogler.mmrl.webui.interfaces.WXInterface
import com.dergoogler.mmrl.webui.interfaces.WXOptions
import com.dergoogler.mmrl.wx.util.scrambleClassName

class FileSystemInputStream(wxOptions: WXOptions) : WXInterface(wxOptions) {
    private val objectName = scrambleClassName<FileSystemOutputStream>()
    override var name = "__wx--$objectName"

    init {
        view.addEventListener(objectName, FileSystemInputStreamEvent())
    }

    override fun onDestroy() {
        view.removeEventListener(objectName)
        super.onDestroy()
    }

    class FileSystemInputStreamEvent : HybridWebUI.EventListener() {
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

            when (message.type) {
                WebMessageCompat.TYPE_STRING -> {
                    if (WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_ARRAY_BUFFER)) {
                        try {
                            val bytes = file.newInputStream().use { it.readBytes() }
                            reply.postMessage(bytes)
                        } catch (e: Exception) {
                            reply.postMessage("Failed! ${e.message}")
                        }
                    } else {
                        reply.postMessage("Failed! WebMessageCompat.TYPE_ARRAY_BUFFER not supported.")
                    }
                }

                else -> {}
            }
        }
    }

    override fun onPageStarted(view: HybridWebUI, url: String, favicon: Bitmap?) {
        runJs("""window.fs = window.fs || {};
let inputStreamQueue = Promise.resolve()
const interfaceRef = window["$objectName"]
delete window["$objectName"]
delete window["$name"]

function enqueueInputStreamRequest(task) {
  const run = inputStreamQueue.then(task, task)

  inputStreamQueue = run.then(
    () => undefined,
    () => undefined
  )

  return run
}

window.fs.newInputStream = async function(path, init = {}) {
  if (typeof path !== "string") {
    throw new TypeError("'path' must be a string")
  }

  if (path.trim() === "") {
    throw new Error("'path' cannot be empty")
  }

  const inputStream =
    typeof window !== "undefined" ? interfaceRef : undefined

  if (!inputStream || typeof inputStream.postMessage !== "function") {
    throw new Error(
      `Unable to find 'window["$objectName"]'. The required filesystem permission may not be granted.`
    )
  }

  const mergedInit = {
    headers: { "Content-Type": "application/octet-stream" },
    ...init
  }

  const { signal, ...responseInit } = mergedInit

  return enqueueInputStreamRequest(
    () =>
      new Promise((resolve, reject) => {
        let settled = false

        const cleanup = () => {
          if (signal) signal.removeEventListener("abort", onAbort)
          inputStream.removeEventListener("message", handler)
        }

        const onAbort = () => {
          if (settled) return
          settled = true
          cleanup()
          reject(new DOMException("The operation was aborted.", "AbortError"))
        }

        if (signal?.aborted) {
          onAbort()
          return
        }

        signal?.addEventListener("abort", onAbort)

        const handler = event => {
          if (settled) return

          settled = true
          const msg = event.data

          if (msg instanceof ArrayBuffer) {
            cleanup()
            resolve(new Response(new Uint8Array(msg), responseInit))
            return
          }

          cleanup()

          if (typeof msg === "string") {
            reject(new Error(msg))
            return
          }

          reject(new Error("Received unexpected message type"))
        }

        inputStream.addEventListener("message", handler)

        try {
          inputStream.postMessage(path)
        } catch (error) {
          if (!settled) {
            settled = true
            cleanup()
            reject(
              new Error(`Failed to send message to FileSystemInputStream: ${'$'}{error}`)
            )
          }
        }
      })
  )
}

window.fs.readTextFile = async function(path, encoding = "utf-8", signal) {
  const response = await window.fs.newInputStream(path, { signal });
  return await response.text();
}
""".trimIndent().iife)
        super.onPageStarted(view, url, favicon)
    }
}