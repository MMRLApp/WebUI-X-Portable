package com.dergoogler.mmrl.wx.ui.screens.modules.screens

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dergoogler.mmrl.ext.isScrollingUp
import com.dergoogler.mmrl.ext.none
import com.dergoogler.mmrl.ext.systemBarsPaddingEnd
import com.dergoogler.mmrl.platform.content.LocalModule
import com.dergoogler.mmrl.platform.model.ModId
import com.dergoogler.mmrl.ui.component.BottomSheet
import com.dergoogler.mmrl.ui.component.NavigateUpTopBar
import com.dergoogler.mmrl.ui.component.card.Card
import com.dergoogler.mmrl.ui.component.listItem.dsl.List
import com.dergoogler.mmrl.ui.component.listItem.dsl.component.RadioDialogItem
import com.dergoogler.mmrl.ui.component.listItem.dsl.component.SwitchItem
import com.dergoogler.mmrl.ui.component.listItem.dsl.component.TextEditDialogItem
import com.dergoogler.mmrl.ui.component.listItem.dsl.component.item.Description
import com.dergoogler.mmrl.ui.component.listItem.dsl.component.item.Title
import com.dergoogler.mmrl.ui.providable.LocalNavController
import com.dergoogler.mmrl.webui.model.DexSourceType
import com.dergoogler.mmrl.webui.model.MutableConfig
import com.dergoogler.mmrl.webui.model.WebUIConfig
import com.dergoogler.mmrl.webui.model.WebUIConfig.Companion.asWebUIConfigFlow
import com.dergoogler.mmrl.webui.model.WebUIConfigDexFile
import com.dergoogler.mmrl.wx.R
import com.dergoogler.mmrl.wx.util.asMutableMap
import com.dergoogler.mmrl.wx.util.toDataClass
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import kotlinx.coroutines.launch

private val Context.dexTypeList: List<RadioDialogItem<DexSourceType>>
    get() = listOf(
        RadioDialogItem(
            value = DexSourceType.DEX,
            title = getString(R.string.dex_source_type_dex),
            desc = getString(R.string.dex_source_type_dex_desc)
        ),
        RadioDialogItem(
            value = DexSourceType.APK,
            title = getString(R.string.dex_source_type_apk),
            desc = getString(R.string.dex_source_type_apk_desc)
        ),
    )

@Destination<RootGraph>()
@Composable
fun PluginsScreen(module: LocalModule) {
    val navController = LocalNavController.current
    val modId = module.id

    val scope = rememberCoroutineScope()

    val stableFlow = remember(modId) { modId.asWebUIConfigFlow }
    val config by stableFlow.collectAsStateWithLifecycle(WebUIConfig(modId))

    val listState = rememberLazyListState()

    val isScrollingUp by listState.isScrollingUp()
    val showFab by remember {
        derivedStateOf {
            isScrollingUp
        }
    }

    var addSheet by remember { mutableStateOf(false) }
    if (addSheet) AddBottomSheet(
        onClose = {
            addSheet = false
        },
        onAddClick = { type, path, className ->
            scope.launch {
                val struct = WebUIConfigDexFile(
                    type = type.value,
                    path = path,
                    className = className
                )

                val dexFiles = config.dexFiles.toMutableList().apply {
                    add(struct)
                }

                config.save {
                    "dexFiles" change dexFiles.toList()
                }
            }
        }
    )

    Scaffold(
        topBar = {
            NavigateUpTopBar(
                title = "Plugins",
                subtitle = module.name,
                onBack = { navController.popBackStack() },
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = showFab,
                enter = scaleIn(
                    animationSpec = tween(100),
                    initialScale = 0.8f
                ),
                exit = scaleOut(
                    animationSpec = tween(100),
                    targetScale = 0.8f
                )
            ) {
                FloatingButton {
                    addSheet = true
                }
            }
        },
        contentWindowInsets = WindowInsets.none
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            itemsIndexed(
                items = config.dexFiles,
                key = { index, plugin ->
                    index.toString() + (plugin.path ?: plugin.className ?: "")
                }
            ) { index, plugin ->
                PluginCard(
                    index = index,
                    modId = modId,
                    plugin = plugin
                )
            }
        }
    }
}

