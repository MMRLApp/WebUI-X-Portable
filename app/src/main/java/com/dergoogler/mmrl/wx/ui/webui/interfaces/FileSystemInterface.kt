@file:Suppress("unused")

package com.dergoogler.mmrl.wx.ui.webui.interfaces

import android.os.Build
import android.system.OsConstants
import com.dergoogler.mmrl.wx.ui.webui.sufile
import com.dergoogler.mmrl.wx.ui.webui.util.Permissions
import com.dergoogler.mmrl.wx.ui.webui.util.dangerousAsyncFunction
import com.dergoogler.mmrl.wx.ui.webui.util.dangerousFunction
import com.dergoogler.mmrl.wx.util.PermissionParser
import dev.mmrlx.nio.inputStream
import dev.mmrlx.nio.writeText
import dev.mmrlx.utilities.json.getAs
import dev.mmrlx.webui.JavaScriptFunctionScope
import dev.mmrlx.webui.JavaScriptRegistry
import org.json.JSONObject
import java.nio.charset.Charset

fun JavaScriptRegistry.fileSystemInterface() = namespace("fs") {
    property("constants", constants())

    dangerousAsyncFunction("inputstream", Permissions.MX.IO) {
        val options = optObject(1)
        val path = checkString(0)
        val flags = options.getAs<Int>("flags", OsConstants.O_RDONLY)
        val mode = PermissionParser.parse(options?.opt("mode") ?: 0)

        val stream = inputStream(path, flags, mode)
        stream.toJSObject()
    }

    dangerousAsyncFunction("outputstream", Permissions.MX.IO) {
        val path = checkString(0)
        val overwrite = optBoolean(1, false)
        val fos = outputStream(path, overwrite)
        fos.toJSObject()
    }

    dangerousAsyncFunction("readFile", Permissions.MX.IO) {
        val path = checkString(0)
        val options = optObject(1)
        val charset = options.charset()
        val flags = options.getAs<Int>("flags", OsConstants.O_RDONLY)
        val mode = PermissionParser.parse(options?.opt("mode") ?: 0)
        sufile(path).inputStream(flags, mode).reader(charset).use { it.readText() }
    }

    dangerousAsyncFunction("readFileSync", Permissions.MX.IO) {
        val path = checkString(0)
        val options = optObject(1)
        val charset = options.charset()
        val flags = options.getAs<Int>("flags", OsConstants.O_RDONLY)
        val mode = PermissionParser.parse(options?.opt("mode") ?: 0)
        sufile(path).inputStream(flags, mode).reader(charset).use { it.readText() }
    }

    dangerousAsyncFunction("writeFile", Permissions.MX.IO) {
        val path = checkString(0)
        val data = checkString(1)
        val options = optObject(2)
        val charset = options.charset()
        val flags = options.getAs<Int>(
            "flags", OsConstants.O_CREAT or OsConstants.O_WRONLY or OsConstants.O_TRUNC
        )
        val mode = PermissionParser.parse(options?.opt("mode") ?: 438)
        sufile(path).writeText(data, charset, flags, mode)
    }

    dangerousAsyncFunction("access", Permissions.MX.IO) {
        val path = checkString(0)
        val mode = checkAccessMode(1)
        sufile(path).access(mode)
    }

    dangerousFunction("accessSync", Permissions.MX.IO) {
        val path = checkString(0)
        val mode = checkAccessMode(1)
        sufile(path).access(mode)
    }
}

private fun JavaScriptFunctionScope.checkAccessMode(index: Int): Int {
    val mode = checkInt(index)
    require(mode == OsConstants.W_OK || mode == OsConstants.R_OK || mode == OsConstants.X_OK) {
        "Invalid access mode"
    }
    return mode
}

private fun JSONObject?.charset(): Charset = try {
    Charset.forName(getAs<String>("encoding", "UTF-8"))
} catch (e: Exception) {
    throw IllegalArgumentException("Invalid charset")
}

private fun constants() = JSONObject().apply {
    // O_DIRECT is hidden, sadly
    put("O_EXCL", OsConstants.O_EXCL)
    put("O_NOCTTY", OsConstants.O_NOCTTY)
    put("O_NOFOLLOW", OsConstants.O_NOFOLLOW)
    put("O_NONBLOCK", OsConstants.O_NONBLOCK)
    put("O_RDONLY", OsConstants.O_RDONLY)
    put("O_RDWR", OsConstants.O_RDWR)
    put("O_SYNC", OsConstants.O_SYNC)
    put(
        "O_DSYNC",
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) OsConstants.O_DSYNC else 0
    )
    put("O_TRUNC", OsConstants.O_TRUNC)
    put("O_WRONLY", OsConstants.O_WRONLY)
    put("O_ACCMODE", OsConstants.O_ACCMODE)
    put("O_APPEND", OsConstants.O_APPEND)
    put(
        "O_CLOEXEC",
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) OsConstants.O_CLOEXEC else 0
    )
    put("O_CREAT", OsConstants.O_CREAT)
    put("W_OK", OsConstants.W_OK)
    put("R_OK", OsConstants.R_OK)
    put("X_OK", OsConstants.X_OK)
    put("F_OK", OsConstants.F_OK)
}

// writeNIOText, newReplaceEncoder, byteBufferForEncoding and writeTextImpl:
// keep exactly as they were, just as private top-level functions (they don't depend on the class).