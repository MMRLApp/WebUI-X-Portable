package com.dergoogler.mmrl.wx.ui.component.devtools

import android.webkit.WebResourceRequest
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dergoogler.mmrl.ui.component.Tab
import com.dergoogler.mmrl.webui.view.WXView

@Composable
fun NetworkTab(webview: WXView) {
    val requests = webview.networkRequests

    val columns = listOf(
        ColumnDef("Name", 220.dp),
        ColumnDef("Method", 70.dp),
        ColumnDef("Type", 100.dp),
        ColumnDef("Initiator", 200.dp),
        ColumnDef("URL", 300.dp),
    )

    val headerBg = MaterialTheme.colorScheme.tonalSurface
    val rowEvenBg = MaterialTheme.colorScheme.surface
    val rowOddBg = MaterialTheme.colorScheme.tonalSurface.copy(alpha = 0.3f)
    val textColor = MaterialTheme.colorScheme.onSurface
    val mutedColor = MaterialTheme.colorScheme.onSurfaceVariant
    val dividerColor = MaterialTheme.colorScheme.outlineVariant
    val selectedBg = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)

    val scrollState = rememberScrollState()
    var selectedRequest by remember { mutableStateOf<WebResourceRequest?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .horizontalScroll(scrollState)
                .background(headerBg)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            columns.forEach { col ->
                Text(
                    text = col.label,
                    modifier = Modifier
                        .width(col.width)
                        .padding(horizontal = 4.dp),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    ),
                    color = mutedColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        HorizontalDivider(thickness = 0.5.dp, color = dividerColor)

        if (requests.all.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No network requests recorded.",
                    style = MaterialTheme.typography.bodySmall,
                    color = mutedColor
                )
            }
        } else {
            // Split view when an item is selected
            val listWeight = if (selectedRequest != null) 0.5f else 1f

            Column(modifier = Modifier.fillMaxSize()) {
                LazyColumn(modifier = Modifier.weight(listWeight)) {
                    itemsIndexed(requests.all) { index, request ->
                        val url = request.url
                        val name = url.lastPathSegment ?: url.host ?: url.toString()
                        val method = request.method ?: "GET"
                        val type = guessType(url.toString())
                        val initiator = url.host ?: "Other"
                        val fullUrl = url.toString()
                        val isSelected = selectedRequest == request

                        Row(
                            modifier = Modifier
                                .horizontalScroll(scrollState)
                                .background(
                                    when {
                                        isSelected -> selectedBg
                                        index % 2 == 0 -> rowEvenBg
                                        else -> rowOddBg
                                    }
                                )
                                .clickable {
                                    selectedRequest = if (isSelected) null else request
                                }
                                .padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            NetworkCell(name, columns[0].width, textColor)
                            NetworkCell(method, columns[1].width, mutedColor)
                            NetworkCell(type, columns[2].width, mutedColor)
                            NetworkCell(initiator, columns[3].width, mutedColor)
                            NetworkCell(fullUrl, columns[4].width, mutedColor)
                        }

                        HorizontalDivider(thickness = 0.3.dp, color = dividerColor)
                    }
                }

                // Inspector panel
                if (selectedRequest != null) {
                    HorizontalDivider(thickness = 1.dp, color = dividerColor)
                    RequestInspector(
                        request = selectedRequest!!,
                        onClose = { selectedRequest = null },
                        modifier = Modifier.weight(0.5f)
                    )
                }
            }
        }
    }
}

@Composable
private fun RequestInspector(
    request: WebResourceRequest,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val mutedColor = MaterialTheme.colorScheme.onSurfaceVariant
    val dividerColor = MaterialTheme.colorScheme.outlineVariant
    val headerBg = MaterialTheme.colorScheme.surfaceVariant

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("General", "Headers")

    Column(modifier = modifier.fillMaxWidth()) {
        // Inspector top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(headerBg)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = request.url.lastPathSegment ?: request.url.toString(),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = FontFamily.Monospace
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close inspector",
                    modifier = Modifier.size(16.dp),
                    tint = mutedColor
                )
            }
        }

        HorizontalDivider(thickness = 0.5.dp, color = dividerColor)

        // Tabs
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.height(36.dp)
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    modifier = Modifier.height(36.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    )
                }
            }
        }

        HorizontalDivider(thickness = 0.5.dp, color = dividerColor)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            when (selectedTab) {
                0 -> {
                    // General tab
                    val generalItems = listOf(
                        "Request URL" to request.url.toString(),
                        "Request Method" to (request.method ?: "GET"),
                        "Resource Type" to guessType(request.url.toString()),
                        "Is For Main Frame" to request.isForMainFrame.toString(),
                        "Has Gesture" to request.hasGesture().toString(),
                    )
                    items(generalItems) { (key, value) ->
                        InspectorRow(key = key, value = value)
                    }
                }
                1 -> {
                    // Headers tab
                    val headers = request.requestHeaders
                    if (headers.isEmpty()) {
                        item {
                            Text(
                                text = "No headers available.",
                                style = MaterialTheme.typography.bodySmall,
                                color = mutedColor
                            )
                        }
                    } else {
                        items(headers.entries.toList()) { (key, value) ->
                            InspectorRow(key = key, value = value)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InspectorRow(key: String, value: String) {
    val dividerColor = MaterialTheme.colorScheme.outlineVariant

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "$key:",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(140.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                ),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
                softWrap = true
            )
        }
        HorizontalDivider(thickness = 0.3.dp, color = dividerColor)
    }
}

@Composable
private fun NetworkCell(text: String, width: Dp, color: Color) {
    Text(
        text = text,
        modifier = Modifier
            .width(width)
            .padding(horizontal = 4.dp),
        style = MaterialTheme.typography.bodySmall.copy(
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        ),
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

private fun guessType(url: String): String = when {
    url.endsWith(".js") -> "script"
    url.endsWith(".css") -> "stylesheet"
    url.endsWith(".html") || url.endsWith(".htm") -> "document"
    url.endsWith(".png") || url.endsWith(".jpg") ||
            url.endsWith(".jpeg") || url.endsWith(".gif") ||
            url.endsWith(".webp") -> "image"
    url.endsWith(".woff") || url.endsWith(".woff2") ||
            url.endsWith(".ttf") -> "font"
    url.contains("/api/") || url.contains(".json") -> "fetch"
    else -> "other"
}

private data class ColumnDef(val label: String, val width: Dp)
