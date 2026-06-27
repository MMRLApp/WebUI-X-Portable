package com.dergoogler.mmrl.wx.ui.screens.modules.screens

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.BitmapFactory
import android.graphics.drawable.Icon
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dergoogler.mmrl.platform.model.ModId.Companion.INTENT_MOD_ID
import com.dergoogler.mmrl.wx.R
import com.dergoogler.mmrl.wx.datastore.model.WebUIEngine
import com.dergoogler.mmrl.wx.model.module.Module
import com.dergoogler.mmrl.wx.ui.component.LocalModule
import com.dergoogler.mmrl.wx.ui.component.ModuleScope
import com.dergoogler.mmrl.wx.ui.component.NavigateUpToolbar
import com.dergoogler.mmrl.wx.ui.providable.LocalDestinationsNavigator
import com.dergoogler.mmrl.wx.util.toPainter
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import dev.mmrlx.compose.ui.HorizontalDivider
import dev.mmrlx.compose.ui.Text
import dev.mmrlx.compose.ui.button.Button
import dev.mmrlx.compose.ui.button.ButtonVariant
import dev.mmrlx.compose.ui.icon.Icon
import dev.mmrlx.compose.ui.scaffold.Scaffold
import dev.mmrlx.compose.ui.text.Input
import dev.mmrlx.compose.ui.text.rememberInputState
import dev.mmrlx.compose.ui.toolbar.ToolbarTitle
import dev.mmrlx.nio.SuFile
import com.dergoogler.mmrl.wx.ui.activity.webui.WebUIActivity as WxWebUIActivity
import com.dergoogler.mmrl.wx.ui.webui.WebUIActivity as MxWebUIActivity

@Destination<RootGraph>()
@Composable
fun ShortcutCreateScreen(moduleId: String) {
    ModuleScope(moduleId) {
        ShortcutCreateContent()
    }
}

@Composable
fun ShortcutCreateContent() {
    val module = LocalModule.current
    val context = LocalContext.current

    val navigator = LocalDestinationsNavigator.current

    val moduleIcon = remember(module) { module.icon }

    var shortcutName = rememberInputState(module.name)
    var selectedEngine by remember { mutableStateOf<WebUIEngine?>(null) }
    var iconUri by remember { mutableStateOf<String?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { iconUri = it.toString() }
    }

    val isCreateEnabled =
        selectedEngine != null && shortcutName.text.isNotBlank() && (iconUri != null || moduleIcon != null)

    Scaffold(
        toolbar = {
            NavigateUpToolbar(
                title = {
                    ToolbarTitle(
                        title = "Create Shortcut",
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
                .fillMaxSize()
                .scaffoldPadding()
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = stringResource(R.string.shortcut_name)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Input(
                    state = shortcutName,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(module.name)
                    }
                )

                Spacer(modifier = Modifier.height(22.dp))

                Text(
                    text = stringResource(R.string.shortcut_icon)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = { launcher.launch("image/*") },
                        variant = ButtonVariant.Outline
                    ) {
                        Text(stringResource(R.string.shortcut_pick_icon))
                    }

                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = { iconUri = null },
                        variant = ButtonVariant.Outline
                    ) {
                        Text(stringResource(R.string.shortcut_reset_icon))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                ShortcutIconPreview(
                    iconUri = iconUri,
                    moduleIcon = moduleIcon
                )

                Spacer(modifier = Modifier.height(22.dp))

                Text(
                    text = stringResource(R.string.settings_webui_engine)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    EngineOption(
                        modifier = Modifier.weight(1f),
                        title = stringResource(R.string.settings_webui_engine_wx),
                        selected = selectedEngine == WebUIEngine.WX,
                        onClick = { selectedEngine = WebUIEngine.WX }
                    )

                    EngineOption(
                        modifier = Modifier.weight(1f),
                        title = stringResource(R.string.settings_webui_engine_mx),
                        selected = selectedEngine == WebUIEngine.MX,
                        onClick = { selectedEngine = WebUIEngine.MX }
                    )
                }
            }

            HorizontalDivider()

            val engineRequiredString = stringResource(R.string.shortcut_engine_required)
            val invalidShortcutString = stringResource(R.string.shortcut_invalid_fields)
            val createRequestedString = stringResource(R.string.shortcut_create_requested)

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    text = "Review your shortcut settings and create it.",
                )

                Button(
                    enabled = isCreateEnabled,
                    variant = ButtonVariant.Default,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    onClick = {
                        if (selectedEngine == null) {
                            Toast.makeText(
                                context,
                                engineRequiredString,
                                Toast.LENGTH_SHORT
                            ).show()
                            return@Button
                        }

                        if (shortcutName.text.isBlank()) {
                            Toast.makeText(
                                context,
                                invalidShortcutString,
                                Toast.LENGTH_SHORT
                            ).show()
                            return@Button
                        }

                        val isCreated = createShortcut(
                            context = context,
                            module = module,
                            title = shortcutName.text.toString(),
                            engine = selectedEngine!!,
                            iconUri = iconUri,
                        )

                        if (isCreated) {
                            Toast.makeText(
                                context,
                                createRequestedString,
                                Toast.LENGTH_SHORT
                            ).show()
                            navigator.popBackStack()
                        }
                    }
                ) {
                    Text(stringResource(R.string.add_shortcut))
                }
            }
        }
    }
}

