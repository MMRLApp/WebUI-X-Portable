package com.dergoogler.mmrl.wx.ui.screens.modules.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dergoogler.mmrl.wx.R
import com.dergoogler.mmrl.wx.model.module.DEFAULT_CSP
import com.dergoogler.mmrl.wx.ui.component.LocalModule
import com.dergoogler.mmrl.wx.ui.component.ModuleScope
import com.dergoogler.mmrl.wx.ui.component.NavigateUpToolbar
import com.dergoogler.mmrl.wx.ui.providable.LocalDestinationsNavigator
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import dev.mmrlx.compose.layout.outlinedCard
import dev.mmrlx.compose.ui.Badge
import dev.mmrlx.compose.ui.BadgeVariant
import dev.mmrlx.compose.ui.Separator
import dev.mmrlx.compose.ui.Text
import dev.mmrlx.compose.ui.button.Button
import dev.mmrlx.compose.ui.button.ButtonSize
import dev.mmrlx.compose.ui.button.ButtonVariant
import dev.mmrlx.compose.ui.dialog.Content
import dev.mmrlx.compose.ui.dialog.Dialog
import dev.mmrlx.compose.ui.dialog.Footer
import dev.mmrlx.compose.ui.dialog.Title
import dev.mmrlx.compose.ui.icon.Icon
import dev.mmrlx.compose.ui.icon.IconButton
import dev.mmrlx.compose.ui.scaffold.Scaffold
import dev.mmrlx.compose.ui.text.Input
import dev.mmrlx.compose.ui.theme.MMRLXTheme
import dev.mmrlx.compose.ui.toolbar.ToolbarTitle
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

private const val CSP = "Content-Security-Policy"

private val SUGGESTED_HEADERS = listOf(
    CSP,
    "Cache-Control",
    "X-Frame-Options",
    "X-Content-Type-Options",
    "Access-Control-Allow-Origin",
    "Referrer-Policy",
    "Permissions-Policy",
)

private val HEADER_NAME_REGEX = Regex("^[!#$%&'*+.^_`|~0-9A-Za-z-]+$")

private data class HeaderEntry(val name: String, val value: String)

private data class HeaderRule(val pattern: String, val headers: List<HeaderEntry>)

/** Editable header row; holds its own input states and is keyed by identity in the editor. */
private class HeaderDraft(name: String = "", value: String = "") {
    val name = TextFieldState(name)
    val value = TextFieldState(value)

    val trimmedName get() = name.text.toString().trim()
}

/** index == null means "new rule" */
private data class EditTarget(val index: Int?, val rule: HeaderRule)

private fun JsonObject.toRules(): List<HeaderRule> = entries.map { (pattern, element) ->
    val headers = (element as? JsonObject)?.entries?.map { (name, value) ->
        HeaderEntry(name, (value as? JsonPrimitive)?.content ?: value.toString())
    } ?: emptyList()

    HeaderRule(pattern, headers)
}

private fun List<HeaderRule>.toJson(): JsonObject = buildJsonObject {
    forEach { rule ->
        putJsonObject(rule.pattern) {
            rule.headers.forEach { put(it.name, it.value) }
        }
    }
}

@Destination<RootGraph>()
@Composable
fun ResponseHeadersScreen(moduleId: String) {
    ModuleScope(moduleId) {
        ResponseHeadersScreenContent()
    }
}

