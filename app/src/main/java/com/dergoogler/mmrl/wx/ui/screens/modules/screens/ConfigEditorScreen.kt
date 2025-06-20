package com.dergoogler.mmrl.wx.ui.screens.modules.screens

import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.dergoogler.mmrl.ext.moshi.moshi
import com.dergoogler.mmrl.ext.none
import com.dergoogler.mmrl.ext.nullable
import com.dergoogler.mmrl.ext.shareFile
import com.dergoogler.mmrl.ext.shareText
import com.dergoogler.mmrl.platform.content.LocalModule
import com.dergoogler.mmrl.platform.file.SuFile
import com.dergoogler.mmrl.platform.model.ModId.Companion.moduleDir
import com.dergoogler.mmrl.platform.model.ModId.Companion.webrootDir
import com.dergoogler.mmrl.ui.component.BottomSheet
import com.dergoogler.mmrl.ui.component.NavigateUpTopBar
import com.dergoogler.mmrl.ui.component.card.Card
import com.dergoogler.mmrl.ui.component.dialog.RadioOptionItem
import com.dergoogler.mmrl.ui.component.listItem.ListButtonItem
import com.dergoogler.mmrl.ui.component.listItem.ListEditTextItem
import com.dergoogler.mmrl.ui.component.listItem.ListEditTextSwitchItem
import com.dergoogler.mmrl.ui.component.listItem.ListHeader
import com.dergoogler.mmrl.ui.component.listItem.ListItemDefaults
import com.dergoogler.mmrl.ui.component.listItem.ListRadioCheckItem
import com.dergoogler.mmrl.ui.component.listItem.ListSwitchItem
import com.dergoogler.mmrl.ui.providable.LocalNavController
import com.dergoogler.mmrl.webui.model.WebUIConfigDexFile
import com.dergoogler.mmrl.wx.R
import com.dergoogler.mmrl.wx.util.getBoolProp
import com.dergoogler.mmrl.wx.util.getProp
import com.dergoogler.mmrl.wx.util.getPropOrNull
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Types
import kotlinx.coroutines.launch

private val mapAdapter: JsonAdapter<Map<String, Any>> = moshi.adapter<Map<String, Any>>(
    Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
).indent("  ")

private val webuiDexFileListAdapter: JsonAdapter<List<WebUIConfigDexFile>> = moshi.adapter(
    Types.newParameterizedType(List::class.java, WebUIConfigDexFile::class.java)
)

private val Context.interceptorList: List<RadioOptionItem<String?>>
    get() = listOf(
        RadioOptionItem(
            value = "native",
            title = getString(R.string.controlled_by_native)
        ),
        RadioOptionItem(
            value = "javascript",
            title = getString(R.string.controlled_by_javascript)
        ),
    )

