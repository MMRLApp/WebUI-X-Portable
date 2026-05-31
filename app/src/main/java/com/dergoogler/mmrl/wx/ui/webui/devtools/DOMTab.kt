package com.dergoogler.mmrl.wx.ui.webui.devtools

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.dergoogler.mmrl.wx.R
import dev.mmrlx.compose.ui.theme.MMRLXTheme
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode as JsoupTextNode

private data class DomNode(
    val id: Int,
    val jsoupId: String,
    val parentId: Int?,
    val tag: String,
    val attributes: List<Pair<String, String>>,
    val depth: Int,
    val hasChildren: Boolean,
)

private sealed class RenderEntry {
    abstract val stableId: String
    data class Open(val node: DomNode) : RenderEntry() {
        override val stableId = "open:${node.id}"
    }
    data class Close(val nodeId: Int, val tag: String, val depth: Int) : RenderEntry() {
        override val stableId = "close:$nodeId:$depth"
    }
    data class TextNode(val text: String, val depth: Int, val parentId: Int, val index: Int) : RenderEntry() {
        override val stableId = "text:$parentId:$depth:$index"
    }
}

private sealed class RawEntry {
    data class Element(val node: DomNode) : RawEntry()
    data class Text(val text: String, val depth: Int, val parentId: Int?) : RawEntry()
}

private sealed class DomAction {
    data class EditAttribute(val node: DomNode) : DomAction()
    data class AddAttribute(val node: DomNode) : DomAction()
    data class RemoveAttribute(val node: DomNode, val attrName: String) : DomAction()
    data class EditText(val node: DomNode) : DomAction()
    data class AddChild(val node: DomNode) : DomAction()
    data class RemoveNode(val node: DomNode) : DomAction()
    data class EditStyle(val node: DomNode) : DomAction()
    data class AddClass(val node: DomNode) : DomAction()
    data class RemoveClass(val node: DomNode) : DomAction()
}

private val STAMP_AND_DUMP_JS = """
(function () {
    var id = 0;
    (function stamp(el) {
        el.setAttribute('data-devtools-id', id++);
        for (var i = 0; i < el.children.length; i++) stamp(el.children[i]);
    })(document.documentElement);
    return document.documentElement.outerHTML;
})()
""".trimIndent()

private fun jsNodeById(jsoupId: String) =
    "document.querySelector('[data-devtools-id=\"$jsoupId\"]')"

private fun parseElement(
    element: Element,
    parentId: Int?,
    depth: Int,
    nodes: MutableList<DomNode>,
    rawEntries: MutableList<RawEntry>,
    counter: IntArray,
) {
    val myId = counter[0]++

    val jsoupId = element.attr("data-devtools-id").ifEmpty { myId.toString() }

    val attrs = element.attributes()
        .map { it.key to it.value }
        .filter { it.first != "data-devtools-id" }

    val hasChildren = element.childNodes().any { child ->
        when (child) {
            is Element -> true
            is JsoupTextNode -> child.text().isNotBlank()
            else -> false
        }
    }

    val node = DomNode(
        id = myId,
        jsoupId = jsoupId,
        parentId = parentId,
        tag = element.tagName().lowercase(),
        attributes = attrs,
        depth = depth,
        hasChildren = hasChildren,
    )
    nodes.add(node)
    rawEntries.add(RawEntry.Element(node))

    for (child in element.childNodes()) {
        when (child) {
            is Element -> parseElement(child, myId, depth + 1, nodes, rawEntries, counter)
            is JsoupTextNode -> {
                val trimmed = child.text().trim()
                if (trimmed.isNotEmpty()) {
                    rawEntries.add(
                        RawEntry.Text(
                            text = trimmed.take(120),
                            depth = depth + 1,
                            parentId = myId,
                        )
                    )
                }
            }
        }
    }
}

