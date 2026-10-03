package com.dergoogler.mmrl.wx.ui.component

import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import java.io.InputStream

private const val DefaultAspectRatio = 2.048f
private val DefaultShape: RoundedCornerShape = RoundedCornerShape(0.dp)

@Composable
fun LocalCover(
    modifier: Modifier = Modifier,
    inputStream: InputStream,
    shape: RoundedCornerShape = DefaultShape,
    aspectRatio: Float = DefaultAspectRatio,
) {
    val bitmap = remember(inputStream) {
        try {
            inputStream.use { stream ->
                val bytes = stream.readBytes()
                val nativeBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                nativeBitmap?.asImageBitmap()
            }
        } catch (e: Exception) {
            Log.e("LocalCover", "Failed to load cover image", e)
            null
        }
    }

    // Safely exit the Composable layout if bitmap creation failed
    if (bitmap == null) return

    Image(
        painter = BitmapPainter(bitmap),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .aspectRatio(aspectRatio)
                .then(modifier),
    )
}