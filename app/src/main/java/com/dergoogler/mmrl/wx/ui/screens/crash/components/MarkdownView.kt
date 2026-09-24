package com.dergoogler.mmrl.wx.ui.screens.crash.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.dergoogler.mmrl.wx.datastore.providable.LocalUserPreferences
import dev.mmrlx.compose.ui.theme.MMRLXTheme
import dev.mmrlx.compose.webui.WebUIView
import dev.mmrlx.compose.webui.insets
import dev.mmrlx.compose.webui.rememberWebUIState
import dev.mmrlx.webui.WebUIOptions

@Composable
fun MarkdownView(
    content: String,
    modifier: Modifier = Modifier,
    insets: PaddingValues = PaddingValues(0.dp),
) {
    val userPrefs = LocalUserPreferences.current
    val colors = MMRLXTheme.colors
    val scheme = MaterialTheme.colorScheme.copy(
        background = colors.background,
        onBackground = colors.foreground,
        surface = colors.code,
        surfaceTint = colors.muted

    )

    val wstate =
        rememberWebUIState(
            "https://desc.mmrl.dev",
            WebUIOptions(
                initialPage = "internal/assets/markdown/markdown.html",
            )
        ) {
            it
                .settings {
                    useDefaultFileSystem = false
                    useDefaultApplicationInterface = false
                    darkMode = userPrefs.isDarkMode()
                }
                .client {
                    // not related to client
                    it.webview.setBackgroundColor(Color.Transparent.toArgb())
                }
                .chromeClient { }
                .insets(insets)
                .registerPathHandler(
                    MarkdownPathHandler::class.java
                ) {
                    add(
                        String::class.java to content
                    )
                    add(
                        ColorScheme::class.java to scheme
                    )
                }
        }

    DisposableEffect(Unit) {
        onDispose {
            wstate.destroy()
        }
    }

    WebUIView(wstate, modifier)
}