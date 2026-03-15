package com.dergoogler.mmrl.hybridwebui.interfaces.prebuilt

import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.content.ActivityNotFoundException
import android.content.Intent
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

    override var name = " "

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
        view.addEventListener(FILE_CHOOSER_OBJECT_NAME, SaveFileLauncherEvent())
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

        view.removeEventListener(FILE_CHOOSER_OBJECT_NAME)
        filePathCallback = null
        pendingSaveData = null
        super.onDestroy()
    }
}