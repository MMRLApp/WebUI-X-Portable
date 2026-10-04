package com.dergoogler.mmrl.wx.ui.webui.interfaces.legacy

import com.dergoogler.mmrl.wx.ui.webui.module
import com.dergoogler.mmrl.wx.ui.webui.sanitizedIdWithFileInputStream
import com.dergoogler.mmrl.wx.ui.webui.util.Permissions
import com.dergoogler.mmrl.wx.ui.webui.util.requirePermission
import dev.mmrlx.utilities.json.toJSONArray
import dev.mmrlx.webui.JavaScriptRegistry
import java.io.BufferedInputStream

/** Legacy file input stream interface. `open` returns a stream handle, or `null` on failure. */
fun JavaScriptRegistry.legacyFileInputInterface() = namespace(module.sanitizedIdWithFileInputStream) {
    function("open") {
        val path = checkString(0)

        requirePermission(Permissions.WX.IO, "open") {
            try {
                val stream = BufferedInputStream(inputStream(path))

                newObject {
                    function("read") {
                        try {
                            stream.read()
                        } catch (e: Exception) {
                            console.error("Failed to read byte", e)
                            -1
                        }
                    }

                    function("readChunk") {
                        val buffer = ByteArray(checkInt(0))
                        val bytesRead = stream.read(buffer)
                        if (bytesRead > 0) buffer.copyOf(bytesRead).toJSONArray() else null
                    }

                    function("skip") {
                        try {
                            stream.skip(checkLong(0))
                        } catch (e: Exception) {
                            console.error("Failed to skip bytes", e)
                            -1L
                        }
                    }

                    function("close") {
                        runCatching { stream.close() }
                            .onFailure { console.error("Failed to close stream", it) }
                    }
                }
            } catch (e: Exception) {
                console.error(e)
                null
            }
        }
    }
}