private fun unwrapJsString(raw: String): String {
    if (raw == "null") return ""
    return try {
        org.json.JSONObject("""{"v":$raw}""").getString("v")
    } catch (_: Exception) {
        raw
    }
}

@Composable
fun DomTab() {
    val webui = LocalWebUI.current

    var nodes by remember { mutableStateOf<List<DomNode>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val collapsedIds = remember { mutableStateListOf<Int>() }
    var rawEntries by remember { mutableStateOf<List<RawEntry>>(emptyList()) }

    var contextNode by remember { mutableStateOf<DomNode?>(null) }
    var activeAction by remember { mutableStateOf<DomAction?>(null) }

    fun runJs(script: String) = webui.runJs(script)

    fun fetchDomFull() {
        isLoading = true
        error = null
        nodes = emptyList()
        rawEntries = emptyList()

        webui.runJs(STAMP_AND_DUMP_JS) { raw ->
            isLoading = false
            if (raw == null || raw == "null") {
                error = "Failed to evaluate DOM."
                return@runJs
            }

            val html = unwrapJsString(raw)
            if (html.isBlank()) {
                error = "Empty HTML returned."
                return@runJs
            }

            try {
                val parsedNodes = mutableListOf<DomNode>()
                val parsedRaw   = mutableListOf<RawEntry>()
                val counter     = intArrayOf(0)

                val doc = Jsoup.parse(html)
                val root = doc.child(0) ?: doc.children().firstOrNull()

                if (root == null) {
                    error = "No root element found."
                    return@runJs
                }

                parseElement(
                    element    = root,
                    parentId   = null,
                    depth      = 0,
                    nodes      = parsedNodes,
                    rawEntries = parsedRaw,
                    counter    = counter,
                )

                nodes      = parsedNodes
                rawEntries = parsedRaw
            } catch (e: Exception) {
                error = "Parse error: ${e.message}"
            }
        }
    }

    LaunchedEffect(Unit) { fetchDomFull() }

    val nodeMap    = remember(nodes) { nodes.associateBy { it.id } }
    val renderList = remember(rawEntries, collapsedIds.toList()) {
        buildRenderList(rawEntries, nodeMap, collapsedIds)
    }

    activeAction?.let { action ->
        when (action) {
            is DomAction.EditAttribute -> EditAttributeDialog(
                node = action.node,
                onDismiss = { activeAction = null },
                onConfirm = { attr, value ->
                    runJs("${jsNodeById(action.node.jsoupId)}.setAttribute('$attr','${value.replace("'", "\\'")}');")
                    activeAction = null; fetchDomFull()
                }
            )
            is DomAction.AddAttribute -> AddAttributeDialog(
                onDismiss = { activeAction = null },
                onConfirm = { attr, value ->
                    runJs("${jsNodeById(action.node.jsoupId)}.setAttribute('$attr','${value.replace("'", "\\'")}');")
                    activeAction = null; fetchDomFull()
                }
            )
            is DomAction.EditText -> EditTextDialog(
                node = action.node,
                onDismiss = { activeAction = null },
                onConfirm = { newText ->
                    runJs("""
                        (function(){
                            var el=${jsNodeById(action.node.jsoupId)};
                            var tn=Array.from(el.childNodes).find(function(n){return n.nodeType===3;});
                            if(tn)tn.nodeValue='${newText.replace("'","\\'")}';
                            else el.insertBefore(document.createTextNode('${newText.replace("'","\\'")}'),el.firstChild);
                        })()
                    """.trimIndent())
                    activeAction = null; fetchDomFull()
                }
            )
            is DomAction.AddChild -> AddChildDialog(
                onDismiss = { activeAction = null },
                onConfirm = { tag, text ->
                    runJs("""
                        (function(){
                            var el=${jsNodeById(action.node.jsoupId)};
                            var child=document.createElement('$tag');
                            if('$text'.length>0)child.textContent='${text.replace("'","\\'")}';
                            el.appendChild(child);
                        })()
                    """.trimIndent())
                    activeAction = null; fetchDomFull()
                }
            )
            is DomAction.RemoveNode -> ConfirmDialog(
                title = "Remove <${action.node.tag}>?",
                message = "This will remove the element and all its children from the DOM.",
                onDismiss = { activeAction = null },
                onConfirm = {
                    runJs("${jsNodeById(action.node.jsoupId)}.remove();")
                    activeAction = null; fetchDomFull()
                }
            )
            is DomAction.EditStyle -> EditStyleDialog(
                node = action.node,
                onDismiss = { activeAction = null },
                onConfirm = { css ->
                    runJs("${jsNodeById(action.node.jsoupId)}.style.cssText='${css.replace("'","\\'")}';")
                    activeAction = null; fetchDomFull()
                }
            )
            is DomAction.AddClass -> AddClassDialog(
                onDismiss = { activeAction = null },
                onConfirm = { cls ->
                    runJs("${jsNodeById(action.node.jsoupId)}.classList.add('${cls.replace("'","\\'")}');")
                    activeAction = null; fetchDomFull()
                }
            )
            is DomAction.RemoveClass -> RemoveClassDialog(
                node = action.node,
                onDismiss = { activeAction = null },
                onConfirm = { cls ->
                    runJs("${jsNodeById(action.node.jsoupId)}.classList.remove('${cls.replace("'","\\'")}');")
                    activeAction = null; fetchDomFull()
                }
            )
            else -> {}
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MMRLXTheme.colors.card)
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(onClick = { fetchDomFull() }, modifier = Modifier.size(28.dp)) {
                Icon(
                    painter = painterResource(com.dergoogler.mmrl.webui.R.drawable.refresh),
                    contentDescription = "Refresh DOM",
                    modifier = Modifier.size(16.dp),
                    tint = MMRLXTheme.colors.primary
                )
            }
            Text(
                text = "${nodes.size} nodes",
                style = MMRLXTheme.typography.labelSmall.copy(fontSize = 10.sp, fontFamily = FontFamily.Monospace),
                color = MMRLXTheme.colors.cardForeground
            )
        }

        HorizontalDivider(thickness = 0.5.dp, color = MMRLXTheme.colors.border)

        when {
            isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            }
            error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(error!!, style = MMRLXTheme.typography.bodySmall, color = MMRLXTheme.colors.destructive)
            }
            renderList.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No DOM nodes found.", style = MMRLXTheme.typography.bodySmall, color = MMRLXTheme.colors.foreground)
            }
            else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(renderList, key = { it.stableId }) { entry ->
                    RenderEntryRow(
                        entry = entry,
                        collapsedIds = collapsedIds,
                        onToggle = { id ->
                            if (collapsedIds.contains(id)) collapsedIds.remove(id)
                            else collapsedIds.add(id)
                        },
                        onLongPress = { node -> contextNode = node }
                    )
                    HorizontalDivider(thickness = 0.3.dp, color = MMRLXTheme.colors.border.copy(alpha = 0.3f))
                }
            }
        }
    }

    contextNode?.let { node ->
        DomContextMenu(
            node = node,
            onDismiss = { contextNode = null },
            onAction = { action -> contextNode = null; activeAction = action }
        )
    }
}

