package com.dergoogler.mmrl.wx.util

import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import dev.mmrlx.nio.SuFile
import dev.mmrlx.nio.inputStream

@Composable
fun SuFile.toPainter(): BitmapPainter {
    val bitmap = remember(absolutePath, lastModified()) {
        inputStream().use {
            BitmapFactory.decodeStream(it).asImageBitmap()
        }
    }

    return remember(bitmap) {
        BitmapPainter(bitmap)
    }
}