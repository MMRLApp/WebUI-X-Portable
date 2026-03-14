package com.dergoogler.mmrl.wx.util

import android.content.Context
import android.net.Uri
import com.dergoogler.mmrl.platform.Platform
import com.dergoogler.mmrl.platform.PlatformManager
import com.dergoogler.mmrl.platform.file.suContentResolver
import java.io.InputStream
import java.io.OutputStream

class WXContentResolver(
    private val context: Context,
) {
    private val platform
        get() = PlatformManager.get(Platform.Unknown) {
            platform
        }


    fun openInputStream(uri: Uri): InputStream? {
        if (platform.isValid) {
            return context.suContentResolver.openSuInputStream(uri)
        }

        return context.contentResolver.openInputStream(uri)
    }

    fun openOutputStream(uri: Uri): OutputStream? {
        if (platform.isValid) {
            return context.suContentResolver.openSuOutputStream(uri)
        }

        return context.contentResolver.openOutputStream(uri)
    }

    fun takePersistableUriPermission(uri: Uri, modeFlags: Int) {
        // Ignore for rooted devices
        if (!platform.isValid) {
            context.contentResolver.takePersistableUriPermission(uri, modeFlags)
        }

    }
}

val Context.wxContentResolver get() = WXContentResolver(this)