@Composable
private fun PluginCard(
    index: Int,
    modId: ModId,
    plugin: WebUIConfigDexFile,
) {
    if (plugin.path == null || plugin.className == null) return

    val stableFlow = remember(modId) { modId.asWebUIConfigFlow }
    val config by stableFlow.collectAsStateWithLifecycle(WebUIConfig(modId))
    val scope = rememberCoroutineScope()

    val context = LocalContext.current

    fun slave(builderAction: MutableConfig<Any?>.(WebUIConfig) -> Unit) {
        scope.launch {
            config.save(builderAction)
        }
    }

    Card {
        val path = plugin.path!!
        val className = plugin.className!!

        List(
            modifier = Modifier
                .relative()
        ) {
            val dexTypeList = remember(plugin) { context.dexTypeList }

            RadioDialogItem(
                selection = plugin.type,
                options = dexTypeList,
                onConfirm = { t ->
                    slave {
                        val updated = it.dexFiles.toMutableList().apply {
                            val old = this[index].asMutableMap()
                            old["type"] = t.value
                            this[index] = old.toDataClass<WebUIConfigDexFile>()
                        }

                        "dexFiles" change updated
                    }
                }
            ) {
                Title(R.string.plugin_type)

                val finding = dexTypeList.find { d -> d.value == plugin.type }
                Description(finding?.title ?: "Unknown")
            }

            TextEditDialogItem(
                value = path,
                onConfirm = { p ->
                    slave {
                        val updated = it.dexFiles.toMutableList().apply {
                            val old = this[index].asMutableMap()
                            old["path"] = p
                            this[index] = old.toDataClass<WebUIConfigDexFile>()
                        }

                        "dexFiles" change updated
                    }
                }
            ) {
                Title(R.string.path)
                Description(path)
            }

            TextEditDialogItem(
                value = className,
                onConfirm = { c ->
                    slave {
                        val updated = it.dexFiles.toMutableList().apply {
                            val old = this[index].asMutableMap()
                            old["className"] = c
                            this[index] = old.toDataClass<WebUIConfigDexFile>()
                        }

                        "dexFiles" change updated
                    }
                }
            ) {
                Title(R.string.class_name)
                Description(className)
            }

            SwitchItem(
                checked = plugin.cache,
                onChange = { c ->
                    slave {
                        val updated = it.dexFiles.toMutableList().apply {
                            val old = this[index].asMutableMap()
                            old["cache"] = c
                            this[index] = old.toDataClass<WebUIConfigDexFile>()
                        }

                        "dexFiles" change updated
                    }
                }
            ) {
                Title(R.string.cache)
            }

            HorizontalDivider(
                thickness = 1.5.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.padding(top = 8.dp)
            )

            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.weight(1f))
                DexRemove {
                    slave {
                        val files = it.dexFiles.toMutableList()
                        if (files.remove(plugin)) {
                            "dexFiles" change files.toList()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddBottomSheet(
    onClose: () -> Unit,
    onAddClick: (type: RadioDialogItem<DexSourceType>, path: String, className: String) -> Unit,
) = BottomSheet(
    onDismissRequest = onClose
) {
    val context = LocalContext.current

    val types = context.dexTypeList

    var selectedType by remember { mutableStateOf(types.first()) }
    var expanded by remember { mutableStateOf(false) }

    var path by remember { mutableStateOf("") }
    var className by remember { mutableStateOf("") }

    val classRegex = Regex("^[a-zA-Z_][a-zA-Z0-9_]*(\\.[a-zA-Z_][a-zA-Z0-9_]*)*$")
    val pathRegex = Regex("^(?!/|[a-zA-Z]:)[^:*?\"<>|]*\\.dex$")

    val isPathValid = path.isNotBlank() && when (selectedType.value) {
        DexSourceType.DEX -> path.endsWith(".dex") && path.matches(pathRegex)
        else -> true
    }

    val isClassNameValid =
        className.matches(classRegex)
    val isFormValid = isPathValid && isClassNameValid

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            modifier = Modifier.padding(bottom = 16.dp),
            text = "New Plugin",
            style = MaterialTheme.typography.headlineMedium
        )

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
        ) {
            OutlinedTextField(
                value = selectedType.title!!,
                onValueChange = { },
                readOnly = true,
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth(),
                trailingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.caret_down_filled),
                        contentDescription = "Dropdown"
                    )
                },
                singleLine = true
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                context.dexTypeList.forEach { type ->
                    DropdownMenuItem(
                        text = {
                            Text(type.title!!)
                        },
                        onClick = {
                            selectedType = type
                            expanded = false
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = path,
            onValueChange = { path = it },
            label = { Text("Path") },
            isError = !isPathValid && path.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        if (!isPathValid && path.isNotBlank()) {
            Text(
                "Path cannot be empty",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 16.dp)
            )
        }

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = className,
            onValueChange = { className = it },
            label = { Text("Class Name") },
            isError = !isClassNameValid && className.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Ascii)
        )
        if (!isClassNameValid && className.isNotBlank()) {
            Text(
                "Invalid class name format",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 16.dp)
            )
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                onAddClick(selectedType, path.trim(), className.trim())
                onClose()
            },
            enabled = isFormValid,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add")
        }
    }
}

@Composable
private fun DexRemove(
    onClick: () -> Unit,
) = FilledTonalButton(
    onClick = onClick,
    contentPadding = PaddingValues(horizontal = 12.dp)
) {
    Icon(
        modifier = Modifier.size(20.dp),
        painter = painterResource(id = R.drawable.trash),
        contentDescription = null
    )

    Spacer(modifier = Modifier.width(6.dp))
    Text(
        text = stringResource(id = R.string.delete)
    )
}

@Composable
private fun FloatingButton(
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }

    FloatingActionButton(
        modifier = Modifier.systemBarsPaddingEnd(),
        interactionSource = interactionSource,
        onClick = onClick,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        containerColor = MaterialTheme.colorScheme.primary
    ) {
        Icon(
            painter = painterResource(id = R.drawable.plus),
            contentDescription = null
        )
    }
}