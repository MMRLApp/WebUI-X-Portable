package com.dergoogler.mmrl.wx.ui.component.devtools

import android.webkit.ConsoleMessage
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dergoogler.mmrl.webui.client.WXChromeClient
import com.dergoogler.mmrl.webui.view.WXView
import com.dergoogler.mmrl.wx.R

@Composable
fun ConsoleTab(webview: WXView) {
    val console = WXChromeClient.consoleLogs
    val listState = rememberLazyListState()

    var filterLevel by remember { mutableStateOf<ConsoleMessage.MessageLevel?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var jsInput by remember { mutableStateOf("") }

    LaunchedEffect(console.size) {
        if (console.isNotEmpty()) {
            listState.animateScrollToItem(console.size - 1)
        }
    }

    val filtered = remember(console.size, filterLevel, searchQuery) {
        console.filter { msg ->
            val levelMatch = filterLevel == null || msg.messageLevel() == filterLevel
            val queryMatch =
                searchQuery.isBlank() || msg.message().contains(searchQuery, ignoreCase = true)
            levelMatch && queryMatch
        }
    }

    val errorCount = remember(console.size) {
        console.count { it.messageLevel() == ConsoleMessage.MessageLevel.ERROR }
    }
    val warnCount = remember(console.size) {
        console.count { it.messageLevel() == ConsoleMessage.MessageLevel.WARNING }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ConsoleToolbar(
            searchQuery = searchQuery,
            onSearchChange = { searchQuery = it },
            filterLevel = filterLevel,
            onFilterChange = { filterLevel = it },
            errorCount = errorCount,
            warnCount = warnCount,
            onClear = { WXChromeClient.consoleLogs.clear() }
        )

        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

        if (filtered.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (console.isEmpty()) "No console output." else "No results for current filter.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f)
            ) {
                itemsIndexed(filtered) { _, msg ->
                    ConsoleRow(msg = msg)
                    HorizontalDivider(
                        thickness = 0.3.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }

        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

        JsInputBar(
            value = jsInput,
            onValueChange = { jsInput = it },
            onRun = {
                val code = jsInput.trim()
                if (code.isNotEmpty()) {
                    webview.runJs(code)
                    jsInput = ""
                }
            }
        )
    }
}

@Composable
private fun ConsoleToolbar(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    filterLevel: ConsoleMessage.MessageLevel?,
    onFilterChange: (ConsoleMessage.MessageLevel?) -> Unit,
    errorCount: Int,
    warnCount: Int,
    onClear: () -> Unit,
) {
    val dividerColor = MaterialTheme.colorScheme.outlineVariant
    val colors = consoleColors()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.tonalSurface)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        IconButton(onClick = onClear, modifier = Modifier.size(28.dp)) {
            Icon(
                imageVector = Icons.Default.Clear,
                contentDescription = "Clear console",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        VerticalDivider(modifier = Modifier.height(16.dp), color = dividerColor)

        BasicTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier
                .weight(1f)
                .background(
                    MaterialTheme.colorScheme.surface,
                    shape = MaterialTheme.shapes.small
                )
                .padding(horizontal = 8.dp, vertical = 4.dp),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodySmall.copy(
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurface
            ),
            decorationBox = { inner ->
                if (searchQuery.isEmpty()) {
                    Text(
                        "Filter",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                inner()
            }
        )

        VerticalDivider(modifier = Modifier.height(16.dp), color = dividerColor)

        // ERROR chip
        LevelFilterChip(
            icon = {
                Icon(
                    painterResource(R.drawable.exclamation_circle),
                    null,
                    modifier = Modifier.size(13.dp)
                )
            },
            label = if (errorCount > 0) "$errorCount" else null,
            selected = filterLevel == ConsoleMessage.MessageLevel.ERROR,
            selectedColor = colors.error,
            onClick = {
                onFilterChange(
                    if (filterLevel == ConsoleMessage.MessageLevel.ERROR) null
                    else ConsoleMessage.MessageLevel.ERROR
                )
            }
        )

        // WARNING chip
        LevelFilterChip(
            icon = {
                Icon(
                    painterResource(R.drawable.alert_triangle_filled),
                    null,
                    modifier = Modifier.size(13.dp)
                )
            },
            label = if (warnCount > 0) "$warnCount" else null,
            selected = filterLevel == ConsoleMessage.MessageLevel.WARNING,
            selectedColor = colors.warn,
            onClick = {
                onFilterChange(
                    if (filterLevel == ConsoleMessage.MessageLevel.WARNING) null
                    else ConsoleMessage.MessageLevel.WARNING
                )
            }
        )

        // LOG chip
        LevelFilterChip(
            icon = {
                Icon(
                    painterResource(R.drawable.info_circle),
                    null,
                    modifier = Modifier.size(13.dp)
                )
            },
            label = null,
            selected = filterLevel == ConsoleMessage.MessageLevel.LOG,
            selectedColor = MaterialTheme.colorScheme.primary,
            onClick = {
                onFilterChange(
                    if (filterLevel == ConsoleMessage.MessageLevel.LOG) null
                    else ConsoleMessage.MessageLevel.LOG
                )
            }
        )

        // TIP chip
        LevelFilterChip(
            icon = {
                Icon(
                    painterResource(R.drawable.bulb),
                    null,
                    modifier = Modifier.size(13.dp)
                )
            },
            label = null,
            selected = filterLevel == ConsoleMessage.MessageLevel.TIP,
            selectedColor = colors.tip,
            onClick = {
                onFilterChange(
                    if (filterLevel == ConsoleMessage.MessageLevel.TIP) null
                    else ConsoleMessage.MessageLevel.TIP
                )
            }
        )

        // DEBUG chip
        LevelFilterChip(
            icon = { Icon(painterResource(R.drawable.bug), null, modifier = Modifier.size(13.dp)) },
            label = null,
            selected = filterLevel == ConsoleMessage.MessageLevel.DEBUG,
            selectedColor = colors.debug,
            onClick = {
                onFilterChange(
                    if (filterLevel == ConsoleMessage.MessageLevel.DEBUG) null
                    else ConsoleMessage.MessageLevel.DEBUG
                )
            }
        )
    }
}

@Composable
private fun LevelFilterChip(
    icon: @Composable () -> Unit,
    label: String?,
    selected: Boolean,
    selectedColor: Color,
    onClick: () -> Unit,
) {
    val bg = if (selected) selectedColor.copy(alpha = 0.15f) else Color.Transparent
    val tint = if (selected) selectedColor else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier
            .background(bg, shape = MaterialTheme.shapes.small)
            .padding(horizontal = 2.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        CompositionLocalProvider(LocalContentColor provides tint) {
            IconButton(onClick = onClick, modifier = Modifier.size(22.dp)) {
                icon()
            }
        }
        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = tint,
                modifier = Modifier.padding(end = 4.dp)
            )
        }
    }
}

@Composable
private fun ConsoleRow(msg: ConsoleMessage) {
    val colors = consoleColors()
    val level = msg.messageLevel()

    val rowStyle = when (level) {
        ConsoleMessage.MessageLevel.ERROR -> ConsoleRowStyle(
            bg = colors.error.copy(alpha = 0.08f),
            textColor = colors.error,
            borderColor = colors.error.copy(alpha = 0.7f),
            icon = R.drawable.exclamation_circle
        )

        ConsoleMessage.MessageLevel.WARNING -> ConsoleRowStyle(
            bg = colors.warn.copy(alpha = 0.08f),
            textColor = colors.warn,
            borderColor = colors.warn.copy(alpha = 0.7f),
            icon = R.drawable.alert_triangle_filled
        )

        ConsoleMessage.MessageLevel.TIP -> ConsoleRowStyle(
            bg = colors.tip.copy(alpha = 0.06f),
            textColor = colors.tip,
            borderColor = colors.tip.copy(alpha = 0.6f),
            icon = R.drawable.bulb
        )

        ConsoleMessage.MessageLevel.DEBUG -> ConsoleRowStyle(
            bg = Color.Transparent,
            textColor = colors.debug,
            borderColor = Color.Transparent,
            icon = R.drawable.bug
        )

        ConsoleMessage.MessageLevel.LOG -> ConsoleRowStyle(
            bg = Color.Transparent,
            textColor = MaterialTheme.colorScheme.onSurface,
            borderColor = Color.Transparent,
            icon = R.drawable.info_circle
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(rowStyle.bg)
            .drawLeftBorder(rowStyle.borderColor, 2.dp)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            painter = painterResource(rowStyle.icon),
            contentDescription = level.name,
            modifier = Modifier
                .size(14.dp)
                .padding(top = 1.dp),
            tint = rowStyle.textColor.copy(alpha = 0.8f)
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = msg.message(),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                ),
                color = rowStyle.textColor,
                softWrap = true
            )

            val source = msg.sourceId()
            if (source.isNotBlank()) {
                Text(
                    text = "${source.substringAfterLast("/")}:${msg.lineNumber()}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    ),
                    color = rowStyle.textColor.copy(alpha = 0.45f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// --- Helpers ---

private data class ConsoleRowStyle(
    val bg: Color,
    val textColor: Color,
    val borderColor: Color,
    @get:DrawableRes val icon: Int,
)

private data class ConsoleColorTokens(
    val error: Color,
    val warn: Color,
    val tip: Color,
    val debug: Color,
)

@Composable
private fun consoleColors() = ConsoleColorTokens(
    error = MaterialTheme.colorScheme.error,
    warn = Color(0xFFF59E0B),
    tip = Color(0xFF22C55E),
    debug = MaterialTheme.colorScheme.onSurfaceVariant,
)

private fun Modifier.drawLeftBorder(color: Color, width: Dp): Modifier =
    this.drawWithContent {
        drawContent()
        if (color != Color.Transparent) {
            drawLine(
                color = color,
                start = Offset(0f, 0f),
                end = Offset(0f, size.height),
                strokeWidth = width.toPx()
            )
        }
    }

@Composable
private fun JsInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onRun: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.tonalSurface)
            .navigationBarsPadding()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // > prompt symbol
        Text(
            text = ">",
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
            ),
            color = MaterialTheme.colorScheme.primary
        )

        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 2.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Unspecified,
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Ascii,
                imeAction = ImeAction.Done,
                platformImeOptions = null,
                showKeyboardOnFocus = null,
                hintLocales = null
            ),
            keyboardActions = KeyboardActions(
                onDone = { onRun() }
            ),
            textStyle = MaterialTheme.typography.bodySmall.copy(
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurface
            ),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text(
                        text = "Evaluate JavaScript...",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
                inner()
            }
        )

        IconButton(
            onClick = onRun,
            modifier = Modifier.size(26.dp),
            enabled = value.trim().isNotEmpty()
        ) {
            Icon(
                painter = painterResource(R.drawable.arrow_badge_right),
                contentDescription = "Run",
                modifier = Modifier.size(14.dp),
                tint = if (value.trim().isNotEmpty())
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
            )
        }
    }
}