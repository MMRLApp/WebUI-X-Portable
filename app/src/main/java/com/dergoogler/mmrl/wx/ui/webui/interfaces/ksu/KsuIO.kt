@file:Suppress("unused")

package com.dergoogler.mmrl.wx.ui.webui.interfaces.ksu

import android.util.Base64
import com.dergoogler.mmrl.wx.ui.webui.randomAccessFile
import com.dergoogler.mmrl.wx.ui.webui.sufile
import dev.mmrlx.nio.SuFile
import dev.mmrlx.nio.SuFileOutputStream
import dev.mmrlx.nio.SuRandomAccessFile
import dev.mmrlx.nio.inputStream
import dev.mmrlx.webui.JavaScriptFunctionScope
import dev.mmrlx.webui.JavaScriptScope
import dev.mmrlx.webui.WebUI
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

const val TAG = "KsuIO"

/** Open streams/files of one `ksu.io()` user. Everything is closed when the WebUI is destroyed. */
class KsuIO(webui: WebUI) : WebUI by webui {
    internal val inputs = ConcurrentHashMap<String, ManagedInputStream>()
    internal val outputs = ConcurrentHashMap<String, OutputStream>()
    internal val randomFiles = ConcurrentHashMap<String, SuRandomAccessFile>()

    init {
        onDestroy {
            closeAll(inputs) { it.close() }
            closeAll(outputs) { it.close() }
            closeAll(randomFiles) { it.close() }
        }
    }

    private fun <T> closeAll(map: ConcurrentHashMap<String, T>, close: (T) -> Unit) {
        map.forEach { (id, item) ->
            runCatching { synchronized(item as Any) { close(item) } }
                .onFailure { console.error("closeAll failed for $id: $it") }
        }
        map.clear()
    }
}

/** Registers `File()`, `FileInputStream()`, `FileOutputStream()` and `RandomAccessFile()`. */
fun JavaScriptScope.ksuIO(io: KsuIO) {
    function("File") { newObject { fileInterface(io, checkString(0)) } }
    function("FileInputStream") { newObject { fileInputStreamInterface(io) } }
    function("FileOutputStream") { newObject { fileOutputStreamInterface(io) } }
    function("RandomAccessFile") { newObject { randomAccessFileInterface(io) } }
}

private inline fun <T> JavaScriptFunctionScope.guard(what: String, default: T, block: () -> T): T =
    runCatching(block).onFailure { console.error("$what failed $it") }.getOrDefault(default)

internal class ManagedInputStream(
    inputStream: InputStream,
    private val defaultChunkSize: Int = DEFAULT_CHUNK_SIZE,
    private val maxChunkSize: Int = MAX_CHUNK_SIZE,
) : AutoCloseable {
    private val stream = BufferedInputStream(inputStream, maxChunkSize)
    private var buffer = ByteArray(defaultChunkSize)

    @Synchronized
    fun readInto(maxBytes: Int = defaultChunkSize): Int {
        val requestedSize = maxBytes.coerceIn(1, maxChunkSize)
        if (buffer.size < requestedSize) buffer = ByteArray(requestedSize)
        return stream.read(buffer, 0, requestedSize)
    }

    @Synchronized
    fun buffer(): ByteArray = buffer

    @Synchronized
    fun available(): Int = stream.available()

    @Synchronized
    override fun close() = stream.close()

    companion object {
        const val DEFAULT_CHUNK_SIZE = 8 * 1024
        const val MAX_CHUNK_SIZE = 64 * 1024
    }
}

private fun JavaScriptScope.fileInputStreamInterface(io: KsuIO) {
    function("open") {
        val path = checkString(0)
        guard("FileInputStream open", "") {
            val id = UUID.randomUUID().toString()
            io.inputs[id] = ManagedInputStream(inputStream(path))
            id
        }
    }

    // read(id) | read(id, maxBytes)
    function("read") {
        val id = checkString(0)
        val max = if (isNull(1)) null else checkInt(1)
        val stream = io.inputs[id] ?: return@function ""

        guard("FileInputStream read", "") {
            val bytesRead = if (max == null) stream.readInto() else stream.readInto(max)
            if (bytesRead > 0) Base64.encodeToString(
                stream.buffer(),
                0,
                bytesRead,
                Base64.NO_WRAP
            ) else ""
        }
    }

    function("available") {
        val stream = io.inputs[checkString(0)] ?: return@function 0
        runCatching { stream.available() }.getOrDefault(0)
    }

    function("close") {
        val stream = io.inputs.remove(checkString(0)) ?: return@function false
        guard("FileInputStream close", false) { stream.close(); true }
    }
}

private fun JavaScriptScope.fileOutputStreamInterface(io: KsuIO) {
    // open(path) | open(path, append)
    function("open") {
        val path = checkString(0)
        val append = optBoolean(1, false)
        guard("FileOutputStream open", "") {
            val id = UUID.randomUUID().toString()
            io.outputs[id] = BufferedOutputStream(outputStream(file(path), append), 64 * 1024)
            id
        }
    }

    fun write(name: String, block: JavaScriptFunctionScope.(OutputStream) -> Unit) =
        function(name) {
            val bos = io.outputs[checkString(0)] ?: return@function false
            guard("FileOutputStream $name", false) {
                synchronized(bos) { block(bos) }
                true
            }
        }

    write("writeByte") { it.write(checkInt(1)) }
    write("write") { it.write(Base64.decode(checkString(1), Base64.NO_WRAP)) }
    write("flush") { it.flush() }

    function("close") {
        val bos = io.outputs.remove(checkString(0)) ?: return@function false
        guard("FileOutputStream close", false) { synchronized(bos) { bos.close() }; true }
    }
}

