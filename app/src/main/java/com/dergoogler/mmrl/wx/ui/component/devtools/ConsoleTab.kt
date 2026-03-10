package com.dergoogler.mmrl.wx.ui.component.devtools

import android.webkit.ConsoleMessage
import androidx.annotation.DrawableRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dergoogler.mmrl.webui.client.WXChromeClient
import com.dergoogler.mmrl.webui.view.WXView
import com.dergoogler.mmrl.wx.R
import org.json.JSONObject

// ---------------------------------------------------------------------------
// Unified log entry model
// ---------------------------------------------------------------------------

private sealed class LogEntry {
    data class WebMessage(val msg: ConsoleMessage) : LogEntry()
    data class EvalInput(val code: String) : LogEntry()
    sealed class EvalResult : LogEntry() {
        data class Parsed(val root: ResultNode) : EvalResult()
        data class Error(val message: String) : EvalResult()
    }
}

// ---------------------------------------------------------------------------
// Result tree model
// ---------------------------------------------------------------------------

private sealed class ResultNode {
    abstract val key: String?
    abstract val depth: Int

    data class Primitive(
        override val key: String?,
        val value: String,
        val kind: PrimitiveKind,
        override val depth: Int,
    ) : ResultNode()

    data class Expandable(
        override val key: String?,
        val label: String,
        val closeToken: String,
        val children: List<ResultNode>,
        override val depth: Int,
        val id: String,
    ) : ResultNode()
}

private enum class PrimitiveKind {
    STRING, NUMBER, BOOLEAN, NULL_UNDEFINED, FUNCTION, OTHER
}

// ---------------------------------------------------------------------------
// Flat row model for rendering
// ---------------------------------------------------------------------------

private sealed class FlatRow {
    data class PrimitiveRow(val node: ResultNode.Primitive) : FlatRow()
    data class OpenRow(val node: ResultNode.Expandable, val isCollapsed: Boolean) : FlatRow()
    data class CloseRow(val token: String, val depth: Int, val parentId: String) : FlatRow()
}

// ---------------------------------------------------------------------------
// ConsoleTab
// ---------------------------------------------------------------------------

