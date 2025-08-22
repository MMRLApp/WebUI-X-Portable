package com.dergoogler.mmrl.modconf

import androidx.compose.runtime.Composable
import com.dergoogler.mmrl.modconf.model.TargetPackage
import com.dergoogler.mmrl.platform.file.SuFile
import java.io.InputStreamReader
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.Charset
import java.nio.charset.CharsetEncoder
import java.nio.charset.CodingErrorAction
import kotlin.math.ceil

data class ModConfClass<T : ModConfModule>(
    val clazz: Class<T>,
    val initargs: Array<Any>? = null,
    val parameterTypes: Array<Class<*>>? = null,
) {
    data class Instance(
        private val inst: ModConfModule,
    ) {
        val name: String = inst.name
        val description: String = inst.description
        val version: Long = inst.version
        val targetPackages: List<TargetPackage> = inst.targetPackages
        val onDestroy: () -> Unit = inst::onDestroy
        val onStop: () -> Unit = inst::onStop
        val onPause: () -> Unit = inst::onPause
        val onLowMemory: () -> Unit = inst::onLowMemory
        val onPostResume: () -> Unit = inst::onPostResume
        val onResume: () -> Unit = inst::onResume

        @Composable
        fun Content() {
            inst.Content()
        }
    }

    fun createNew(kontext: Kontext): Instance {
        val constructor = if (parameterTypes != null) {
            clazz.getDeclaredConstructor(Kontext::class.java, *parameterTypes)
        } else {
            clazz.getDeclaredConstructor(Kontext::class.java)
        }

        val newInstance = if (initargs != null) {
            constructor.newInstance(kontext, *initargs)
        } else {
            constructor.newInstance(kontext)
        }

        return Instance((newInstance as ModConfModule))
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ModConfClass<*>

        if (clazz != other.clazz) return false
        if (!initargs.contentEquals(other.initargs)) return false
        if (!parameterTypes.contentEquals(other.parameterTypes)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = clazz.hashCode()
        result = 31 * result + (initargs?.contentHashCode() ?: 0)
        result = 31 * result + (parameterTypes?.contentHashCode() ?: 0)
        return result
    }
}

fun SuFile.reader(charset: Charset = Charsets.UTF_8): InputStreamReader =
    newInputStream().reader(charset)

fun SuFile.readText(charset: Charset = Charsets.UTF_8): String =
    reader(charset).use { it.readText() }

fun SuFile.writeText(
    text: String,
    charset: Charset = Charsets.UTF_8,
    append: Boolean = false,
): Unit =
    newOutputStream(append).use { it.writeTextImpl(text, charset) }

internal fun OutputStream.writeTextImpl(text: String, charset: Charset) {
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
        encoder.encode(charBuffer, byteBuffer, endIndex == text.length)
            .also { check(it.isUnderflow) }
        this.write(byteBuffer.array(), 0, byteBuffer.position())

        if (charBuffer.position() != charBuffer.limit()) {
            charBuffer.put(0, charBuffer.get())
            leftover = 1
        } else {
            leftover = 0
        }

        charBuffer.clear()
        byteBuffer.clear()
        startIndex = endIndex
    }
}

internal fun Charset.newReplaceEncoder() = newEncoder()
    .onMalformedInput(CodingErrorAction.REPLACE)
    .onUnmappableCharacter(CodingErrorAction.REPLACE)

internal fun byteBufferForEncoding(chunkSize: Int, encoder: CharsetEncoder): ByteBuffer {
    val maxBytesPerChar = ceil(encoder.maxBytesPerChar()).toInt()
    return ByteBuffer.allocate(chunkSize * maxBytesPerChar)
}