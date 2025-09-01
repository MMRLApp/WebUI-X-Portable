@file:Suppress("unused")

package com.dergoogler.mmrl.wx.ui.screens.modules.screens

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.dergoogler.mmrl.compat.MediaStoreCompat.getPathForUri
import com.dergoogler.mmrl.platform.file.SuFile
import com.dergoogler.mmrl.platform.file.SuFile.Companion.toSuFile
import java.io.File
import java.io.InputStream
import java.io.OutputStream

val SuFile.parentSuFile: SuFile?
    get() = try {
        val parent = this.parentFile
        if (parent != null && parent.path != this.path) {
            SuFile(parent)
        } else {
            null
        }
    } catch (e: Exception) {
        null
    }

class SuFileInputStream : InputStream {
    private var fis: InputStream

    constructor(path: String) : this(path.toSuFile())
    constructor(file: SuFile) {
        fis = file.newInputStream()
    }

    constructor(file: File) : this(file.path.toSuFile())

    override fun read(): Int = fis.read()
}

class SuFileOutputStream : OutputStream {
    private var ops: OutputStream

    constructor(path: String, append: Boolean = false) : this(path.toSuFile(), append)
    constructor(file: SuFile, append: Boolean = false) {
        ops = file.newOutputStream(append)
    }

    constructor(file: File, append: Boolean = false) : this(file.path, append)

    override fun write(b: Int) = ops.write(b)
}


class SuContentResolver : ContentResolver {
    private val mContext: Context

    internal constructor(context: Context) : super(context) {
        mContext = context
    }

    /**
     * Opens a [SuFileInputStream] for a given [Uri].
     *
     * This function attempts to resolve the a file path from the provided [Uri]
     * using [getPathForUri] and then opens an input stream with superuser privileges
     * if necessary. It is designed to handle URIs that point to files requiring
     * root access.
     *
     * @param uri The [Uri] of the file to open.
     * @return A [SuFileInputStream] for reading the file, or `null` if the [Uri] is null
     *         or its path cannot be resolved.
     * @see SuFileInputStream
     */
    fun openSuInputStream(uri: Uri?): SuFileInputStream? {
        if (uri == null) return null
        val path = mContext.getPathForUri(uri)
        if (path == null) return null
        return SuFileInputStream(path)
    }

    /**
     * Opens a file descriptor to write to the content represented by a content URI,
     * using superuser (root) privileges. This is useful for writing to files that
     * are normally protected by the system.
     *
     * It first resolves the content URI to a real file path and then attempts
     * to open a [SuFileOutputStream] for that path.
     *
     * @param uri The URI of the content to open.
     * @return A [SuFileOutputStream] for writing to the file, or `null` if the
     *         URI is invalid, the path cannot be resolved, or an error occurs.
     * @see SuFileOutputStream
     * @see ContentResolver.openOutputStream
     */
    fun openSuOutputStream(uri: Uri?): SuFileOutputStream? {
        if (uri == null) return null
        val path = mContext.getPathForUri(uri)
        if (path == null) return null
        return SuFileOutputStream(path)
    }
}

val Context.suContentResolver: SuContentResolver get() = SuContentResolver(this)