private fun buildRenderList(
    raw: List<RawEntry>,
    nodeMap: Map<Int, DomNode>,
    collapsedIds: List<Int>,
): List<RenderEntry> {
    if (raw.isEmpty()) return emptyList()
    val result = mutableListOf<RenderEntry>()
    val stack = ArrayDeque<DomNode>()
    val textCounters = mutableMapOf<Long, Int>()

    for (entry in raw) {
        val currentDepth = when (entry) {
            is RawEntry.Element -> entry.node.depth
            is RawEntry.Text    -> entry.depth
        }
        while (stack.isNotEmpty() && stack.last().depth >= currentDepth) {
            val closing = stack.removeLast()
            if (!collapsedIds.contains(closing.id))
                result.add(RenderEntry.Close(closing.id, closing.tag, closing.depth))
        }
        when (entry) {
            is RawEntry.Element -> {
                val node = entry.node
                if (isAncestorCollapsed(node.parentId, nodeMap, collapsedIds)) continue
                result.add(RenderEntry.Open(node))
                if (node.hasChildren && !collapsedIds.contains(node.id)) stack.addLast(node)
            }
            is RawEntry.Text -> {
                if (entry.parentId != null && collapsedIds.contains(entry.parentId)) continue
                if (entry.parentId != null && isAncestorCollapsed(entry.parentId, nodeMap, collapsedIds)) continue
                val pid = entry.parentId ?: -1
                val counterKey = (pid.toLong() shl 32) or (entry.depth.toLong() and 0xFFFFFFFFL)
                val idx = textCounters.getOrDefault(counterKey, 0)
                textCounters[counterKey] = idx + 1
                result.add(RenderEntry.TextNode(entry.text, entry.depth, pid, idx))
            }
        }
    }
    while (stack.isNotEmpty()) {
        val closing = stack.removeLast()
        if (!collapsedIds.contains(closing.id))
            result.add(RenderEntry.Close(closing.id, closing.tag, closing.depth))
    }
    return result
}

