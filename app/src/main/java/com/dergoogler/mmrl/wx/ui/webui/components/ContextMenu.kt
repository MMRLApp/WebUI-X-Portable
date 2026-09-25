package com.dergoogler.mmrl.wx.ui.webui.components

import android.app.DownloadManager
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.provider.ContactsContract
import android.util.Base64
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.dergoogler.mmrl.wx.R
import com.dergoogler.mmrl.wx.ui.providable.LocalBrowser
import dev.mmrlx.compose.layout.outlinedCard
import dev.mmrlx.compose.ui.HorizontalDivider
import dev.mmrlx.compose.ui.Text
import dev.mmrlx.compose.ui.theme.MMRLXTheme
import dev.mmrlx.compose.webui.WebUIState
import dev.mmrlx.webui.WebUIContextMenu
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

private const val TAG = "ContextMenu"

private val LocalDismissRequest = staticCompositionLocalOf<(() -> Unit)?> { null }

@Composable
fun ContextMenu(
    webui: WebUIState,
    menu: WebUIContextMenu,
    onDismiss: () -> Unit,
) {
    val browser = LocalBrowser.current
    val clipboard = LocalClipboard.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val windowSizePx = LocalWindowInfo.current.containerSize
    val windowWidthPx = windowSizePx.width.toFloat()
    val density = LocalDensity.current
    val windowWidthDp = with(density) { windowWidthPx.toDp().value }
    val mobileMaxCap = 224.0f
    val calculatedMaxWidth = (windowWidthDp * 0.65f).coerceAtMost(mobileMaxCap)

    var pendingSaveUrl by remember { mutableStateOf<String?>(null) }

    val saveImageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("image/jpeg")
    ) { destination ->
        val url = pendingSaveUrl
        pendingSaveUrl = null
        if (destination != null && url != null) {
            scope.launch(Dispatchers.IO) {
                runCatching {
                    val bitmap = downloadBitmap(url)
                    context.contentResolver.openOutputStream(destination)?.use { out ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                    } ?: error("Could not open output stream for $destination")
                }.onFailure { e ->
                    Log.e(TAG, "Save image failed for $url", e)
                }
            }
        }
    }

    CompositionLocalProvider(
        LocalDismissRequest provides onDismiss
    ) {
        Popup(
            alignment = Alignment.TopStart,
            offset = IntOffset(
                x = menu.x.toInt(),
                y = menu.y.toInt()
            ),
            onDismissRequest = onDismiss,
            properties = PopupProperties(
                focusable = true,
                dismissOnClickOutside = true,
                dismissOnBackPress = true
            )
        ) {
            Column(
                modifier = Modifier
                    .outlinedCard()
                    .widthIn(
                        min = 112.0f.dp,
                        max = calculatedMaxWidth.dp
                    )
            ) {
                ContextMenuContent {
                    ContextMenuItem(
                        enabled = webui.canGoBack(),
                        text = stringResource(R.string.back)
                    ) {
                        webui.goBack()
                    }

                    ContextMenuItem(
                        enabled = webui.canGoForward(),
                        text = stringResource(R.string.forward)
                    ) {
                        webui.goForward()
                    }

                    ContextMenuItem(stringResource(R.string.reload)) {
                        webui.reload()
                    }

                    if (webui.settings.debug) {
                        ContextMenuItem(stringResource(R.string.recompose)) {
                            webui.recompose()
                        }
                    }
                }

                ContextMenuDivider()

                ContextMenuContent {
                    if (menu.type == WebUIContextMenu.Type.SRC_ANCHOR ||
                        menu.type == WebUIContextMenu.Type.SRC_IMAGE_ANCHOR
                    ) {
                        ContextMenuItem(stringResource(R.string.open_link_in_new_tab)) {
                            menu.extra?.let { browser.open(it) }
                        }
                        ContextMenuItem(stringResource(R.string.copy_link_address)) {
                            val clipData = ClipData.newPlainText("label", menu.extra)
                            clipboard.setClipEntry(ClipEntry(clipData))
                        }
                        ContextMenuItem(stringResource(R.string.share_link)) {
                            val url = menu.extra ?: return@ContextMenuItem
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, url)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, null))
                        }
                        ContextMenuItem(stringResource(R.string.download_link)) {
                            val url = menu.extra ?: return@ContextMenuItem
                            runCatching {
                                val fileName = url.substringAfterLast('/').substringBefore('?')
                                    .ifBlank { "download_${System.currentTimeMillis()}" }
                                val request = DownloadManager.Request(url.toUri()).apply {
                                    setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                    setDestinationInExternalPublicDir(
                                        Environment.DIRECTORY_DOWNLOADS,
                                        fileName
                                    )
                                    setAllowedNetworkTypes(
                                        DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE
                                    )
                                }
                                val downloadManager =
                                    context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                                downloadManager.enqueue(request)
                            }.onFailure { e ->
                                Log.e(TAG, "Download link failed for $url", e)
                            }
                        }
                    }

                    if (menu.type == WebUIContextMenu.Type.IMAGE) {
                        val imageUrl = menu.extra

                        ContextMenuItem(stringResource(R.string.open_image_in_new_tab)) {
                            imageUrl?.let { browser.open(it) }
                        }

                        ContextMenuItem(stringResource(R.string.save_image_as)) {
                            val url = imageUrl ?: return@ContextMenuItem
                            pendingSaveUrl = url
                            saveImageLauncher.launch(jpgFileNameFromUrl(url))
                        }

                        ContextMenuItem(stringResource(R.string.copy_image)) {
                            val url = imageUrl ?: return@ContextMenuItem
                            val uri = withContext(Dispatchers.IO) {
                                runCatching {
                                    val bitmap = downloadBitmap(url)
                                    saveImageToCache(context, bitmap, jpgFileNameFromUrl(url))
                                }.onFailure { e ->
                                    Log.e(TAG, "Copy image failed for $url", e)
                                }.getOrNull()
                            }
                            if (uri != null) {
                                val clipData =
                                    ClipData.newUri(context.contentResolver, "image", uri)
                                clipboard.setClipEntry(ClipEntry(clipData))
                            } else {
                                Log.w(TAG, "Copy image: no uri produced for $url")
                            }
                        }

                        ContextMenuItem(stringResource(R.string.copy_image_address)) {
                            val clipData = ClipData.newPlainText("label", imageUrl)
                            clipboard.setClipEntry(ClipEntry(clipData))
                        }

                        ContextMenuItem(stringResource(R.string.share_image)) {
                            val url = imageUrl ?: return@ContextMenuItem
                            val uri = withContext(Dispatchers.IO) {
                                runCatching {
                                    val bitmap = downloadBitmap(url)
                                    saveImageToCache(context, bitmap, jpgFileNameFromUrl(url))
                                }.onFailure { e ->
                                    Log.e(TAG, "Share image failed for $url", e)
                                }.getOrNull()
                            }
                            if (uri != null) {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "image/jpeg"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, null))
                            }
                        }
                    }

                    if (menu.type == WebUIContextMenu.Type.PHONE) {
                        val phoneNumber = menu.extra

                        ContextMenuItem(stringResource(R.string.call)) {
                            phoneNumber?.let {
                                context.startActivity(Intent(Intent.ACTION_DIAL, "tel:$it".toUri()))
                            }
                        }
                        ContextMenuItem(stringResource(R.string.send_sms)) {
                            phoneNumber?.let {
                                context.startActivity(
                                    Intent(
                                        Intent.ACTION_SENDTO,
                                        "smsto:$it".toUri()
                                    )
                                )
                            }
                        }
                        ContextMenuItem(stringResource(R.string.copy_phone_number)) {
                            clipboard.setClipEntry(
                                ClipEntry(
                                    ClipData.newPlainText(
                                        "label",
                                        phoneNumber
                                    )
                                )
                            )
                        }
                        ContextMenuItem(stringResource(R.string.add_to_contacts)) {
                            phoneNumber?.let {
                                val intent = Intent(Intent.ACTION_INSERT).apply {
                                    type = ContactsContract.Contacts.CONTENT_TYPE
                                    putExtra(ContactsContract.Intents.Insert.PHONE, it)
                                }
                                context.startActivity(intent)
                            }
                        }
                    }

                    if (menu.type == WebUIContextMenu.Type.GEO) {
                        val geo = menu.extra

                        ContextMenuItem(stringResource(R.string.open_in_maps)) {
                            geo?.let {
                                val uri = if (it.startsWith("geo:")) {
                                    it.toUri()
                                } else {
                                    "geo:0,0?q=${Uri.encode(it)}".toUri()
                                }
                                context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                            }
                        }
                        ContextMenuItem(stringResource(R.string.copy_address)) {
                            clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("label", geo)))
                        }
                    }

                    if (menu.type == WebUIContextMenu.Type.EMAIL) {
                        val email = menu.extra

                        ContextMenuItem(stringResource(R.string.send_email)) {
                            email?.let {
                                context.startActivity(
                                    Intent(
                                        Intent.ACTION_SENDTO,
                                        "mailto:$it".toUri()
                                    )
                                )
                            }
                        }
                        ContextMenuItem(stringResource(R.string.copy_email_address)) {
                            clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("label", email)))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ContextMenuItem(
    text: String,
    enabled: Boolean = true,
    onClick: suspend CoroutineScope.() -> Unit,
) {
    val scope = rememberCoroutineScope()
    val dismissRequest = LocalDismissRequest.current
    val contentAlpha = if (enabled) 1.0f else 0.38f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MMRLXTheme.shapes.medium)
            .clickable(
                enabled = enabled,
                onClick = {
                    scope.launch {
                        onClick()
                        dismissRequest?.invoke()
                    }
                }
            )
            .padding(6.dp),
    ) {
        Text(
            text = text,
            style = MMRLXTheme.typography.labelMedium,
            modifier = Modifier.alpha(contentAlpha)
        )
    }
}