@Composable
fun ResponseHeadersScreenContent() {
    val module = LocalModule.current
    val navigator = LocalDestinationsNavigator.current
    val config = remember { module.webrootConfig }

    // Order matters: after an exact filename match, the first matching glob wins.
    val rules = remember {
        mutableStateListOf<HeaderRule>().apply {
            addAll(config.get("responseHeaders", JsonObject(emptyMap())).toRules())
        }
    }

    var editing by remember { mutableStateOf<EditTarget?>(null) }
    var deleting by remember { mutableStateOf<Int?>(null) }

    // If config.set doesn't accept a JsonObject, change only this line (e.g. pass a Map).
    fun save() {
        config["responseHeaders"] = rules.toList().toJson()
    }

    Scaffold(
        toolbar = {
            NavigateUpToolbar(
                title = {
                    ToolbarTitle(
                        title = stringResource(R.string.webui_config_response_headers),
                        subtitle = module.name
                    )
                },
                onBack = { navigator.popBackStack() },
            )
        },
        contentWindowInsets = WindowInsets.systemBars,
    ) {
        Column(
            modifier = Modifier
                .scaffoldHazeSource()
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .scaffoldPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            InfoCard()

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(R.string.rules),
                    style = MMRLXTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Badge(text = rules.size.toString(), variant = BadgeVariant.Secondary)

                Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.End) {
                    Button(
                        onClick = { editing = EditTarget(null, HeaderRule("", emptyList())) },
                        size = ButtonSize.Sm,
                    ) {
                        Icon(
                            painterResource(R.drawable.plus),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            stringResource(R.string.add_rule),
                            modifier = Modifier.padding(start = 6.dp)
                        )
                    }
                }
            }

            if (rules.isEmpty()) {
                EmptyState()
            }

            rules.forEachIndexed { index, rule ->
                RuleCard(
                    position = index + 1,
                    rule = rule,
                    canMoveUp = index > 0,
                    canMoveDown = index < rules.lastIndex,
                    onEdit = { editing = EditTarget(index, rule) },
                    onDelete = { deleting = index },
                    onMoveUp = {
                        rules.add(index - 1, rules.removeAt(index))
                        save()
                    },
                    onMoveDown = {
                        rules.add(index + 1, rules.removeAt(index))
                        save()
                    },
                )
            }
        }
    }

    editing?.let { target ->
        RuleEditorDialog(
            initial = target.rule,
            takenPatterns = rules
                .filterIndexed { i, _ -> i != target.index }
                .map { it.pattern },
            onDismiss = { editing = null },
            onSave = { result ->
                if (target.index == null) rules.add(result) else rules[target.index] = result
                save()
                editing = null
            },
        )
    }

    deleting?.let { index ->
        Dialog(
            onDismissRequest = {
                deleting = null
            }
        ) {
            Title {
                Text(stringResource(R.string.delete_rule))
            }

            Content {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = rules.getOrNull(index)?.pattern.orEmpty(),
                        style = MMRLXTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace,
                    )
                    Text(
                        text = stringResource(R.string.dialog_delete_rule_no_undone),
                        style = MMRLXTheme.typography.bodySmall,
                        color = MMRLXTheme.colors.mutedForeground,
                    )
                }
            }

            Footer {
                Button(
                    onClick = {
                        deleting = null
                    },
                    variant = ButtonVariant.Outline,
                ) {
                    Text(stringResource(R.string.cancel))
                }
                Button(
                    onClick = {
                        if (index in rules.indices) {
                            rules.removeAt(index)
                            save()
                        }
                        deleting = null
                    },
                    variant = ButtonVariant.Destructive,
                ) {
                    Text(stringResource(R.string.delete))
                }
            }
        }
    }
}

@Composable
private fun InfoCard() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .outlinedCard()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.info_circle),
            contentDescription = null,
            tint = MMRLXTheme.colors.mutedForeground,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(18.dp),
        )

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = stringResource(R.string.how_rules_work),
                style = MMRLXTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            InfoLine(stringResource(R.string.how_rules_work_info_1))
            InfoLine(stringResource(R.string.how_rules_work_info_2, CSP))
            InfoLine(stringResource(R.string.how_rules_work_info_3))
        }
    }
}

