@file:Suppress("unused", "PropertyName")

package com.dergoogler.mmrl.wx.ui.webui.interfaces

import android.os.Build
import android.system.OsConstants
import com.dergoogler.mmrl.platform.file.SuFile
import com.dergoogler.mmrl.platform.file.SuFileOutputStream
import com.dergoogler.mmrl.platform.file.inputStream
import com.dergoogler.mmrl.wx.util.PermissionParser
import dev.mmrlx.utilities.json.getAs
import dev.mmrlx.webui.WebUI
import dev.mmrlx.webui.interfaces.ExportMethod
import dev.mmrlx.webui.interfaces.ExportVariable
import dev.mmrlx.webui.interfaces.JavaScriptInterface
import org.json.JSONObject
import java.io.IOException
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.Charset
import java.nio.charset.CharsetEncoder
import java.nio.charset.CodingErrorAction
import kotlin.math.ceil

// TODO: Not yet implemented, requires file system re-work.
class FileSystemInterface(webui: WebUI) : JavaScriptInterface(webui) {
    override val objectClassName = "FileSystem"
    override val windowObjectName = "fs"

    // Hidden, sadly
    //    val O_DIRECT: Int = OsConstants.O_DIRECT

    // TODO: support pure for variables
    //    @ExportVariable(pure = true)
    @ExportVariable
    val O_EXCL: Int = OsConstants.O_EXCL

    @ExportVariable
    val O_NOCTTY: Int = OsConstants.O_NOCTTY

    @ExportVariable
    val O_NOFOLLOW: Int = OsConstants.O_NOFOLLOW

    @ExportVariable
    val O_NONBLOCK: Int = OsConstants.O_NONBLOCK

    @ExportVariable
    val O_RDONLY: Int = OsConstants.O_RDONLY

    @ExportVariable
    val O_RDWR: Int = OsConstants.O_RDWR

    @ExportVariable
    val O_SYNC: Int = OsConstants.O_SYNC

    @ExportVariable
    val O_DSYNC: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
        OsConstants.O_DSYNC
    } else {
        0
    }

    @ExportVariable
    val O_TRUNC: Int = OsConstants.O_TRUNC

    @ExportVariable
    val O_WRONLY: Int = OsConstants.O_WRONLY

    @ExportVariable
    val O_ACCMODE: Int = OsConstants.O_ACCMODE

    @ExportVariable
    val O_APPEND: Int = OsConstants.O_APPEND

    @ExportVariable
    val O_CLOEXEC: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
        OsConstants.O_CLOEXEC
    } else {
        0
    }

    @ExportVariable
    val O_CREAT: Int = OsConstants.O_CREAT

// TODO: promise callback broken, fix in mx
    @ExportMethod
    suspend fun readFile(
        path: String,
        options: JSONObject?,
    ): Promise<String> {
        val charset: Charset? = Charset.forName(options.getAs<String>("encoding", "UTF-8"))
        val flags = options.getAs<Int>("flags", O_RDONLY)
        val rawMode = options?.opt("mode") ?: 0
        val mode = PermissionParser.parse(rawMode)

        return Promise {
            if (charset == null) {
                reject(Error("Invalid charset"))
                return@Promise
            }

            try {
                val file = SuFile(path)
                val stream = file.inputStream(flags, mode)
                val reader = stream.reader(charset)
                val text = reader.use { it.readText() }
                resolve(text)
            } catch (e: Exception) {
                reject(e)
            }
        }
    }

    @ExportMethod
    fun readFileSync(
        path: String,
        options: JSONObject?,
    ): String? {
        val charset: Charset? = Charset.forName(options.getAs<String>("encoding", "UTF-8"))
        val flags = options.getAs<Int>("flags", O_RDONLY)
        val rawMode = options?.opt("mode") ?: 0
        val mode = PermissionParser.parse(rawMode)

        if (charset == null) {
            console.error(Error("Invalid charset"))
            return null
        }

        try {
            val file = SuFile(path)
            val stream = file.inputStream(flags, mode)
            val reader = stream.reader(charset)
            val text = reader.use { it.readText() }
            return text
        } catch (e: Exception) {
            console.error(e)
            return null
        }
    }

    @ExportMethod
    suspend fun writeFile(
        path: String,
        data: String,
        options: JSONObject?,
    ): Promise<Unit> {
        val charset: Charset? = Charset.forName(options.getAs<String>("encoding", "UTF-8"))
        val flags = options.getAs<Int>("flags", O_CREAT or O_WRONLY or O_TRUNC)
        val rawMode = options?.opt("mode") ?: 438
        val mode = PermissionParser.parse(rawMode)

        return Promise {
            if (charset == null) {
                console.error(Error("Invalid charset"))
                return@Promise
            }

            try {
                SuFile(path).writeNIOText(data,flags, mode, charset)
                resolve(Unit)
            } catch (e: Exception) {
                reject(e)
            }
        }
    }
// TODO: implement it with Os.access(path, flags)
//
//    @ExportMethod
//    suspend fun access(
//        path: String,
//        mode: Int,
//    ): Promise<Boolean> {
//        return Promise {
//            val accessMode = AccessMode.from(mode)
//
//            if (accessMode == null) {
//                reject(Error("Invalid access mode"))
//                return@Promise
//            }
//
//            try {
//                val success = when (accessMode) {
//                    AccessMode.F_OK -> {
//                        SuFile(path).exists()
//                    }
//
//                    AccessMode.R_OK -> {
//                        SuFile(path).canRead()
//                    }
//
//                    AccessMode.W_OK -> {
//                        SuFile(path).canWrite()
//                    }
//
//                    AccessMode.X_OK -> {
//                        SuFile(path).canExecute()
//                    }
//                }
//
//                resolve(success)
//            } catch (e: Exception) {
//                reject(e)
//            }
//        }
//    }

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