@Composable
private fun EngineOption(
    modifier: Modifier = Modifier,
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Button(
        modifier = modifier.defaultMinSize(minHeight = 56.dp),
        onClick = onClick,
        variant = if (selected) ButtonVariant.Default else ButtonVariant.Outline,
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.engine)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(title)
            }
        }
    }
}

@Composable
private fun ShortcutIconPreview(
    iconUri: String?,
    moduleIcon: SuFile?,
) {
    val context = LocalContext.current

    val uriBitmap = remember(iconUri) {
        iconUri?.let {
            runCatching {
                context.contentResolver.openInputStream(Uri.parse(it)).use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            }.getOrNull()
        }
    }

    val uriPainter = uriBitmap?.let { BitmapPainter(it.asImageBitmap()) }

    when {
        uriPainter != null -> {
            Image(
                painter = uriPainter,
                contentDescription = null,
                modifier = Modifier.size(64.dp)
            )

            Spacer(modifier = Modifier.height(6.dp))
            Text(text = stringResource(R.string.shortcut_preview_custom_icon))
        }

        moduleIcon != null -> {
            Image(
                painter = moduleIcon.toPainter(),
                contentDescription = null,
                modifier = Modifier.size(64.dp)
            )

            Spacer(modifier = Modifier.height(6.dp))
            Text(text = stringResource(R.string.shortcut_preview_module_icon))
        }

        else -> {
            Text(
                text = stringResource(R.string.shortcut_icon_file_not_found),
                color = colorResource(android.R.color.holo_red_light)
            )
        }
    }
}

private fun createShortcut(
    context: Context,
    module: Module,
    title: String,
    engine: WebUIEngine,
    iconUri: String?,
): Boolean {
    val shortcutManager = context.getSystemService(ShortcutManager::class.java)
    val shortcutId = "shortcut_${module.id}_${engine.name.lowercase()}"

    if (!shortcutManager.isRequestPinShortcutSupported) {
        Toast.makeText(
            context,
            context.getString(R.string.shortcut_not_supported),
            Toast.LENGTH_SHORT
        )
            .show()
        return false
    }

    if (shortcutManager.pinnedShortcuts.any { it.id == shortcutId }) {
        Toast.makeText(
            context,
            context.getString(R.string.shortcut_already_exists),
            Toast.LENGTH_SHORT
        )
            .show()
        return false
    }

    val bitmap = loadShortcutBitmap(
        context = context,
        module = module,
        iconUri = iconUri
    )

    if (bitmap == null) {
        Toast.makeText(
            context,
            context.getString(R.string.shortcut_icon_invalid),
            Toast.LENGTH_SHORT
        )
            .show()
        return false
    }

    val shortcutIntent = when (engine) {
        WebUIEngine.WX -> {
            Intent(context, WxWebUIActivity::class.java).apply {
                putExtra(INTENT_MOD_ID, module.id)
                action = Intent.ACTION_VIEW
            }
        }

        WebUIEngine.MX -> {
            Intent(context, MxWebUIActivity::class.java).apply {
                putExtra("MODULE_ID", module.id)
                action = Intent.ACTION_VIEW
                addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
            }
        }

        else -> {
            Toast.makeText(
                context,
                context.getString(R.string.unsupported_engine),
                Toast.LENGTH_SHORT
            )
                .show()
            return false
        }
    }

    val shortcut = ShortcutInfo.Builder(context, shortcutId)
        .setShortLabel(title)
        .setLongLabel(title)
        .setIcon(Icon.createWithAdaptiveBitmap(bitmap))
        .setIntent(shortcutIntent)
        .build()

    shortcutManager.requestPinShortcut(shortcut, null)
    return true
}

private fun loadShortcutBitmap(
    context: Context,
    module: Module,
    iconUri: String?,
) = runCatching {
    if (iconUri != null) {
        context.contentResolver.openInputStream(Uri.parse(iconUri)).use { input ->
            input?.let { BitmapFactory.decodeStream(it) }
        }
    } else {
        module.icon?.newInputStream()?.buffered()?.use { BitmapFactory.decodeStream(it) }
    }
}.getOrNull()
