@file:Suppress("unused", "PropertyName")

package com.dergoogler.mmrl.wx.ui.webui.interfaces

import android.os.Build
import android.system.OsConstants
import com.dergoogler.mmrl.platform.file.SuFile
import com.dergoogler.mmrl.platform.file.SuFileOutputStream
import com.dergoogler.mmrl.wx.ui.webui.sufile
import com.dergoogler.mmrl.wx.ui.webui.util.Permissions
import com.dergoogler.mmrl.wx.ui.webui.util.requirePermission
import com.dergoogler.mmrl.wx.util.PermissionParser
import dev.mmrlx.nio.inputStream
import dev.mmrlx.utilities.json.getAs
import dev.mmrlx.utilities.json.toByteArray
import dev.mmrlx.webui.WebUI
import dev.mmrlx.webui.interfaces.prebuilt.WebUIFileSystemInterface
import dev.mmrlx.webui.javascript.annotation.ExportMethod
import dev.mmrlx.webui.javascript.annotation.ExportVariable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.Charset
import java.nio.charset.CharsetEncoder
import java.nio.charset.CodingErrorAction
import kotlin.math.ceil

// TODO: migrate from com.dergoogler.mmrl.platform.file.SuFile to dev.mmrlx.nio.SuFile
// TODO: Not yet implemented, requires file system re-work.
class FileSystemInterface(webui: WebUI) : WebUIFileSystemInterface(webui) {
    // Hidden, sadly
    //    val O_DIRECT: Int = OsConstants.O_DIRECT

    @ExportVariable
    val constants = JSONObject().apply {
        put("O_EXCL", OsConstants.O_EXCL)
        put("O_NOCTTY", OsConstants.O_NOCTTY)
        put("O_NOFOLLOW", OsConstants.O_NOFOLLOW)
        put("O_NONBLOCK", OsConstants.O_NONBLOCK)
        put("O_RDONLY", OsConstants.O_RDONLY)
        put("O_RDWR", OsConstants.O_RDWR)
        put("O_SYNC", OsConstants.O_SYNC)
        put(
            "O_DSYNC", if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                OsConstants.O_DSYNC
            } else {
                0
            }
        )
        put("O_TRUNC", OsConstants.O_TRUNC)
        put("O_WRONLY", OsConstants.O_WRONLY)
        put("O_ACCMODE", OsConstants.O_ACCMODE)
        put("O_APPEND", OsConstants.O_APPEND)
        put(
            "O_CLOEXEC", if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                OsConstants.O_CLOEXEC
            } else {
                0
            }
        )
        put("O_CREAT", OsConstants.O_CREAT)
        put("W_OK", OsConstants.W_OK)
        put("R_OK", OsConstants.R_OK)
        put("X_OK", OsConstants.X_OK)
        put("F_OK", OsConstants.F_OK)

    }

    // TODO: better handle
    private class JSInputStream : JSObject {
        private var stream: InputStream

        constructor(webui: WebUI, path: String, flags: Int, mode: Int) {
            stream = webui.inputStream(path, flags, mode)
        }

        @ExportMethod
        suspend fun read(): Int = withContext(Dispatchers.IO) {
            return@withContext stream.read()
        }

        @ExportMethod
        suspend fun read(b: JSONArray): Int = withContext(Dispatchers.IO) {
            return@withContext stream.read(b.toByteArray())
        }

        @ExportMethod
        suspend fun read(b: JSONArray, off: Int, len: Int): Int =
            withContext(Dispatchers.IO) {
                return@withContext stream.read(b.toByteArray(), off, len)
            }

        @ExportMethod
        suspend fun skip(n: Long): Long = withContext(Dispatchers.IO) {
            return@withContext stream.skip(n)
        }

        @ExportMethod
        fun mark(readLimit: Int) {
            stream.mark(readLimit)
        }

        @ExportMethod
        suspend fun reset() =
            withContext(Dispatchers.IO) {
                stream.reset()
            }

        @ExportMethod
        fun markSupported() = stream.markSupported()

        @ExportMethod
        suspend fun available(): Int = withContext(Dispatchers.IO) {
            stream.available()
        }

        @ExportMethod
        suspend fun close() {
            withContext(Dispatchers.IO) {
                stream.close()
            }
        }
    }

    @ExportMethod
    suspend fun inputstream(path: String, options: JSONObject?): Promise<JSObject?> {
        val flags = options.getAs<Int>("flags", OsConstants.O_RDONLY)
        val rawMode = options?.opt("mode") ?: 0
        val mode = PermissionParser.parse(rawMode)

        return Promise(Dispatchers.Main) {
            try {
                requirePermission(
                    Permissions.MX.IO, "inputstream"
                ) {
                    resolve(JSInputStream(this@FileSystemInterface, path, flags, mode))
                }
            } catch (e: Exception) {
                reject(e)
            }
        }
    }

    @ExportMethod
    suspend fun outputstream(path: String, options: JSONObject): Promise<JSObject> {
        return Promise {
            try {
                requirePermission(
                    Permissions.MX.IO, "outputstream"
                ) {
                    val fos = outputStream(path, false) // overwrite (use true for append)

                    val writer = object : JSObject {
                        @ExportMethod
                        fun write(chunk: JSONArray) {
                            val byteArray = ByteArray(chunk.length()) { i ->
                                chunk.getInt(i).toByte()
                            }
                            fos.write(byteArray)
                        }

                        @ExportMethod
                        fun close() {
                            fos.flush()
                            fos.close()
                        }
                    }

                    resolve(writer)
                }
            } catch (e: Throwable) {
                reject(e)
            }
        }
    }

    @ExportMethod
    suspend fun readFile(
        path: String,
        options: JSONObject?,
    ): Promise<String> {
        val charset: Charset? = Charset.forName(options.getAs<String>("encoding", "UTF-8"))
        val flags = options.getAs<Int>("flags", OsConstants.O_RDONLY)
        val rawMode = options?.opt("mode") ?: 0
        val mode = PermissionParser.parse(rawMode)

        return Promise {
            if (charset == null) {
                reject(Error("Invalid charset"))
                return@Promise
            }

            requirePermission(
                Permissions.MX.IO, "readFile"
            ) {
                try {
                    val file = sufile(path)
                    val stream = file.inputStream(flags, mode)
                    val reader = stream.reader(charset)
                    val text = reader.use { it.readText() }
                    resolve(text)
                } catch (e: Exception) {
                    reject(e)
                }
            }
        }
    }

    @ExportMethod
    fun readFileSync(
        path: String,
        options: JSONObject?,
    ): String? {
        val charset: Charset? = Charset.forName(options.getAs<String>("encoding", "UTF-8"))
        val flags = options.getAs<Int>("flags", OsConstants.O_RDONLY)
        val rawMode = options?.opt("mode") ?: 0
        val mode = PermissionParser.parse(rawMode)

        if (charset == null) {
            console.error(Error("Invalid charset"))
            return null
        }

        return requirePermission(
            Permissions.MX.IO, "readFileSync"
        ) {
            try {
                val file = sufile(path)
                val stream = file.inputStream(flags, mode)
                val reader = stream.reader(charset)
                val text = reader.use { it.readText() }
                return@requirePermission text
            } catch (e: Exception) {
                console.error(e)
                return@requirePermission null
            }
        }
    }

    @ExportMethod
    suspend fun writeFile(
        path: String,
        data: String,
        options: JSONObject?,
    ): Promise<Unit> {
        val charset: Charset? = Charset.forName(options.getAs<String>("encoding", "UTF-8"))
        val flags = options.getAs<Int>(
            "flags",
            OsConstants.O_CREAT or OsConstants.O_WRONLY or OsConstants.O_TRUNC
        )
        val rawMode = options?.opt("mode") ?: 438
        val mode = PermissionParser.parse(rawMode)

        return Promise {
            if (charset == null) {
                reject(Error("Invalid charset"))
                return@Promise
            }
            requirePermission(
                Permissions.MX.IO, "writeFile"
            ) {
                try {
                    SuFile(path).writeNIOText(data, flags, mode, charset)
                    resolve(Unit)
                } catch (e: Exception) {
                    reject(e)
                }
            }
        }
    }
