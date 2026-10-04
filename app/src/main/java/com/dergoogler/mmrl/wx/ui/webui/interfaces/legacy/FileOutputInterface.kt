package com.dergoogler.mmrl.wx.ui.webui.interfaces.legacy

import com.dergoogler.mmrl.wx.ui.webui.module
import com.dergoogler.mmrl.wx.ui.webui.sanitizedIdWithFileOutputStream
import com.dergoogler.mmrl.wx.ui.webui.util.Permissions
import com.dergoogler.mmrl.wx.ui.webui.util.requirePermission
import dev.mmrlx.webui.JavaScriptRegistry
import java.io.BufferedOutputStream

/** Legacy file output stream interface. `open` returns a stream handle, or `null` on failure. */
fun JavaScriptRegistry.legacyFileOutputInterface() = namespace(module.sanitizedIdWithFileOutputStream) {
    function("open") {
        val path = checkString(0)
        val append = optBoolean(1, false)

        requirePermission(Permissions.WX.IO, "open") {
            try {
                val stream = BufferedOutputStream(outputStream(path, append))

                newObject {
                    function("write") {
                        runCatching { stream.write(checkInt(0)) }
                            .onFailure { console.error("Failed to write byte", it) }
                    }
                    function("flush") {
                        runCatching { stream.flush() }
                            .onFailure { console.error("Failed to flush stream", it) }
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