@Composable
fun ContextMenuDivider() = HorizontalDivider(
    thickness = 0.3.dp,
    modifier = Modifier.padding(vertical = 1.321.dp)
)

@Composable
fun ContextMenuContent(
    content: @Composable ColumnScope.() -> Unit,
) = Column(
    modifier = Modifier
        .padding(6.dp),
    content = content
)

private fun downloadBitmap(url: String): Bitmap {
    if (url.startsWith("blob:")) {
        error("blob: image sources can't be fetched directly — they must be read via the page's JavaScript (e.g. canvas.toDataURL or a fetch()+postMessage bridge) before this can decode them.")
    }

    if (url.startsWith("data:")) {
        val commaIndex = url.indexOf(',')
        require(commaIndex != -1) { "Malformed data URI: $url" }
        val meta = url.substring(5, commaIndex) // e.g. "image/png;base64"
        val data = url.substring(commaIndex + 1)
        val bytes = if (meta.contains("base64")) {
            Base64.decode(data, Base64.DEFAULT)
        } else {
            Uri.decode(data).toByteArray(Charsets.UTF_8)
        }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            ?: error("Failed to decode inline data: image")
    }

    val connection = (URL(url).openConnection() as HttpURLConnection).apply {
        // Many hosts reject/blank-image requests with no UA or referer.
        setRequestProperty(
            "User-Agent",
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0 Mobile Safari/537.36"
        )
        setRequestProperty("Referer", url)
        instanceFollowRedirects = true
        connectTimeout = 15_000
        readTimeout = 15_000
    }

    connection.use {
        val code = connection.responseCode
        if (code !in 200..299) {
            error("HTTP $code fetching $url")
        }
        connection.inputStream.use { input ->
            return BitmapFactory.decodeStream(input)
                ?: error("Server responded but bytes weren't a decodable image: $url")
        }
    }
}

private inline fun <T> HttpURLConnection.use(block: (HttpURLConnection) -> T): T =
    try {
        block(this)
    } finally {
        disconnect()
    }

private fun jpgFileNameFromUrl(url: String): String {
    val base = url.substringAfterLast('/')
        .substringBefore('?')
        .substringBeforeLast('.')
        .ifBlank { "image_${System.currentTimeMillis()}" }
    return "$base.jpg"
}

private fun saveImageToCache(context: Context, bitmap: Bitmap, fileName: String): Uri {
    val cacheDir = File(context.cacheDir, "images").apply { mkdirs() }
    val file = File(cacheDir, fileName)
    FileOutputStream(file).use { out ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
    }
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
}