package com.dergoogler.mmrl.wx.ui.activity.webui.interfaces.fs

import android.annotation.SuppressLint
import android.graphics.Bitmap
import androidx.annotation.UiThread
import androidx.webkit.WebMessageCompat
import androidx.webkit.WebViewFeature
import dev.mmrlx.hybridwebui.HybridWebUI
import dev.mmrlx.hybridwebui.HybridWebUIEvent
import dev.mmrlx.hybridwebui.iife
import com.dergoogler.mmrl.platform.file.SuFile
import com.dergoogler.mmrl.platform.file.SuFileInputStream
import com.dergoogler.mmrl.webui.interfaces.WXInterface
import com.dergoogler.mmrl.webui.interfaces.WXOptions
import com.dergoogler.mmrl.wx.util.scrambleClassName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    private inner class FileSystemInputStreamEvent : HybridWebUI.EventListener() {
        @UiThread
        @SuppressLint("RequiresFeature")
        override fun listen(view: HybridWebUI, event: HybridWebUIEvent): Unit = with(event) {
            scope.launch(Dispatchers.IO) {
                val data: String? = message.data

                if (data == null) {
                    withContext(Dispatchers.Main) { reply.postMessage("Failed! Data was null.") }
                    return@launch
                }

                val file = SuFile(data)
                if (!file.exists()) {
                    withContext(Dispatchers.Main) { reply.postMessage("Failed! File does not exist.") }
                    return@launch
                }

                if (message.type == WebMessageCompat.TYPE_STRING) {
                    if (WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_ARRAY_BUFFER)) {
                        try {
                            SuFileInputStream(file).use { fis ->
                                val buffer = ByteArray(64 * 1024) // 64KB chunks
                                var bytesRead: Int
                                while (fis.read(buffer).also { bytesRead = it } != -1) {
                                    val chunk = if (bytesRead == buffer.size) buffer.copyOf() else buffer.copyOfRange(0, bytesRead)
                                    withContext(Dispatchers.Main) {
                                        reply.postMessage(chunk)
                                    }
                                }
                                withContext(Dispatchers.Main) { reply.postMessage("__EOF__") }
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) { reply.postMessage("Failed! ${e.message}") }
                        }
                    } else {
                        withContext(Dispatchers.Main) { reply.postMessage("Failed! WebMessageCompat.TYPE_ARRAY_BUFFER not supported.") }
                    }
                }
            }
        }
    }

    override fun onPageStarted(view: HybridWebUI, url: String, favicon: Bitmap?) {
        runJs(
            """window.fs = window.fs || {};
let inputStreamQueue = Promise.resolve()
const interfaceRef = window["$objectName"]
delete window["$objectName"]
delete window["$name"]

function enqueueInputStreamRequest(task) {
  const run = inputStreamQueue.then(task, task)
  inputStreamQueue = run.then(() => undefined, () => undefined)
  return run
}

window.fs.newInputStream = async function(path, init = {}) {
  if (typeof path !== "string") throw new TypeError("'path' must be a string")
  if (path.trim() === "") throw new Error("'path' cannot be empty")

  const inputStream = typeof window !== "undefined" ? interfaceRef : undefined
  if (!inputStream || typeof inputStream.postMessage !== "function") {
    throw new Error(`Unable to find 'window["$objectName"]'. Permissions may not be granted.`)
  }

  const mergedInit = { headers: { "Content-Type": "application/octet-stream" }, ...init }
  const { signal, ...responseInit } = mergedInit

  return enqueueInputStreamRequest(() => new Promise((resolve, reject) => {
    let settled = false;
    
    // Using ReadableStream to handle chunks efficiently
    const stream = new ReadableStream({
      start(controller) {
        const onAbort = () => {
          if (settled) return; settled = true;
          cleanup();
          controller.error(new DOMException("Aborted", "AbortError"));
          reject(new DOMException("Aborted", "AbortError"));
        };

        const cleanup = () => {
          if (signal) signal.removeEventListener("abort", onAbort);
          inputStream.removeEventListener("message", handler);
        };

        const handler = event => {
          const msg = event.data;
          
          if (msg instanceof ArrayBuffer) {
            controller.enqueue(new Uint8Array(msg));
            if (!settled) {
               settled = true;
               resolve(new Response(stream, responseInit));
            }
            return;
          }

          if (msg === "__EOF__") {
            cleanup();
            controller.close();
            return;
          }

          cleanup();
          const err = new Error(typeof msg === "string" ? msg : "Unexpected message type");
          controller.error(err);
          if (!settled) { settled = true; reject(err); }
        };

        if (signal?.aborted) return onAbort();
        signal?.addEventListener("abort", onAbort);
        inputStream.addEventListener("message", handler);

        try {
          inputStream.postMessage(path);
        } catch (error) {
          cleanup();
          controller.error(error);
          reject(error);
        }
      }
    });
  }));
}

window.fs.readTextFile = async function(path, encoding = "utf-8", signal) {
  const response = await window.fs.newInputStream(path, { signal });
  return await response.text();
}
""".trimIndent().iife
        )
        super.onPageStarted(view, url, favicon)
    }
}