package com.dergoogler.mmrl.hybridwebui.event

import android.annotation.SuppressLint
import androidx.annotation.UiThread
import androidx.webkit.WebMessageCompat
import com.dergoogler.mmrl.hybridwebui.HybridWebUI
import com.dergoogler.mmrl.hybridwebui.HybridWebUIEvent
import com.dergoogler.mmrl.hybridwebui.HybridWebUIState.onSaveFileRequest
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

internal class SaveFileLauncherEvent : HybridWebUI.EventListener() {

    companion object {
        private const val CHUNK_WRITTEN = "CHUNK_WRITTEN"
    }

    private var currentPath: String? = null
    private var mimeType: String? = null
    private var buffer: ByteArrayOutputStream? = null
    private var metadataDecoded: Boolean = false

    @UiThread
    @SuppressLint("RequiresFeature")
    override fun listen(view: HybridWebUI, event: HybridWebUIEvent) = with(event) {
        when (message.type) {
            WebMessageCompat.TYPE_ARRAY_BUFFER -> {
                val bytes = message.arrayBuffer ?: run {
                    reply.postMessage("FAIL_NO_ARRAYBUFFER")
                    return
                }

                try {
                    if (!metadataDecoded) {
                        val bufferWrap = ByteBuffer.wrap(bytes)

                        // Decode filename
                        val nameLength = bufferWrap.int
                        val nameBytes = ByteArray(nameLength)
                        bufferWrap.get(nameBytes)
                        currentPath = String(nameBytes, Charsets.UTF_8)

                        // Decode MIME type
                        val typeLength = bufferWrap.int
                        val typeBytes = ByteArray(typeLength)
                        bufferWrap.get(typeBytes)
                        mimeType = String(typeBytes, Charsets.UTF_8)

                        buffer = ByteArrayOutputStream()

                        // Write remaining bytes
                        val remaining = ByteArray(bufferWrap.remaining())
                        bufferWrap.get(remaining)
                        buffer!!.write(remaining)

                        metadataDecoded = true
                    } else {
                        buffer!!.write(bytes)
                    }

                    reply.postMessage("$CHUNK_WRITTEN:${bytes.size}")
                } catch (e: Exception) {
                    reply.postMessage("FAIL_WRITE_ERROR")
                }
            }

            WebMessageCompat.TYPE_STRING -> {
                // Use this as the "close" signal
                if (message.data == "CLOSE") {
                    try {
                        if (buffer != null && currentPath != null && mimeType != null) {
                            onSaveFileRequest?.invoke(
                                buffer!!.toByteArray(),
                                currentPath!!,
                                mimeType!!
                            )
                        }
                    } finally {
                        buffer?.close()
                        buffer = null
                        currentPath = null
                        mimeType = null
                        metadataDecoded = false
                    }
                } else {
                    reply.postMessage("FAIL_UNSUPPORTED_TYPE")
                }
            }

            else -> {
                reply.postMessage("FAIL_UNSUPPORTED_TYPE")
            }
        }
    }
}