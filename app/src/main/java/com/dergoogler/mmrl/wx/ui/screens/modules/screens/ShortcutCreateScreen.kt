package com.dergoogler.mmrl.wx.ui.screens.modules.screens

import android.graphics.BitmapFactory
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
import androidx.core.net.toUri
import com.dergoogler.mmrl.wx.R
import com.dergoogler.mmrl.wx.datastore.model.WebUIEngine
import com.dergoogler.mmrl.wx.model.module.title
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

    val shortcutName = rememberInputState(module.webrootConfig.title ?: module.name)
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

                        val isCreated = module.createShortcut(
                            title = shortcutName.text.toString(),
                            iconUri = iconUri,
                            engine = selectedEngine!!
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
                context.contentResolver.openInputStream(it.toUri()).use { stream ->
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