private fun JavaScriptScope.randomAccessFileInterface(io: KsuIO) {
    function("open") {
        val path = checkString(0)
        val mode = checkString(1)
        guard("RandomAccessFile open", "") {
            val id = UUID.randomUUID().toString()
            io.randomFiles[id] = randomAccessFile(path, mode)
            id
        }
    }

    /** Defines a function whose first argument is the file id; unknown ids return [default]. */
    fun <T> op(
        name: String,
        default: T,
        block: JavaScriptFunctionScope.(SuRandomAccessFile) -> T,
    ) = function(name) {
        val raf = io.randomFiles[checkString(0)] ?: return@function default
        guard("RandomAccessFile $name", default) { synchronized(raf) { block(raf) } }
    }

    op("read", -1) { it.read() }
    op("readBytes", "") {
        val buffer = ByteArray(checkInt(1))
        val n = it.read(buffer)
        if (n > 0) Base64.encodeToString(buffer, 0, n, Base64.NO_WRAP) else ""
    }
    op("readBoolean", false) { it.readBoolean() }
    op("readByte", 0.toByte()) { it.readByte() }
    op("readInt", 0) { it.readInt() }
    op("readLong", 0L) { it.readLong() }
    op("readShort", 0.toShort()) { it.readShort() }
    op("readFloat", 0f) { it.readFloat() }
    op("readDouble", 0.0) { it.readDouble() }
    op("readUTF", "") { it.readUTF() }
    op<String?>("readLine", null) { it.readLine() }

    op("write", Unit) { it.write(checkInt(1)) }
    op("writeBase64", Unit) { it.write(Base64.decode(checkString(1), Base64.NO_WRAP)) }
    op("writeBoolean", Unit) { it.writeBoolean(checkBoolean(1)) }
    op("writeByte", Unit) { it.writeByte(checkInt(1)) }
    op("writeInt", Unit) { it.writeInt(checkInt(1)) }
    op("writeLong", Unit) { it.writeLong(checkLong(1)) }
    op("writeShort", Unit) { it.writeShort(checkInt(1)) }
    op("writeFloat", Unit) { it.writeFloat(checkDouble(1).toFloat()) }
    op("writeDouble", Unit) { it.writeDouble(checkDouble(1)) }
    op("writeUTF", Unit) { it.writeUTF(checkString(1)) }

    op("seek", false) { it.seek(checkLong(1)); true }
    op("getFilePointer", -1L) { it.getFilePointer() }
    op("length", -1L) { it.length() }
    op("setLength", false) { it.setLength(checkLong(1)); true }

    function("close") {
        val raf = io.randomFiles.remove(checkString(0)) ?: return@function false
        guard("RandomAccessFile close", false) { synchronized(raf) { raf.close() }; true }
    }
}

private fun JavaScriptScope.fileInterface(io: KsuIO, path: String) {
    val suFile: SuFile = sufile(path)

    fun <T> query(name: String, default: T, block: JavaScriptFunctionScope.(SuFile) -> T) =
        function(name) { runCatching { block(suFile) }.getOrDefault(default) }

    query("exists", false) { it.exists() }
    query("isFile", false) { it.isFile }
    query("isDirectory", false) { it.isDirectory }
    query("canRead", false) { it.canRead() }
    query("canWrite", false) { it.canWrite() }
    query("canExecute", false) { it.canExecute() }
    query("createNewFile", false) { it.createNewFile() }
    query("delete", false) { it.delete() }
    query("mkdir", false) { it.mkdir() }
    query("mkdirs", false) { it.mkdirs() }
    query("renameTo", false) { it.renameTo(SuFile(checkString(0))) }
    query("list", emptyArray<String>()) { it.list() ?: emptyArray() }
    query("listFiles", emptyArray<String>()) {
        it.listFiles()?.map { f -> f.absolutePath }?.toTypedArray() ?: emptyArray()
    }
    query("length", -1L) { it.length() }
    query("lastModified", -1L) { it.lastModified() }
    query("setLastModified", false) { it.setLastModified(checkLong(0)) }
    query("getAbsolutePath", path) { it.absolutePath }
    query("getCanonicalPath", path) { it.canonicalPath }
    query<String?>("getParent", null) { it.parent }
    query("getPath", path) { path }
    query("getName", "") { it.name }
    query("isHidden", false) { it.isHidden }
    query("isBlock", false) { it.isBlock() }
    query("isCharacter", false) { it.isCharacter() }
    query("isSymlink", false) { it.isSymlink() }
    query("setReadOnly", false) { it.setReadOnly() }
    query("setReadable", false) { it.setReadable(checkBoolean(0), optBoolean(1, true)) }
    query("setWritable", false) { it.setWritable(checkBoolean(0), optBoolean(1, true)) }
    query("setExecutable", false) { it.setExecutable(checkBoolean(0), optBoolean(1, true)) }
    query("getFreeSpace", -1L) { it.freeSpace }
    query("getTotalSpace", -1L) { it.totalSpace }
    query("getUsableSpace", -1L) { it.usableSpace }

    // Not implemented (kept so old callers still get `false`)
    listOf("deleteRecursive", "createNewSymlink", "createNewLink", "clear").forEach { name ->
        function(name) { false }
    }

    function("newInputStream") {
        guard("newInputStream", "") {
            val id = UUID.randomUUID().toString()
            io.inputs[id] = ManagedInputStream(suFile.inputStream())
            id
        }
    }

    // newOutputStream() | newOutputStream(append)
    function("newOutputStream") {
        val append = optBoolean(0, false)
        guard("newOutputStream", "") {
            val id = UUID.randomUUID().toString()
            io.outputs[id] =
                BufferedOutputStream(
                    SuFileOutputStream(suFile, append),
                    ManagedInputStream.MAX_CHUNK_SIZE
                )
            id
        }
    }
}
