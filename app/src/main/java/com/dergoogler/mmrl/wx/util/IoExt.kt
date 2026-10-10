package com.dergoogler.mmrl.wx.util

import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import coil3.ImageLoader
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.svg.SvgDecoder
import dev.mmrlx.nio.SuFile
import dev.mmrlx.nio.inputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.InputStream

enum class ImageType {
    PNG, JPEG, WEBP, GIF, BMP, HEIF, AVIF, SVG, UNKNOWN
}

fun InputStream.detectImageType(): ImageType {
    val input = if (markSupported()) this else BufferedInputStream(this)
    input.mark(64)
    val header = ByteArray(64)
    val count = input.read(header)
    input.reset()

    if (count < 4) return ImageType.UNKNOWN

    // PNG
    if (header.copyOfRange(0, 8).contentEquals(
            byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
        )
    ) return ImageType.PNG

    // JPEG
    if (header[0] == 0xFF.toByte() && header[1] == 0xD8.toByte() && header[2] == 0xFF.toByte()) {
        return ImageType.JPEG
    }

    // GIF
    if (header.copyOfRange(0, 6).contentEquals("GIF87a".encodeToByteArray()) ||
        header.copyOfRange(0, 6).contentEquals("GIF89a".encodeToByteArray())
    ) return ImageType.GIF

    // BMP
    if (header[0] == 'B'.code.toByte() && header[1] == 'M'.code.toByte()) return ImageType.BMP

    // WebP / RIFF
    if (header.copyOfRange(0, 4).contentEquals("RIFF".encodeToByteArray()) &&
        header.copyOfRange(8, 12).contentEquals("WEBP".encodeToByteArray())
    ) return ImageType.WEBP

    // SVG
    val text =
        header.copyOf(count).toString(Charsets.UTF_8).trimStart('\uFEFF', ' ', '\t', '\r', '\n')
    if (text.startsWith("<svg", ignoreCase = true) ||
        (text.startsWith("<?xml", ignoreCase = true) && "<svg" in text)
    ) return ImageType.SVG

    // HEIF/AVIF
    if (count >= 12 && header.copyOfRange(4, 8).contentEquals("ftyp".encodeToByteArray())) {
        val brand = header.copyOfRange(8, 12).toString(Charsets.US_ASCII)
        return when (brand) {
            "heic", "heix", "hevc", "hevx", "mif1", "msf1" -> ImageType.HEIF
            "avif", "avis" -> ImageType.AVIF
            else -> ImageType.UNKNOWN
        }
    }

    return ImageType.UNKNOWN
}

private sealed interface ResourceData {
    data object Empty : ResourceData
    data class Raster(val painter: BitmapPainter) : ResourceData
    data class Vector(val bytes: ByteArray) : ResourceData {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Vector

            return bytes.contentEquals(other.bytes)
        }

        override fun hashCode(): Int {
            return bytes.contentHashCode()
        }
    }
}

@Composable
fun SuFile.toPainter(): Painter {
    val context = LocalContext.current

    val svgImageLoader = remember {
        ImageLoader.Builder(context)
            .components { add(SvgDecoder.Factory()) }
            .build()
    }

    val resourceState by produceState<ResourceData>(
        initialValue = ResourceData.Empty,
        key1 = absolutePath,
        key2 = lastModified()
    ) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                inputStream().use { stream ->
                    val allBytes = stream.readBytes()
                    val type = allBytes.inputStream().detectImageType()

                    when (type) {
                        ImageType.SVG -> ResourceData.Vector(allBytes)
                        ImageType.UNKNOWN -> ResourceData.Empty
                        else -> {
                            // Decode standard rasters
                            val bitmap = BitmapFactory.decodeByteArray(allBytes, 0, allBytes.size)
                            if (bitmap != null) {
                                ResourceData.Raster(BitmapPainter(bitmap.asImageBitmap()))
                            } else {
                                ResourceData.Empty
                            }
                        }
                    }
                }
            }.getOrElse { ResourceData.Empty }
        }
    }

    return when (val data = resourceState) {
        is ResourceData.Raster -> data.painter
        is ResourceData.Vector -> {
            val imageRequest = remember(data.bytes) {
                ImageRequest.Builder(context)
                    .data(data.bytes)
                    .build()
            }
            rememberAsyncImagePainter(
                model = imageRequest,
                imageLoader = svgImageLoader
            )
        }
        is ResourceData.Empty -> remember { ColorPainter(Color.Transparent) }
    }
}