private fun isAncestorCollapsed(parentId: Int?, nodeMap: Map<Int, DomNode>, collapsedIds: List<Int>): Boolean {
    var id = parentId
    while (id != null) {
        if (collapsedIds.contains(id)) return true
        id = nodeMap[id]?.parentId
    }
    return false
}

@Composable
private fun DomContextMenu(node: DomNode, onDismiss: () -> Unit, onAction: (DomAction) -> Unit) {
    val hasClass = node.attributes.any { it.first == "class" && it.second.isNotBlank() }
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(modifier = Modifier.width(260.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "<${node.tag}>",
                        style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                HorizontalDivider()
                val items = buildList {
                    add("Edit attribute"       to DomAction.EditAttribute(node))
                    add("Add attribute"        to DomAction.AddAttribute(node))
                    node.attributes.forEach { (name, _) ->
                        add("Remove attr: $name" to DomAction.RemoveAttribute(node, name))
                    }
                    add("Edit inline style"    to DomAction.EditStyle(node))
                    add("Add class"            to DomAction.AddClass(node))
                    if (hasClass) add("Remove class" to DomAction.RemoveClass(node))
                    add("Edit text content"    to DomAction.EditText(node))
                    add("Append child element" to DomAction.AddChild(node))
                    add("Remove node"          to DomAction.RemoveNode(node))
                }
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    items.forEachIndexed { index, (label, action) ->
                        val isDestructive = action is DomAction.RemoveNode || action is DomAction.RemoveAttribute
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                    color = if (isDestructive) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.onSurface
                                )
                            },
                            onClick = { onAction(action) }
                        )
                        if (index < items.lastIndex)
                            HorizontalDivider(thickness = 0.3.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }
}

