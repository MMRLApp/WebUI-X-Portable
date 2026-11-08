@file:Suppress("unused", "PropertyName", "PrivatePropertyName")

package com.dergoogler.mmrl.wx.ui.webui.interfaces.ksu

import android.util.Base64
import com.dergoogler.mmrl.wx.ui.webui.randomAccessFile
import com.dergoogler.mmrl.wx.ui.webui.sufile
import com.dergoogler.mmrl.wx.ui.webui.util.cls
import dev.mmrlx.nio.SuFile
import dev.mmrlx.nio.SuFileOutputStream
import dev.mmrlx.nio.SuRandomAccessFile
import dev.mmrlx.nio.inputStream
import dev.mmrlx.webui.ConstructorArgument
import dev.mmrlx.webui.ConstructorArgumentType
import dev.mmrlx.webui.JavaScriptClassFunctionScope
import dev.mmrlx.webui.JavaScriptClassScope
import dev.mmrlx.webui.JavaScriptFunctionScope
import dev.mmrlx.webui.JavaScriptScope
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

const val TAG = "KsuIO"

/** Open streams/files of one `ksu.io()` user. Everything is closed when the WebUI is destroyed. */
class KsuIO(scope: JavaScriptScope) : JavaScriptScope by scope {
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

    val IoClass = cls("IO") {
        method("version") {
            1
        }

        method("File") {
            val path = checkString(0)
            File(path)
        }

        method("FileInputStream") { InputStream() }
        method("FileOutputStream") { OutputStream() }
        method("RandomAccessFile") { RandomAccessFile() }
    }

    private val File = cls("File") {
        fileInterface(this@KsuIO)
    }

    private val InputStream = cls("FileInputStream") {
        fileInputStreamInterface(this@KsuIO)
    }

    private val OutputStream = cls("FileOutputStream") {
        fileOutputStreamInterface(this@KsuIO)
    }

    private val RandomAccessFile = cls("RandomAccessFile") {
        randomAccessFileInterface(this@KsuIO)
    }

    private fun <T> closeAll(map: ConcurrentHashMap<String, T & Any>, close: (T) -> Unit) {
        map.forEach { (id, item) ->
            runCatching { synchronized(item as Any) { close(item) } }
                .onFailure { console.error("closeAll failed for $id: $it") }
        }
        map.clear()
    }
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

private fun JavaScriptClassScope.fileInputStreamInterface(io: KsuIO) {
    method("open") {
        val path = checkString(0)
        guard("FileInputStream open", "") {
            val id = UUID.randomUUID().toString()
            io.inputs[id] = ManagedInputStream(inputStream(path))
            id
        }
    }

    // read(id) | read(id, maxBytes)
    method("read") {
        val id = checkString(0)
        val max = if (isNull(1)) null else checkInt(1)
        val stream = io.inputs[id] ?: return@method ""

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

    method("available") {
        val stream = io.inputs[checkString(0)] ?: return@method 0
        runCatching { stream.available() }.getOrDefault(0)
    }

    method("close") {
        val stream = io.inputs.remove(checkString(0)) ?: return@method false
        guard("FileInputStream close", false) { stream.close(); true }
    }
}

private fun JavaScriptClassScope.fileOutputStreamInterface(io: KsuIO) {
    // open(path) | open(path, append)
    method("open") {
        val path = checkString(0)
        val append = optBoolean(1, false)
        guard("FileOutputStream open", "") {
            val id = UUID.randomUUID().toString()
            io.outputs[id] = BufferedOutputStream(outputStream(file(path), append), 64 * 1024)
            id
        }
    }

    fun write(name: String, block: JavaScriptFunctionScope.(OutputStream) -> Unit) =
        method(name) {
            val bos = io.outputs[checkString(0)] ?: return@method false
            guard("FileOutputStream $name", false) {
                synchronized(bos) { block(bos) }
                true
            }
        }

    write("writeByte") { it.write(checkInt(1)) }
    write("write") { it.write(Base64.decode(checkString(1), Base64.NO_WRAP)) }
    write("flush") { it.flush() }

    method("close") {
        val bos = io.outputs.remove(checkString(0)) ?: return@method false
        guard("FileOutputStream close", false) { synchronized(bos) { bos.close() }; true }
    }
}

private fun JavaScriptClassScope.randomAccessFileInterface(io: KsuIO) {
    method("open") {
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
    ) = method(name) {
        val raf = io.randomFiles[checkString(0)] ?: return@method default
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

    method("close") {
        val raf = io.randomFiles.remove(checkString(0)) ?: return@method false
        guard("RandomAccessFile close", false) { synchronized(raf) { raf.close() }; true }
    }
}

private fun JavaScriptClassFunctionScope.getPathVar(): String? {
    val path = getVariable("path")
    if (path !is String) {
        console.error("path is not a string: $path")
        return null
    }
    return path
}

private fun JavaScriptClassScope.fileInterface(io: KsuIO) {
    constructor(
        ConstructorArgument("path", ConstructorArgumentType.STRING)
    ) {
        val path = argument("path").getString()
        setVariable("path", path)
    }

    fun <T> query(name: String, default: T, block: JavaScriptClassFunctionScope.(SuFile) -> T) =
        method(name) {
            runCatching {
                val path = getPathVar() ?: return@runCatching default
                block(sufile(path))
            }.getOrDefault(default)
        }

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
    query("getAbsolutePath", null) { it.absolutePath }
    query("getCanonicalPath", null) { it.canonicalPath }
    query<String?>("getParent", null) { it.parent }
    query("getPath", null) { getVariable("path") }
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
        method(name) { false }
    }

    method("newInputStream") {
        guard("newInputStream", "") {
            val path = getPathVar() ?: return@guard ""

            val id = UUID.randomUUID().toString()
            io.inputs[id] = ManagedInputStream(sufile(path).inputStream())
            id
        }
    }

    // newOutputStream() | newOutputStream(append)
    method("newOutputStream") {
        val path = getPathVar() ?: return@method ""
        val append = optBoolean(0, false)
        guard("newOutputStream", "") {
            val id = UUID.randomUUID().toString()
            io.outputs[id] =
                BufferedOutputStream(
                    SuFileOutputStream(sufile(path), append),
                    ManagedInputStream.MAX_CHUNK_SIZE
                )
            id
        }
    }
}