// TODO: implement it with Os.access(path, flags)

    @ExportMethod
    suspend fun access(
        path: String,
        mode: Int,
    ): Promise<Boolean> {
        return Promise {

            if (mode != OsConstants.W_OK && mode != OsConstants.R_OK && mode != OsConstants.X_OK) {
                reject(Error("Invalid access mode"))
                return@Promise
            }

            requirePermission(
                Permissions.MX.IO, "access"
            ) {
                try {
                    resolve(sufile(path).access(mode))
                } catch (e: Exception) {
                    reject(e)
                }
            }
        }
    }

    @ExportMethod
    fun accessSync(
        path: String,
        mode: Int,
    ): Boolean? {
        if (mode != OsConstants.W_OK && mode != OsConstants.R_OK && mode != OsConstants.X_OK) {
            console.error("Invalid access mode")
            return false
        }

        return requirePermission(
            Permissions.MX.IO, "accessSync"
        ) {
            try {
                return@requirePermission sufile(path).access(mode)
            } catch (e: Exception) {
                console.error(e)
                return@requirePermission false
            }
        }
    }

    @Throws(IOException::class)
    private fun SuFile.writeNIOText(
        text: String,
        flags: Int,
        mode: Int,
        charset: Charset = Charsets.UTF_8,
    ): Unit =
        SuFileOutputStream(this, flags, mode).use { it.writeTextImpl(text, charset) }

    private fun Charset.newReplaceEncoder() = newEncoder()
        .onMalformedInput(CodingErrorAction.REPLACE)
        .onUnmappableCharacter(CodingErrorAction.REPLACE)

    private fun byteBufferForEncoding(chunkSize: Int, encoder: CharsetEncoder): ByteBuffer {
        val maxBytesPerChar =
            ceil(encoder.maxBytesPerChar()).toInt() // including replacement sequence
        return ByteBuffer.allocate(chunkSize * maxBytesPerChar)
    }

    private fun OutputStream.writeTextImpl(text: String, charset: Charset) {
        val chunkSize = DEFAULT_BUFFER_SIZE

        if (text.length < 2 * chunkSize) {
            this.write(text.toByteArray(charset))
            return
        }

        val encoder = charset.newReplaceEncoder()
        val charBuffer = CharBuffer.allocate(chunkSize)
        val byteBuffer = byteBufferForEncoding(chunkSize, encoder)

        var startIndex = 0
        var leftover = 0

        while (startIndex < text.length) {
            val copyLength = minOf(chunkSize - leftover, text.length - startIndex)
            val endIndex = startIndex + copyLength

            text.toCharArray(charBuffer.array(), leftover, startIndex, endIndex)
            charBuffer.limit(copyLength + leftover)
            encoder.encode(charBuffer, byteBuffer, /*endOfInput = */endIndex == text.length)
                .also { check(it.isUnderflow) }
            this.write(byteBuffer.array(), 0, byteBuffer.position())

            if (charBuffer.position() != charBuffer.limit()) {
                charBuffer.put(0, charBuffer.get()) // the last char is a high surrogate
                leftover = 1
            } else {
                leftover = 0
            }

            charBuffer.clear()
            byteBuffer.clear()
            startIndex = endIndex
        }
    }

}