@Composable
private fun EditAttributeDialog(node: DomNode, onDismiss: () -> Unit, onConfirm: (attr: String, value: String) -> Unit) {
    var selectedAttr by remember { mutableStateOf(node.attributes.firstOrNull()?.first ?: "") }
    var attrValue by remember { mutableStateOf(node.attributes.firstOrNull { it.first == selectedAttr }?.second ?: "") }
    DevToolsDialog(title = "Edit Attribute", onDismiss = onDismiss, onConfirm = { onConfirm(selectedAttr, attrValue) }) {
        if (node.attributes.isEmpty()) {
            Text("No attributes on this element.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Text("Attribute", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                node.attributes.forEach { (name, _) ->
                    val selected = name == selectedAttr
                    Text(
                        text = name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent, RoundedCornerShape(4.dp))
                            .combinedClickable(onClick = {
                                selectedAttr = name
                                attrValue = node.attributes.first { it.first == name }.second
                            })
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                        color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            DevToolsTextField(label = "Value", value = attrValue, onValueChange = { attrValue = it })
        }
    }
}

@Composable
private fun AddAttributeDialog(onDismiss: () -> Unit, onConfirm: (attr: String, value: String) -> Unit) {
    var attr by remember { mutableStateOf("") }
    var value by remember { mutableStateOf("") }
    DevToolsDialog(title = "Add Attribute", onDismiss = onDismiss, onConfirm = { onConfirm(attr, value) }) {
        DevToolsTextField(label = "Attribute name", value = attr, onValueChange = { attr = it })
        Spacer(Modifier.height(8.dp))
        DevToolsTextField(label = "Value", value = value, onValueChange = { value = it })
    }
}

@Composable
private fun EditTextDialog(node: DomNode, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    DevToolsDialog(title = "Edit Text Content", onDismiss = onDismiss, onConfirm = { onConfirm(text) }) {
        DevToolsTextField(label = "Text", value = text, onValueChange = { text = it }, singleLine = false)
    }
}

@Composable
private fun AddChildDialog(onDismiss: () -> Unit, onConfirm: (tag: String, text: String) -> Unit) {
    var tag by remember { mutableStateOf("div") }
    var text by remember { mutableStateOf("") }
    DevToolsDialog(title = "Append Child", onDismiss = onDismiss, onConfirm = { onConfirm(tag, text) }) {
        DevToolsTextField(label = "Tag name", value = tag, onValueChange = { tag = it })
        Spacer(Modifier.height(8.dp))
        DevToolsTextField(label = "Text content (optional)", value = text, onValueChange = { text = it })
    }
}

@Composable
private fun EditStyleDialog(node: DomNode, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    val existing = node.attributes.firstOrNull { it.first == "style" }?.second ?: ""
    var css by remember { mutableStateOf(existing) }
    DevToolsDialog(title = "Edit Inline Style", onDismiss = onDismiss, onConfirm = { onConfirm(css) }) {
        DevToolsTextField(label = "CSS (e.g. color:red; font-size:14px)", value = css, onValueChange = { css = it }, singleLine = false)
    }
}

@Composable
private fun AddClassDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var cls by remember { mutableStateOf("") }
    DevToolsDialog(title = "Add Class", onDismiss = onDismiss, onConfirm = { onConfirm(cls) }) {
        DevToolsTextField(label = "Class name", value = cls, onValueChange = { cls = it })
    }
}

@Composable
private fun RemoveClassDialog(node: DomNode, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    val classes = remember {
        node.attributes.firstOrNull { it.first == "class" }?.second
            ?.split(" ")?.filter { it.isNotBlank() } ?: emptyList()
    }
    var selected by remember { mutableStateOf(classes.firstOrNull() ?: "") }
    DevToolsDialog(title = "Remove Class", onDismiss = onDismiss, onConfirm = { onConfirm(selected) }) {
        if (classes.isEmpty()) {
            Text("No classes on this element.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                classes.forEach { cls ->
                    Text(
                        text = cls,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (cls == selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent, RoundedCornerShape(4.dp))
                            .combinedClickable(onClick = { selected = cls })
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                        color = if (cls == selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun ConfirmDialog(title: String, message: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    TextButton(onClick = onConfirm) { Text("Remove", color = MaterialTheme.colorScheme.error) }
                }
            }
        }
    }
}

@Composable
private fun DevToolsDialog(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp).width(280.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                HorizontalDivider()
                content()
                HorizontalDivider()
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    TextButton(onClick = onConfirm) { Text("Apply") }
                }
            }
        }
    }
}

