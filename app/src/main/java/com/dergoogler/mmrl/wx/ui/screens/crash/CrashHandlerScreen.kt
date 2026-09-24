package com.dergoogler.mmrl.wx.ui.screens.crash

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.dergoogler.mmrl.ext.none
import com.dergoogler.mmrl.ext.nullable
import com.dergoogler.mmrl.ext.shareText
import com.dergoogler.mmrl.ui.component.NavigationBarsSpacer
import com.dergoogler.mmrl.wx.BuildConfig
import com.dergoogler.mmrl.wx.R
import com.dergoogler.mmrl.wx.ui.screens.crash.components.MarkdownView
import com.dergoogler.mmrl.wx.util.HelpMessage
import dev.mmrlx.compose.layout.card
import dev.mmrlx.compose.ui.Surface
import dev.mmrlx.compose.ui.Text
import dev.mmrlx.compose.ui.button.Button
import dev.mmrlx.compose.ui.button.ButtonVariant
import dev.mmrlx.compose.ui.dialog.SheetValue
import dev.mmrlx.compose.ui.dialog.rememberModalBottomSheet
import dev.mmrlx.compose.ui.dialog.rememberModalBottomSheetState
import dev.mmrlx.compose.ui.ext.with
import dev.mmrlx.compose.ui.scaffold.Scaffold
import dev.mmrlx.compose.ui.text.FormatText
import dev.mmrlx.compose.ui.theme.MMRLXTheme
import dev.mmrlx.compose.ui.toolbar.Toolbar
import dev.mmrlx.compose.ui.toolbar.ToolbarDefaults
import dev.mmrlx.compose.ui.toolbar.ToolbarScrollBehavior
import dev.mmrlx.compose.ui.toolbar.ToolbarTitle

@Composable
fun CrashHandlerScreen(
    message: String,
    stacktrace: String,
    help: HelpMessage?,
) {
    WindowInsets.statusBars
    val scrollBehavior = ToolbarDefaults.pinnedScrollBehavior()
    val context = LocalContext.current
    val browser = LocalUriHandler.current

    val helpSheetState =
        rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
            confirmValueChange = { newState ->
                newState != SheetValue.Hidden
            })

    val helpSheet = rememberModalBottomSheet()

    val hasHelp = help != null

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        toolbar = {
            TopBar0(
                scrollBehavior = scrollBehavior,
            )
        },
        contentWindowInsets = WindowInsets.none,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .with(this@Scaffold) { it.scaffoldHazeSource() },
            contentPadding = PaddingValues(
                top = this@Scaffold.scaffoldTopPadding + 8.dp,
                start = 8.dp,
                end = 8.dp,
                bottom = this@Scaffold.scaffoldBottomPadding + 8.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Surface(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .card(),
                ) {
                    SelectionContainer {
                        Text(
                            modifier =
                                Modifier
                                    .padding(16.dp)
                                    .horizontalScroll(rememberScrollState()),
                            text = message,
                            style =
                                MMRLXTheme.typography.bodyLarge.copy(
                                    fontFamily = FontFamily.Monospace,
                                ),
                        )
                    }
                }

                help.nullable {
                    Spacer(Modifier.height(8.dp))

                    Button(
                        variant = ButtonVariant.Secondary,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { helpSheet.open() },
                    ) {
                        FormatText(
                            text = "${stringResource(R.string.help)} %y",
                            style = MaterialTheme.typography.labelLarge,
                        ) {
                            composable {
                                Icon(
                                    modifier = Modifier.size(fontSize.dp),
                                    painter = painterResource(com.dergoogler.mmrl.wx.R.drawable.info_circle),
                                    contentDescription = null
                                )
                            }
                        }
                    }
                }
            }

            item {
                Surface(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .card(),
                ) {
                    SelectionContainer {
                        Text(
                            modifier =
                                Modifier
                                    .padding(16.dp)
                                    .horizontalScroll(rememberScrollState()),
                            text = stacktrace,
                            style =
                                MMRLXTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                ),
                        )
                    }
                }
            }

            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Button(
                        modifier = Modifier.weight(1f),
                        variant = ButtonVariant.Outline,
                        onClick = {
                            browser.openUri("${BuildConfig.ORIGIN}/issues")
                        }
                    ) {
                        Text(
                            text = stringResource(R.string.report_to_issues),
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            context.shareText("$message\n\n$stacktrace")
                        },
                    ) {
                        Text(
                            text = stringResource(R.string.copy_logs),
                        )
                    }
                }

                NavigationBarsSpacer()
            }
        }
    }

    if (hasHelp) {
        helpSheet(
            sheetState = helpSheetState,
            sheetGesturesEnabled = false,
            containerColor = MMRLXTheme.colors.background
        ) {
            MarkdownView(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.89754f),
                content = help.content,
            )
        }
    }
}

@Composable
private fun TopBar0(scrollBehavior: ToolbarScrollBehavior) =
    Toolbar(
        title = {
            ToolbarTitle(titleResId = R.string.we_hit_a_brick_crash)
        },
        scrollBehavior = scrollBehavior,
    )