@Composable
fun ConsoleTab(webview: WXView) {
    val console = WXChromeClient.consoleLogs
    val evalEntries = remember { mutableStateListOf<LogEntry>() }
    val listState = rememberLazyListState()

    var filterLevel by remember { mutableStateOf<ConsoleMessage.MessageLevel?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var jsInput by remember { mutableStateOf("") }

    val allEntries: List<LogEntry> = remember(console.size, evalEntries.size) {
        console.map { LogEntry.WebMessage(it) } + evalEntries
    }

    val filtered: List<LogEntry> = remember(allEntries.size, filterLevel, searchQuery) {
        allEntries.filter { entry ->
            when (entry) {
                is LogEntry.WebMessage -> {
                    val levelMatch = filterLevel == null || entry.msg.messageLevel() == filterLevel
                    val queryMatch = searchQuery.isBlank() || entry.msg.message()
                        .contains(searchQuery, ignoreCase = true)
                    levelMatch && queryMatch
                }
                is LogEntry.EvalInput ->
                    searchQuery.isBlank() || entry.code.contains(searchQuery, ignoreCase = true)
                is LogEntry.EvalResult.Parsed -> true
                is LogEntry.EvalResult.Error ->
                    searchQuery.isBlank() || entry.message.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val errorCount = remember(console.size) {
        console.count { it.messageLevel() == ConsoleMessage.MessageLevel.ERROR }
    }
    val warnCount = remember(console.size) {
        console.count { it.messageLevel() == ConsoleMessage.MessageLevel.WARNING }
    }

    LaunchedEffect(filtered.size) {
        if (filtered.isNotEmpty()) listState.animateScrollToItem(filtered.size - 1)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ConsoleToolbar(
            searchQuery = searchQuery,
            onSearchChange = { searchQuery = it },
            filterLevel = filterLevel,
            onFilterChange = { filterLevel = it },
            errorCount = errorCount,
            warnCount = warnCount,
            onClear = {
                WXChromeClient.consoleLogs.clear()
                evalEntries.clear()
            }
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
                    text = if (allEntries.isEmpty()) "No console output."
                    else "No results for current filter.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(state = listState, modifier = Modifier.weight(1f)) {
                itemsIndexed(filtered) { _, entry ->
                    when (entry) {
                        is LogEntry.WebMessage -> ConsoleRow(msg = entry.msg)
                        is LogEntry.EvalInput -> EvalInputRow(code = entry.code)
                        is LogEntry.EvalResult.Error -> EvalErrorRow(message = entry.message)
                        is LogEntry.EvalResult.Parsed -> EvalTreeRow(root = entry.root)
                    }
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
                    submitJs(code, webview, evalEntries)
                    jsInput = ""
                }
            }
        )
    }
}

// ---------------------------------------------------------------------------
// JS eval
// ---------------------------------------------------------------------------

private var nodeIdCounter = 0

private fun submitJs(code: String, webview: WXView, evalEntries: MutableList<LogEntry>) {
    evalEntries.add(LogEntry.EvalInput(code))

    val wrapped = """
        (function() {
            function formatValue(val, depth) {
                if (depth === undefined) depth = 0;
                if (val === undefined) return { type:'primitive', kind:'undefined', value:'undefined' };
                if (val === null) return { type:'primitive', kind:'null', value:'null' };
                if (typeof val === 'boolean') return { type:'primitive', kind:'boolean', value:String(val) };
                if (typeof val === 'number') return { type:'primitive', kind:'number', value:String(val) };
                if (typeof val === 'string') return { type:'primitive', kind:'string', value:val };
                if (typeof val === 'symbol') return { type:'primitive', kind:'other', value:val.toString() };
                if (typeof val === 'function') {
                    var src = Function.prototype.toString.call(val);
                    var sig = src.split('\n')[0].replace(/\{.*/, '').trim();
                    return { type:'primitive', kind:'function', value:'f ' + sig };
                }
                if (Array.isArray(val)) {
                    if (depth >= 3) return { type:'primitive', kind:'other', value:'[Array(' + val.length + ')]' };
                    var items = [];
                    for (var i = 0; i < Math.min(val.length, 50); i++) {
                        try {
                            var child = formatValue(val[i], depth + 1);
                            child.key = String(i);
                            items.push(child);
                        } catch(e) {
                            items.push({ type:'primitive', kind:'other', value:'?', key:String(i) });
                        }
                    }
                    if (val.length > 50) items.push({ type:'primitive', kind:'other', value:'... ' + (val.length - 50) + ' more', key:'...' });
                    return { type:'expandable', label:'Array(' + val.length + ')', closeToken:']', children:items };
                }
                if (typeof val === 'object') {
                    if (depth >= 3) return { type:'primitive', kind:'other', value:'{...}' };
                    var prefix = '';
                    if (val.constructor && val.constructor.name && val.constructor.name !== 'Object') {
                        prefix = val.constructor.name;
                    }
                    var keys = [];
                    try { keys = Object.getOwnPropertyNames(val).filter(function(k){ return k !== '__proto__'; }); } catch(e) {}
                    if (keys.length === 0) try { keys = Object.keys(val); } catch(e) {}
                    var pairs = [];
                    var shown = 0;
                    for (var i = 0; i < keys.length && shown < 30; i++) {
                        var k = keys[i];
                        try {
                            var child = formatValue(val[k], depth + 1);
                            child.key = k;
                            pairs.push(child);
                            shown++;
                        } catch(e) {
                            pairs.push({ type:'primitive', kind:'other', value:'[getter]', key:k });
                            shown++;
                        }
                    }
                    if (keys.length > 30) pairs.push({ type:'primitive', kind:'other', value:'... ' + (keys.length - 30) + ' more keys', key:'...' });
                    return { type:'expandable', label:prefix, closeToken:'}', children:pairs };
                }
                return { type:'primitive', kind:'other', value:String(val) };
            }
            try {
                var __r = eval(${escapeJsString(code)});
                return JSON.stringify({ ok:true, value:formatValue(__r) });
            } catch(e) {
                return JSON.stringify({ ok:false, value:e.toString() });
            }
        })()
    """.trimIndent()

    webview.evaluateJavascript(wrapped) { raw ->
        if (raw == null || raw == "null") {
            evalEntries.add(
                LogEntry.EvalResult.Parsed(
                    ResultNode.Primitive(null, "null", PrimitiveKind.NULL_UNDEFINED, 0)
                )
            )
            return@evaluateJavascript
        }
        try {
            val outer = JSONObject("{\"v\":$raw}")
            val inner = JSONObject(outer.getString("v"))
            val ok = inner.getBoolean("ok")
            if (!ok) {
                evalEntries.add(LogEntry.EvalResult.Error(inner.getString("value")))
                return@evaluateJavascript
            }
            val tree = parseResultNode(inner.getJSONObject("value"), key = null, depth = 0)
            evalEntries.add(LogEntry.EvalResult.Parsed(tree))
        } catch (e: Exception) {
            evalEntries.add(LogEntry.EvalResult.Error("Parse error: ${e.message}"))
        }
    }
}

private fun parseResultNode(obj: JSONObject, key: String?, depth: Int): ResultNode {
    return when (obj.getString("type")) {
        "expandable" -> {
            val label = obj.optString("label", "")
            val closeToken = obj.optString("closeToken", "}")
            val childArray = obj.optJSONArray("children")
            val children = mutableListOf<ResultNode>()
            if (childArray != null) {
                for (i in 0 until childArray.length()) {
                    val childObj = childArray.getJSONObject(i)
                    val childKey = if (childObj.has("key")) childObj.getString("key") else null
                    children.add(parseResultNode(childObj, childKey, depth + 1))
                }
            }
            ResultNode.Expandable(
                key = key,
                label = label,
                closeToken = closeToken,
                children = children,
                depth = depth,
                id = "node_${nodeIdCounter++}"
            )
        }
        else -> {
            val kind = when (obj.optString("kind")) {
                "string" -> PrimitiveKind.STRING
                "number" -> PrimitiveKind.NUMBER
                "boolean" -> PrimitiveKind.BOOLEAN
                "null", "undefined" -> PrimitiveKind.NULL_UNDEFINED
                "function" -> PrimitiveKind.FUNCTION
                else -> PrimitiveKind.OTHER
            }
            ResultNode.Primitive(key, obj.optString("value", ""), kind, depth)
        }
    }
}

private fun escapeJsString(code: String): String {
    val escaped = code
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
    return "\"$escaped\""
}

// ---------------------------------------------------------------------------
// Eval rows
// ---------------------------------------------------------------------------

@Composable
private fun EvalInputRow(code: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.06f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = ">",
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            ),
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = code,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            ),
            color = MaterialTheme.colorScheme.onSurface,
            softWrap = true
        )
    }
}