@Composable
fun ConfigEditorScreen(module: LocalModule) {
    val navController = LocalNavController.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val modId = module.id

    val webuiConfigFile: SuFile = remember {
        var wfile = modId.webrootDir.fromPaths("config.json", "config.mmrl.json")
        if (wfile == null) {
            wfile = SuFile(modId.webrootDir, "config.json")
            wfile.writeText("{}")
        }
        wfile
    }

    val moduleConfigFile: SuFile = remember {
        var mfile = modId.moduleDir.fromPaths("config.json", "config.mmrl.json")
        if (mfile == null) {
            mfile = SuFile(modId.moduleDir, "config.json")
            mfile.writeText("{}")
        }
        mfile
    }

    var exportBottomSheet by remember { mutableStateOf(false) }
    if (exportBottomSheet) ExportBottomSheet(
        onClose = { exportBottomSheet = false },
        onModuleExport = {
            context.shareText(moduleConfigFile.readText())
        },
        onConfigExport = {
            context.shareText(webuiConfigFile.readText())
        }
    )


    var webuiConfigMap by remember { mutableStateOf<Map<String, Any>?>(null) }
    var moduleConfigMap by remember { mutableStateOf<Map<String, Any>?>(null) }

    LaunchedEffect(webuiConfigFile) {
        coroutineScope.launch {
            webuiConfigMap = try {
                mapAdapter.fromJson(webuiConfigFile.readText()) ?: emptyMap()
            } catch (e: Exception) {
                emptyMap()
            }
        }
    }


    LaunchedEffect(moduleConfigFile) {
        coroutineScope.launch {
            moduleConfigMap = try {
                mapAdapter.fromJson(moduleConfigFile.readText()) ?: emptyMap()
            } catch (e: Exception) {
                emptyMap()
            }
        }
    }

    fun config(key: String, value: Any) {
        val currentConfig = webuiConfigMap ?: return

        val updatedConfig = currentConfig.toMutableMap().apply {
            this[key] = value
        }

        webuiConfigMap = updatedConfig

        coroutineScope.launch {
            val json = mapAdapter.toJson(updatedConfig)
            webuiConfigFile.writeText(json)
        }
    }

    fun module(key: String, value: Any) {
        val currentConfig = moduleConfigMap ?: return

        val updatedConfig = currentConfig.toMutableMap().apply {
            this[key] = value
        }

        moduleConfigMap = updatedConfig

        coroutineScope.launch {
            val json = mapAdapter.toJson(updatedConfig)
            moduleConfigFile.writeText(json)
        }
    }

    Scaffold(
        topBar = {
            NavigateUpTopBar(
                title = "Config",
                subtitle = module.name,
                onBack = { navController.popBackStack() },
                actions = {
                    IconButton(
                        onClick = {
                            exportBottomSheet = true
                        }
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.file_export),
                            contentDescription = null
                        )
                    }
                }
            )
        },
        contentWindowInsets = WindowInsets.none
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            ListHeader(title = stringResource(R.string.webui_config))

            webuiConfigMap.nullable { config ->
                val title = config.getPropOrNull<String?>("title")

                ListEditTextItem(
                    title = stringResource(R.string.webui_config_title_title),
                    desc = title ?: stringResource(R.string.webui_config_title_desc),
                    itemTextStyle = ListItemDefaults.itemStyle.apply {
                        if (title == null) {
                            copy(
                                descTextStyle = MaterialTheme.typography.bodyMedium.copy(
                                    fontStyle = FontStyle.Italic
                                )
                            )
                        }
                    },
                    value = title ?: "",
                    onConfirm = {
                        config("title", it)
                    }
                )

                val icon = config.getPropOrNull<String?>("icon")

                ListEditTextItem(
                    title = stringResource(R.string.webui_config_icon_title),
                    desc = icon ?: stringResource(R.string.webui_config_icon_desc),
                    itemTextStyle = ListItemDefaults.itemStyle.apply {
                        if (icon == null) {
                            copy(
                                descTextStyle = MaterialTheme.typography.bodyMedium.copy(
                                    fontStyle = FontStyle.Italic
                                )
                            )
                        }
                    },
                    value = icon ?: "",
                    onConfirm = {
                        config("icon", it)
                    }
                )

                ListSwitchItem(
                    title = stringResource(R.string.webui_config_exit_confirm_title),
                    desc = stringResource(R.string.webui_config_exit_confirm_desc),
                    checked = config.getBoolProp("exitConfirm", true),
                    onChange = { isChecked ->
                        config("exitConfirm", isChecked)
                    }
                )

                val backHandler = config.getBoolProp("backHandler", true)

                ListSwitchItem(
                    title = stringResource(R.string.webui_config_back_handler_title),
                    desc = stringResource(R.string.webui_config_back_handler_desc),
                    checked = backHandler,
                    onChange = { isChecked ->
                        config("backHandler", isChecked)
                    },
                )

                ListRadioCheckItem(
                    enabled = backHandler,
                    title = stringResource(R.string.webui_config_back_interceptor_title),
                    desc = stringResource(R.string.webui_config_back_interceptor_desc),
                    value = config.getPropOrNull<String?>("backInterceptor"),
                    options = context.interceptorList,
                    onConfirm = {
                        if (it.value == null) {
                            Toast.makeText(context, "Please select an option", Toast.LENGTH_SHORT)
                                .show()
                            return@ListRadioCheckItem
                        }

                        config("backInterceptor", it.value!!)
                    }
                )

                val pullToRefresh = config.getBoolProp("pullToRefresh", false)

                ListSwitchItem(
                    title = stringResource(R.string.webui_config_pull_to_refresh_title),
                    desc = stringResource(R.string.webui_config_pull_to_refresh_desc),
                    checked = pullToRefresh,
                    onChange = { isChecked ->
                        config("pullToRefresh", isChecked)
                    }
                )

                ListRadioCheckItem(
                    enabled = pullToRefresh,
                    title = stringResource(R.string.webui_config_refresh_interceptor_title),
                    desc = stringResource(R.string.webui_config_refresh_interceptor_desc),
                    value = config.getPropOrNull<String?>("refreshInterceptor"),
                    options = context.interceptorList,
                    onConfirm = {
                        if (it.value == null) {
                            Toast.makeText(context, "Please select an option", Toast.LENGTH_SHORT)
                                .show()
                            return@ListRadioCheckItem
                        }

                        config("refreshInterceptor", it.value!!)
                    }
                )

                ListSwitchItem(
                    title = stringResource(R.string.webui_config_window_resize_title),
                    desc = stringResource(R.string.webui_config_window_resize_desc),
                    checked = config.getBoolProp("windowResize", true),
                    onChange = { isChecked ->
                        config("windowResize", isChecked)
                    }
                )

                ListSwitchItem(
                    title = stringResource(R.string.webui_config_auto_style_statusbars_title),
                    desc = stringResource(R.string.webui_config_auto_style_statusbars_desc),
                    checked = config.getBoolProp("autoStatusBarsStyle", true),
                    onChange = { isChecked ->
                        config("autoStatusBarsStyle", isChecked)
                    }
                )

                ListEditTextSwitchItem(
                    title = stringResource(R.string.webui_config_history_fallback_title),
                    desc = stringResource(R.string.webui_config_history_fallback_desc),
                    value = config.getProp("historyFallbackFile", "index.html"),
                    checked = config.getBoolProp("historyFallback", false),
                    onChange = { isChecked ->
                        config("historyFallback", isChecked)
                    },
                    onConfirm = {
                        config("historyFallbackFile", it)
                    }
                )
            }

            ListHeader(title = stringResource(R.string.module_config))

            moduleConfigMap.nullable { config ->
                ListRadioCheckItem(
                    title = stringResource(R.string.settings_webui_engine),
                    desc = stringResource(R.string.settings_webui_engine_desc),
                    value = config.getProp("webui-engine", "wx"),
                    options = listOf(
                        RadioOptionItem(
                            value = "wx",
                            title = stringResource(R.string.settings_webui_engine_wx)
                        ),
                        RadioOptionItem(
                            value = "ksu",
                            title = stringResource(R.string.settings_webui_engine_ksu)
                        ),
                    ),
                    onConfirm = {
                        module("webui-engine", it.value)
                    }
                )
            }






            webuiConfigMap.nullable { config ->
                val dexFiles: MutableList<WebUIConfigDexFile> = run {
                    val raw = config["dexFiles"]
                    if (raw is List<*>) {
                        val json = moshi.adapter(Any::class.java).toJson(raw)
                        (webuiDexFileListAdapter.fromJson(json) ?: emptyList()).toMutableList()
                    } else mutableListOf()
                }

                if (dexFiles.isEmpty()) return@nullable

                ListHeader(title = stringResource(R.string.dex_files))

                dexFiles.forEachIndexed { index, it ->
                    if (it.path == null || it.className == null) return@forEachIndexed

                    Card(
                        modifier = {
                            surface = Modifier.padding(16.dp)
                            column = Modifier.padding(0.dp)
                        }
                    ) {
                        val path = it.path!!
                        val className = it.className!!

                        ListEditTextItem(
                            title = stringResource(R.string.path),
                            desc = path,
                            value = path,
                            onConfirm = { p ->
                                dexFiles[index] = dexFiles[index].copy(path = p)
                                config("dexFiles", dexFiles)
                            }
                        )

                        ListEditTextItem(
                            title = stringResource(R.string.class_name),
                            desc = className,
                            value = className,
                            onConfirm = { c ->
                                dexFiles[index] = dexFiles[index].copy(className = c)
                                config("dexFiles", dexFiles)
                            }
                        )

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
                                dexFiles.remove(it)
                                config("dexFiles", dexFiles)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
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
private fun ExportBottomSheet(
    onClose: () -> Unit,
    onModuleExport: () -> Unit,
    onConfigExport: () -> Unit,
) = BottomSheet(
    onDismissRequest = onClose
) {
    Text(
        modifier = Modifier.padding(vertical = 16.dp, horizontal = 25.dp),
        text = stringResource(R.string.export_config),
        style = MaterialTheme.typography.headlineSmall.copy(color = MaterialTheme.colorScheme.primary)
    )

    ListButtonItem(
        title = stringResource(R.string.export_module_config_json),
        onClick = onModuleExport
    )

    ListButtonItem(
        title = stringResource(R.string.export_webui_config_json),
        onClick = onConfigExport
    )

    Spacer(Modifier.height(16.dp))
}