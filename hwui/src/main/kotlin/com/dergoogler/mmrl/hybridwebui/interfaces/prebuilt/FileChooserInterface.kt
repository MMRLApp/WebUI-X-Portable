package com.dergoogler.mmrl.hybridwebui.interfaces.prebuilt

import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.webkit.WebMessageCompat
import com.dergoogler.mmrl.hybridwebui.HybridWebUI
import com.dergoogler.mmrl.hybridwebui.HybridWebUIEvent
import com.dergoogler.mmrl.hybridwebui.iife
import com.dergoogler.mmrl.hybridwebui.interfaces.JavaScriptInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.BufferUnderflowException

class FileChooserInterface(
    activity: ComponentActivity,
    view: HybridWebUI
) : JavaScriptInterface(activity, view) {

    override var name = "__hw--${this.hashCode()}"
    private val objectName = "__hw_e--${this.hashCode() * 64}"

    private companion object {
        const val FILE_CHOOSER_OBJECT_NAME = "SaveFileLauncher"
        const val CHUNKS_WRITTEN = "CHUNK_WRITTEN"
        const val TAG = "FileChooserInterface"
    }

    private var pendingSaveData: ByteArray? = null
    private var filePathCallback: ValueCallback<Array<Uri>>? = null

    private val saveFileLauncher = activity.activityResultRegistry.register(
        "save_file_launcher_${this.hashCode()}",
        ActivityResultContracts.CreateDocument("*/*")
    ) { uri ->
        val data = pendingSaveData
        pendingSaveData = null

        if (uri != null && data != null) {
            activity.lifecycleScope.launch(Dispatchers.IO) {
                runCatching {
                    activity.contentResolver.openOutputStream(uri)?.use { it.write(data) }
                }.onFailure { e ->
                    Log.e(TAG, "Failed to save file to $uri", e)
                }
            }
        }
    }

    private val fileChooserLauncher = activity.activityResultRegistry.register(
        "file_chooser_launcher_${this.hashCode()}",
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uris: Array<Uri>? = if (result.resultCode == RESULT_OK) {
            val data = result.data
            val clipData = data?.clipData

            if (clipData != null) {
                Array(clipData.itemCount) { i -> clipData.getItemAt(i).uri }
            } else {
                data?.data?.let { arrayOf(it) }
            }
        } else null

        filePathCallback?.onReceiveValue(uris)
        filePathCallback = null
    }

    init {
        view.addEventListener(objectName, SaveFileLauncherEvent())
    }

    override fun onShowFileChooser(
        webView: HybridWebUI,
        filePathCallback: ValueCallback<Array<Uri>>,
        fileChooserParams: WebChromeClient.FileChooserParams
    ): Boolean {
        this.filePathCallback?.onReceiveValue(null)
        this.filePathCallback = filePathCallback

        val intent = fileChooserParams.createIntent()
        if (fileChooserParams.mode == WebChromeClient.FileChooserParams.MODE_OPEN_MULTIPLE) {
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }

        return try {
            fileChooserLauncher.launch(intent)
            true
        } catch (_: ActivityNotFoundException) {
            this.filePathCallback?.onReceiveValue(null)
            this.filePathCallback = null
            false
        }
    }

    private inner class SaveFileLauncherEvent : HybridWebUI.EventListener() {
        private var currentFileName: String? = null
        private var mimeType: String? = null
        private var buffer: ByteArrayOutputStream? = null
        private var metadataDecoded: Boolean = false

        @SuppressLint("RequiresFeature")
        override fun listen(view: HybridWebUI, event: HybridWebUIEvent) = with(event) {
            when (message.type) {
                WebMessageCompat.TYPE_ARRAY_BUFFER -> {
                    val bytes = message.arrayBuffer
                    try {
                        if (!metadataDecoded) {
                            val bufferWrap = ByteBuffer.wrap(bytes)

                            // Check if header exists (at least 8 bytes for two Ints)
                            if (bytes.size < 8) throw BufferUnderflowException()

                            val nameLength = bufferWrap.int
                            if (bufferWrap.remaining() < nameLength) throw BufferUnderflowException()
                            val nameBytes = ByteArray(nameLength)
                            bufferWrap.get(nameBytes)
                            currentFileName = String(nameBytes, Charsets.UTF_8)

                            val typeLength = bufferWrap.int
                            if (bufferWrap.remaining() < typeLength) throw BufferUnderflowException()
                            val typeBytes = ByteArray(typeLength)
                            bufferWrap.get(typeBytes)
                            mimeType = String(typeBytes, Charsets.UTF_8)

                            buffer = ByteArrayOutputStream()
                            val remaining = ByteArray(bufferWrap.remaining())
                            bufferWrap.get(remaining)
                            buffer?.write(remaining)

                            metadataDecoded = true
                        } else {
                            buffer?.write(bytes)
                        }
                        reply.postMessage("$CHUNKS_WRITTEN:${bytes.size}")
                    } catch (e: Exception) {
                        Log.e(TAG, "Buffer error", e)
                        reply.postMessage("FAIL_WRITE_ERROR")
                    }
                }

                WebMessageCompat.TYPE_STRING -> {
                    if (message.data == "CLOSE") {
                        val finalData = buffer?.toByteArray()
                        val fileName = currentFileName ?: "downloaded_file"

                        if (finalData != null) {
                            pendingSaveData = finalData
                            saveFileLauncher.launch(fileName)
                        }
                        resetState()
                    }
                }
            }
        }

        private fun resetState() {
            buffer?.close()
            buffer = null
            currentFileName = null
            mimeType = null
            metadataDecoded = false
        }
    }

    override fun onDestroy() {
        saveFileLauncher.unregister()
        fileChooserLauncher.unregister()

        view.removeEventListener(objectName)
        filePathCallback = null
        pendingSaveData = null
        super.onDestroy()
    }

    override fun onPageStarted(view: HybridWebUI, url: String, favicon: Bitmap?) {
        view.runJs("""window.fs = window.fs || {};
const interfaceRef = window["$objectName"]
delete window["$objectName"]
delete window["$name"]

window.fs.newSaveFileStream = function(
  path,
  mimeType,
  chunkSize = 64 * 1024
) {
  let aborted = false;
  let sentMetadata = false;

  const postMessageWithReply = (buffer) =>
    new Promise((resolve, reject) => {
      if (aborted) return reject(new DOMException("Stream aborted", "AbortError"));

      const handler = (event) => {
        interfaceRef?.removeEventListener("message", handler);
        const data = event.data;
        if (typeof data !== "string") return reject(new Error("Invalid response from native"));
        if (data.startsWith("FAIL_")) reject(new Error(data));
        else resolve(data);
      };

      interfaceRef?.addEventListener("message", handler);
      try {
        interfaceRef?.postMessage(buffer);
      } catch (e) {
        interfaceRef?.removeEventListener("message", handler);
        reject(new Error(`Failed to send message: ${'$'}{e}`));
      }
    });

  const encodeMetadataChunk = (name, mime, chunk) => {
    const encoder = new TextEncoder();
    const nameBytes = encoder.encode(name);
    const typeBytes = encoder.encode(mime);
    const contentBytes = chunk ?? new Uint8Array();
    const buffer = new ArrayBuffer(4 + nameBytes.length + 4 + typeBytes.length + contentBytes.byteLength);
    const view = new DataView(buffer);
    let offset = 0;

    view.setUint32(offset, nameBytes.length);
    offset += 4;
    new Uint8Array(buffer, offset, nameBytes.length).set(nameBytes);
    offset += nameBytes.length;

    view.setUint32(offset, typeBytes.length);
    offset += 4;
    new Uint8Array(buffer, offset, typeBytes.length).set(typeBytes);
    offset += typeBytes.length;

    new Uint8Array(buffer, offset).set(contentBytes);
    return buffer;
  };

  return new WritableStream({
    async start() {},

    async write(chunk) {
      if (aborted) throw new DOMException("Stream aborted", "AbortError");
      if (!(chunk instanceof Uint8Array)) throw new TypeError("Chunk must be Uint8Array");

      // Split large chunk into smaller sub-chunks
      for (let offset = 0; offset < chunk.byteLength; offset += chunkSize) {
        const subChunk = chunk.subarray(offset, offset + chunkSize);
        const bufferToSend = !sentMetadata
          ? encodeMetadataChunk(path, mimeType, subChunk)
          : subChunk.buffer;

        sentMetadata = true;
        await postMessageWithReply(bufferToSend);
      }
    },

    async close() {
      if (!aborted) {
        // Signal Kotlin that all chunks are sent
        await postMessageWithReply("CLOSE");
        console.log("SaveFile WritableStream closed");
      }
    },

    abort(reason) {
      aborted = true;
      console.warn("SaveFile WritableStream aborted:", reason);
    },
  });
};""".trimIndent().iife)

        super.onPageStarted(view, url, favicon)
    }
}