@Composable
private fun DevToolsTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    singleLine: Boolean = true,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp))
                .padding(horizontal = 10.dp, vertical = 7.dp)
                .then(if (!singleLine) Modifier.height(80.dp) else Modifier),
            textStyle = MaterialTheme.typography.bodySmall.copy(
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurface
            )
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RenderEntryRow(
    entry: RenderEntry,
    collapsedIds: List<Int>,
    onToggle: (Int) -> Unit,
    onLongPress: (DomNode) -> Unit,
) {
    val darkMode = MMRLXTheme.colors.isDark
    val indentPerLevel = 12.dp

    val tagColor       = Color(if (darkMode) 0xFF7EE787 else 0xFF166534)
    val attrNameColor  = Color(if (darkMode) 0xFFFFA657 else 0xFF9A6700)
    val attrValueColor = Color(if (darkMode) 0xFFA5D6FF else 0xFF1155A3)
    val punctColor     = Color(if (darkMode) 0xFFC9D1D9 else 0xFF374151)
    val textNodeColor  = Color(if (darkMode) 0xFFA5D6FF else 0xFF1155A3)

    when (entry) {
        is RenderEntry.Open -> {
            val node = entry.node
            val isCollapsed = collapsedIds.contains(node.id)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = { if (node.hasChildren) onToggle(node.id) },
                        onLongClick = { onLongPress(node) }
                    )
                    .padding(start = indentPerLevel * node.depth + 4.dp, end = 8.dp, top = 3.dp, bottom = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (node.hasChildren) {
                    Icon(
                        painter = painterResource(if (isCollapsed) R.drawable.chevron_right else R.drawable.chevron_down),
                        contentDescription = null,
                        modifier = Modifier.size(10.dp),
                        tint = punctColor
                    )
                } else {
                    Spacer(modifier = Modifier.size(10.dp))
                }
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = punctColor)) { append("<") }
                        withStyle(SpanStyle(color = tagColor, fontFamily = FontFamily.Monospace)) { append(node.tag) }
                        node.attributes.forEach { (name, value) ->
                            append(" ")
                            withStyle(SpanStyle(color = attrNameColor, fontFamily = FontFamily.Monospace)) { append(name) }
                            withStyle(SpanStyle(color = punctColor)) { append("=\"") }
                            withStyle(SpanStyle(color = attrValueColor, fontFamily = FontFamily.Monospace)) {
                                append(if (value.length > 60) value.take(60) + "…" else value)
                            }
                            withStyle(SpanStyle(color = punctColor)) { append("\"") }
                        }
                        when {
                            !node.hasChildren -> withStyle(SpanStyle(color = punctColor)) { append("/>") }
                            isCollapsed -> {
                                withStyle(SpanStyle(color = punctColor)) { append(">…</") }
                                withStyle(SpanStyle(color = tagColor, fontFamily = FontFamily.Monospace)) { append(node.tag) }
                                withStyle(SpanStyle(color = punctColor)) { append(">") }
                            }
                            else -> withStyle(SpanStyle(color = punctColor)) { append(">") }
                        }
                    },
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    softWrap = true
                )
            }
        }
        is RenderEntry.Close -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = indentPerLevel * entry.depth + 4.dp + 14.dp, end = 8.dp, top = 3.dp, bottom = 3.dp)
            ) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = punctColor)) { append("</") }
                        withStyle(SpanStyle(color = tagColor, fontFamily = FontFamily.Monospace)) { append(entry.tag) }
                        withStyle(SpanStyle(color = punctColor)) { append(">") }
                    },
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp)
                )
            }
        }
        is RenderEntry.TextNode -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = indentPerLevel * entry.depth + 4.dp + 14.dp, end = 8.dp, top = 2.dp, bottom = 2.dp)
            ) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = textNodeColor, fontFamily = FontFamily.Monospace)) {
                            append(if (entry.text.length > 120) entry.text.take(120) + "…" else entry.text)
                        }
                    },
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    softWrap = true
                )
            }
        }
    }
}