@Composable
private fun EvalErrorRow(message: String) {
    val colors = consoleColors()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.error.copy(alpha = 0.06f))
            .drawLeftBorder(colors.error.copy(alpha = 0.5f), 2.dp)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            painter = painterResource(R.drawable.exclamation_circle),
            contentDescription = null,
            modifier = Modifier
                .size(12.dp)
                .padding(top = 2.dp),
            tint = colors.error.copy(alpha = 0.7f)
        )
        Text(
            text = "✗ $message",
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                lineHeight = 16.sp
            ),
            color = colors.error,
            softWrap = true
        )
    }
}

@Composable
private fun EvalTreeRow(root: ResultNode) {
    val collapsedIds = remember { mutableStateListOf<String>() }

    val rows = remember(root, collapsedIds.toList()) {
        buildList { flattenNode(root, this, collapsedIds) }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.03f))
    ) {
        rows.forEach { row ->
            EvalNodeRow(
                node = row,
                onToggle = { id ->
                    if (collapsedIds.contains(id)) collapsedIds.remove(id)
                    else collapsedIds.add(id)
                }
            )
        }
    }
}

private fun flattenNode(
    node: ResultNode,
    out: MutableList<FlatRow>,
    collapsedIds: List<String>,
) {
    when (node) {
        is ResultNode.Primitive -> out.add(FlatRow.PrimitiveRow(node))
        is ResultNode.Expandable -> {
            val collapsed = collapsedIds.contains(node.id)
            out.add(FlatRow.OpenRow(node, collapsed))
            if (!collapsed) {
                node.children.forEach { flattenNode(it, out, collapsedIds) }
                out.add(FlatRow.CloseRow(node.closeToken, node.depth, node.id))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EvalNodeRow(
    node: FlatRow,
    onToggle: (String) -> Unit,
) {
    val indentPerLevel = 12.dp

    val stringColor = Color(0xFF22C55E)
    val numberColor = Color(0xFF60A5FA)
    val boolColor = Color(0xFFF59E0B)
    val nullColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    val functionColor = MaterialTheme.colorScheme.tertiary
    val keyColor = MaterialTheme.colorScheme.onSurfaceVariant
    val punctColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    val labelColor = MaterialTheme.colorScheme.primary

    when (node) {
        is FlatRow.PrimitiveRow -> {
            val p = node.node
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = indentPerLevel * p.depth + 4.dp + 14.dp,
                        end = 8.dp,
                        top = 2.dp,
                        bottom = 2.dp
                    )
            ) {
                Text(
                    text = buildAnnotatedString {
                        if (p.key != null) {
                            withStyle(SpanStyle(color = keyColor, fontFamily = FontFamily.Monospace)) {
                                append(p.key)
                            }
                            withStyle(SpanStyle(color = punctColor)) { append(": ") }
                        }
                        val valueColor = when (p.kind) {
                            PrimitiveKind.STRING -> stringColor
                            PrimitiveKind.NUMBER -> numberColor
                            PrimitiveKind.BOOLEAN -> boolColor
                            PrimitiveKind.NULL_UNDEFINED -> nullColor
                            PrimitiveKind.FUNCTION -> functionColor
                            PrimitiveKind.OTHER -> MaterialTheme.colorScheme.onSurface
                        }
                        val displayValue = if (p.kind == PrimitiveKind.STRING && p.key != null)
                            "\"${p.value}\"" else p.value
                        withStyle(SpanStyle(color = valueColor, fontFamily = FontFamily.Monospace)) {
                            append(displayValue)
                        }
                    },
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    softWrap = true
                )
            }
        }

        is FlatRow.OpenRow -> {
            val e = node.node
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = { onToggle(e.id) },
                        onLongClick = {}
                    )
                    .padding(
                        start = indentPerLevel * e.depth + 4.dp,
                        end = 8.dp,
                        top = 2.dp,
                        bottom = 2.dp
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    painter = painterResource(
                        if (node.isCollapsed) R.drawable.chevron_right else R.drawable.chevron_down
                    ),
                    contentDescription = null,
                    modifier = Modifier.size(10.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                Text(
                    text = buildAnnotatedString {
                        if (e.key != null) {
                            withStyle(SpanStyle(color = keyColor, fontFamily = FontFamily.Monospace)) {
                                append(e.key)
                            }
                            withStyle(SpanStyle(color = punctColor)) { append(": ") }
                        }
                        if (e.label.isNotEmpty()) {
                            withStyle(SpanStyle(color = labelColor, fontFamily = FontFamily.Monospace)) {
                                append(e.label)
                            }
                            append(" ")
                        }
                        val openToken = if (e.closeToken == "]") "[" else "{"
                        withStyle(SpanStyle(color = punctColor)) { append(openToken) }
                        if (node.isCollapsed) {
                            withStyle(SpanStyle(color = punctColor)) { append("…") }
                            withStyle(SpanStyle(color = punctColor)) { append(e.closeToken) }
                        }
                    },
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    softWrap = false
                )
            }
        }

        is FlatRow.CloseRow -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = indentPerLevel * node.depth + 4.dp + 14.dp,
                        end = 8.dp,
                        top = 2.dp,
                        bottom = 2.dp
                    )
            ) {
                Text(
                    text = node.token,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = punctColor
                    )
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Toolbar
// ---------------------------------------------------------------------------

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
                            fontFamily = FontFamily.Monospace
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                inner()
            }
        )

        VerticalDivider(modifier = Modifier.height(16.dp), color = dividerColor)

        LevelFilterChip(
            icon = { Icon(painterResource(R.drawable.exclamation_circle), null, modifier = Modifier.size(13.dp)) },
            label = if (errorCount > 0) "$errorCount" else null,
            selected = filterLevel == ConsoleMessage.MessageLevel.ERROR,
            selectedColor = colors.error,
            onClick = { onFilterChange(if (filterLevel == ConsoleMessage.MessageLevel.ERROR) null else ConsoleMessage.MessageLevel.ERROR) }
        )
        LevelFilterChip(
            icon = { Icon(painterResource(R.drawable.alert_triangle_filled), null, modifier = Modifier.size(13.dp)) },
            label = if (warnCount > 0) "$warnCount" else null,
            selected = filterLevel == ConsoleMessage.MessageLevel.WARNING,
            selectedColor = colors.warn,
            onClick = { onFilterChange(if (filterLevel == ConsoleMessage.MessageLevel.WARNING) null else ConsoleMessage.MessageLevel.WARNING) }
        )
        LevelFilterChip(
            icon = { Icon(painterResource(R.drawable.info_circle), null, modifier = Modifier.size(13.dp)) },
            label = null,
            selected = filterLevel == ConsoleMessage.MessageLevel.LOG,
            selectedColor = MaterialTheme.colorScheme.primary,
            onClick = { onFilterChange(if (filterLevel == ConsoleMessage.MessageLevel.LOG) null else ConsoleMessage.MessageLevel.LOG) }
        )
        LevelFilterChip(
            icon = { Icon(painterResource(R.drawable.bulb), null, modifier = Modifier.size(13.dp)) },
            label = null,
            selected = filterLevel == ConsoleMessage.MessageLevel.TIP,
            selectedColor = colors.tip,
            onClick = { onFilterChange(if (filterLevel == ConsoleMessage.MessageLevel.TIP) null else ConsoleMessage.MessageLevel.TIP) }
        )
        LevelFilterChip(
            icon = { Icon(painterResource(R.drawable.bug), null, modifier = Modifier.size(13.dp)) },
            label = null,
            selected = filterLevel == ConsoleMessage.MessageLevel.DEBUG,
            selectedColor = colors.debug,
            onClick = { onFilterChange(if (filterLevel == ConsoleMessage.MessageLevel.DEBUG) null else ConsoleMessage.MessageLevel.DEBUG) }
        )
    }
}