@Composable
private fun InfoLine(text: String) {
    Text(
        text = text,
        style = MMRLXTheme.typography.bodySmall,
        color = MMRLXTheme.colors.mutedForeground,
    )
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .outlinedCard()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.shield),
            contentDescription = null,
            tint = MMRLXTheme.colors.mutedForeground,
            modifier = Modifier.size(32.dp),
        )
        Text(
            text = stringResource(R.string.no_rules_yet),
            style = MMRLXTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = stringResource(R.string.only_the_default_csp_is_applied_to_html_files),
            style = MMRLXTheme.typography.bodySmall,
            color = MMRLXTheme.colors.mutedForeground,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun RuleCard(
    position: Int,
    rule: HeaderRule,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
    val colors = MMRLXTheme.colors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .outlinedCard(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Badge(text = "#$position", variant = BadgeVariant.Secondary)
            Text(
                text = rule.pattern,
                modifier = Modifier.weight(1f),
                style = MMRLXTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (rule.headers.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_headers),
                    style = MMRLXTheme.typography.bodySmall,
                    color = colors.mutedForeground,
                )
            }

            rule.headers.forEach { header ->
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = header.name,
                            style = MMRLXTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (header.value.isEmpty()) {
                            Badge(
                                text = stringResource(R.string.removed),
                                variant = BadgeVariant.Destructive
                            )
                        }
                    }
                    if (header.value.isNotEmpty()) {
                        Text(
                            text = header.value,
                            style = MMRLXTheme.typography.bodySmall,
                            color = colors.mutedForeground,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }

        Separator()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onMoveUp, enabled = canMoveUp) {
                Icon(
                    painterResource(dev.mmrlx.ui.R.drawable.chevron_up),
                    contentDescription = "Move up",
                    modifier = Modifier.size(20.dp),
                )
            }
            IconButton(onClick = onMoveDown, enabled = canMoveDown) {
                Icon(
                    painterResource(dev.mmrlx.ui.R.drawable.chevron_down),
                    contentDescription = "Move down",
                    modifier = Modifier.size(20.dp),
                )
            }

            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = onEdit) {
                    Icon(
                        painterResource(R.drawable.pencil),
                        contentDescription = "Edit",
                        modifier = Modifier.size(20.dp),
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        painterResource(R.drawable.trash),
                        contentDescription = "Delete",
                        tint = colors.destructive,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun RuleEditorDialog(
    initial: HeaderRule,
    takenPatterns: List<String>,
    onDismiss: () -> Unit,
    onSave: (HeaderRule) -> Unit,
) {
    val colors = MMRLXTheme.colors
    val pattern = remember { TextFieldState(initial.pattern) }
    val drafts = remember {
        mutableStateListOf<HeaderDraft>().apply {
            addAll(initial.headers.map { HeaderDraft(it.name, it.value) })
        }
    }

    val trimmedPattern = pattern.text.toString().trim()

    val patternError = if (trimmedPattern in takenPatterns) {
        stringResource(R.string.a_rule_with_this_pattern_already_exists)
    } else null

    @Composable
    fun nameError(draft: HeaderDraft): String? {
        val name = draft.trimmedName
        return when {
            name.isEmpty() -> null
            !HEADER_NAME_REGEX.matches(name) -> stringResource(R.string.invalid_header_name)
            drafts.any { it !== draft && it.trimmedName.equals(name, ignoreCase = true) } ->
                stringResource(R.string.duplicate_header)

            else -> null
        }
    }

    @Composable
    fun valueError(draft: HeaderDraft): String? =
        if (draft.value.text.any { it == '\n' || it == '\r' }) stringResource(R.string.no_line_breaks_allowed) else null

    val isValid = trimmedPattern.isNotEmpty() &&
            patternError == null &&
            drafts.all { it.trimmedName.isNotEmpty() && nameError(it) == null && valueError(it) == null }

    Dialog(
        onDismissRequest = onDismiss,
    ) {
        Title {
            Text(
                if (initial.pattern.isEmpty()) stringResource(R.string.add_rule) else stringResource(
                    R.string.edit_rule
                )
            )
        }

        Content {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Field(
                    label = stringResource(R.string.file_name_or_glob),
                    hint = stringResource(R.string.e_g_index_html_or_html_htm),
                    error = patternError,
                ) {
                    Input(
                        state = pattern,
                        isError = patternError != null,
                        lineLimits = TextFieldLineLimits.SingleLine,
                        placeholder = { Text("*.html") },
                    )
                }

                Separator()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.headers),
                        style = MMRLXTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Badge(text = drafts.size.toString(), variant = BadgeVariant.Secondary)
                }

                if (drafts.isEmpty()) {
                    Text(
                        text = stringResource(R.string.no_headers_yet_add_one_below_or_pick_a_suggestion),
                        style = MMRLXTheme.typography.bodySmall,
                        color = colors.mutedForeground,
                    )
                }

                drafts.forEach { draft ->
                    key(draft) {
                        val nErr = nameError(draft)
                        val vErr = valueError(draft)

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .outlinedCard()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Field(
                                    label = stringResource(R.string.name),
                                    error = nErr,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Input(
                                        state = draft.name,
                                        isError = nErr != null,
                                        lineLimits = TextFieldLineLimits.SingleLine,
                                        placeholder = { Text("X-Custom-Header") },
                                    )
                                }

                                IconButton(onClick = { drafts.remove(draft) }) {
                                    Icon(
                                        painterResource(R.drawable.x),
                                        contentDescription = stringResource(R.string.remove_header),
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }

                            Field(
                                label = stringResource(R.string.value),
                                error = vErr,
                                hint = if (draft.trimmedName.equals(CSP, ignoreCase = true)) {
                                    stringResource(R.string.empty_send_no_csp_for_these_files)
                                } else null,
                            ) {
                                Input(
                                    state = draft.value,
                                    isError = vErr != null,
                                    lineLimits = TextFieldLineLimits.MultiLine(
                                        minHeightInLines = 1,
                                        maxHeightInLines = 4,
                                    ),
                                )
                            }
                        }
                    }
                }

                Button(
                    onClick = { drafts.add(HeaderDraft()) },
                    variant = ButtonVariant.Outline,
                    size = ButtonSize.Sm,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        painterResource(R.drawable.plus),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(stringResource(R.string.add_header), modifier = Modifier.padding(start = 6.dp))
                }

                // Quick-add chips for headers that aren't already in this rule
                val unused = SUGGESTED_HEADERS.filter { s ->
                    drafts.none { it.trimmedName.equals(s, ignoreCase = true) }
                }

                if (unused.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(R.string.suggestions),
                            style = MMRLXTheme.typography.labelMedium,
                            color = colors.mutedForeground,
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            unused.forEach { name ->
                                Badge(
                                    modifier = Modifier.clickable {
                                        drafts.add(
                                            HeaderDraft(name, if (name == CSP) DEFAULT_CSP else "")
                                        )
                                    },
                                    text = name,
                                    variant = BadgeVariant.Outline,
                                )
                            }
                        }
                    }
                }
            }
        }

        Footer {
            Button(
                onClick = onDismiss,
                variant = ButtonVariant.Outline
            ) {
                Text(stringResource(R.string.cancel))
            }
            Button(
                enabled = isValid,
                onClick = {
                    onSave(
                        HeaderRule(
                            pattern = trimmedPattern,
                            headers = drafts.map {
                                HeaderEntry(it.trimmedName, it.value.text.toString())
                            },
                        )
                    )
                },
            ) {
                Text(stringResource(R.string.save))
            }
        }
    }
}

@Composable
private fun Field(
    label: String,
    modifier: Modifier = Modifier,
    hint: String? = null,
    error: String? = null,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            style = MMRLXTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
        )
        content()
        when {
            error != null -> Text(
                text = error,
                style = MMRLXTheme.typography.bodySmall,
                color = MMRLXTheme.colors.destructive,
            )

            hint != null -> Text(
                text = hint,
                style = MMRLXTheme.typography.bodySmall,
                color = MMRLXTheme.colors.mutedForeground,
            )
        }
    }
}