// ---------------------------------------------------------------------------
// Filter chip
// ---------------------------------------------------------------------------

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
            IconButton(onClick = onClick, modifier = Modifier.size(22.dp)) { icon() }
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

// ---------------------------------------------------------------------------
// Console row (real WebView messages)
// ---------------------------------------------------------------------------

@Composable
private fun ConsoleRow(msg: ConsoleMessage) {
    val colors = consoleColors()
    val level = msg.messageLevel()
    val rowStyle = when (level) {
        ConsoleMessage.MessageLevel.ERROR -> ConsoleRowStyle(
            colors.error.copy(alpha = 0.08f), colors.error,
            colors.error.copy(alpha = 0.7f), R.drawable.exclamation_circle
        )
        ConsoleMessage.MessageLevel.WARNING -> ConsoleRowStyle(
            colors.warn.copy(alpha = 0.08f), colors.warn,
            colors.warn.copy(alpha = 0.7f), R.drawable.alert_triangle_filled
        )
        ConsoleMessage.MessageLevel.TIP -> ConsoleRowStyle(
            colors.tip.copy(alpha = 0.06f), colors.tip,
            colors.tip.copy(alpha = 0.6f), R.drawable.bulb
        )
        ConsoleMessage.MessageLevel.DEBUG -> ConsoleRowStyle(
            Color.Transparent, colors.debug, Color.Transparent, R.drawable.bug
        )
        ConsoleMessage.MessageLevel.LOG -> ConsoleRowStyle(
            Color.Transparent, MaterialTheme.colorScheme.onSurface,
            Color.Transparent, R.drawable.info_circle
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
                    fontFamily = FontFamily.Monospace
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

// ---------------------------------------------------------------------------
// JS input bar
// ---------------------------------------------------------------------------

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
        Text(
            text = ">",
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
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
            ),
            keyboardActions = KeyboardActions(onDone = { onRun() }),
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
                            fontFamily = FontFamily.Monospace
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

// ---------------------------------------------------------------------------
// Helpers / tokens
// ---------------------------------------------------------------------------

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
