package com.dergoogler.mmrl.wx.ui.screens.home

import android.system.Os
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dergoogler.mmrl.ext.managerVersion
import com.dergoogler.mmrl.ext.none
import com.dergoogler.mmrl.platform.file.SuFile.Companion.toFormattedFileSize
import com.dergoogler.mmrl.wx.R
import com.dergoogler.mmrl.wx.datastore.model.WorkingMode.Companion.isNonRoot
import com.dergoogler.mmrl.wx.datastore.model.WorkingMode.Companion.isRoot
import com.dergoogler.mmrl.wx.datastore.providable.LocalUserPreferences
import com.dergoogler.mmrl.wx.model.module.ModuleAnalytics
import com.dergoogler.mmrl.wx.ui.component.BottomNavigation
import com.dergoogler.mmrl.wx.ui.providable.LocalModulesViewModel
import com.dergoogler.mmrl.wx.ui.screens.home.item.NonRootItem
import com.dergoogler.mmrl.wx.ui.screens.home.item.RootItem
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import dev.mmrlx.compose.layout.card
import dev.mmrlx.compose.ui.LinearProgressIndicator
import dev.mmrlx.compose.ui.Surface
import dev.mmrlx.compose.ui.Text
import dev.mmrlx.compose.ui.list.List
import dev.mmrlx.compose.ui.list.component.Item
import dev.mmrlx.compose.ui.list.component.item.Description
import dev.mmrlx.compose.ui.list.component.item.Icon
import dev.mmrlx.compose.ui.list.component.item.Title
import dev.mmrlx.compose.ui.scaffold.Scaffold
import dev.mmrlx.compose.ui.toolbar.Toolbar
import dev.mmrlx.compose.ui.toolbar.ToolbarDefaults
import dev.mmrlx.compose.ui.toolbar.ToolbarScrollBehavior

val listItemContentPaddingValues: PaddingValues = PaddingValues(vertical = 8.dp, horizontal = 25.dp)

@Destination<RootGraph>(start = true)
@OptIn(ExperimentalComposeApi::class)
@Composable
fun HomeScreen() {
    val viewModel = LocalModulesViewModel.current
    val modules by viewModel.local.collectAsStateWithLifecycle()
    val scrollBehavior = ToolbarDefaults.pinnedScrollBehavior()
    val context = LocalContext.current
    val userPreferences = LocalUserPreferences.current
    val browser = LocalUriHandler.current

    val analytics by ModuleAnalytics.remember(modules)

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        toolbar = {
            TopBar2(
                scrollBehavior = scrollBehavior,
            )
        },
        bottomBar = { BottomNavigation() },
        contentWindowInsets = WindowInsets.none,
    ) {
        Column(
            modifier =
                Modifier
                    .scaffoldHazeSource()
                    .scaffoldPadding()
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when {
                userPreferences.workingMode.isRoot ->
                    RootItem()

                userPreferences.workingMode.isNonRoot ->
                    NonRootItem()
            }

            List(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = listItemContentPaddingValues,
            ) {
                val scope = this

                Surface(
                    modifier = Modifier
                        .card()
                        .padding(vertical = 16.dp)
                        .fillMaxWidth(),
                ) {
                    val uname = Os.uname()
                    Column {
                        scope.Item {
                            Icon(painter = painterResource(R.drawable.cookie_man))
                            Title(R.string.unix_name)
                            Description("${uname.sysname} ${uname.release} ${uname.version} ${uname.machine}")
                        }

                        scope.Item {
                            Icon(painter = painterResource(R.drawable.launcher_outline))
                            Title(R.string.app_version)
                            Description("${context.managerVersion.first} (${context.managerVersion.second})")
                        }

//                        scope.Item {
//                            Icon(painter = painterResource(R.drawable.fingerprint))
//                            Title(R.string.fingerprint)
//                            Description(
//                                if (userPreferences.hideFingerprintInHome) {
//                                    stringResource(id = R.string.hidden)
//                                } else {
//                                    Build.FINGERPRINT
//                                },
//                            )
//                        }

                        scope.Item {
                            Icon(painter = painterResource(R.drawable.cpu_2))
                            Title(R.string.architecture)
                            Description(uname.machine)
                        }
                    }
                }

                Surface(
                    modifier = Modifier
                        .card()
                        .padding(vertical = 16.dp)
                        .fillMaxWidth()
                ) {
                    scope.Item {
                        Title(R.string.home_storage_usage)

                        Description {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = analytics.totalModulesUsageBytes.toFormattedFileSize(),
                                )

                                LinearProgressIndicator(
                                    progress = {
                                        analytics.totalStorageUsage
                                    },
                                    modifier =
                                        Modifier
                                            .height(10.dp)
                                            .weight(1f),
                                    drawStopIndicator = {},
                                )

                                Text(
                                    text = analytics.totalDeviceStorageBytes.toFormattedFileSize(),
                                )
                            }
                        }
                    }
                }

                Surface(
                    modifier = Modifier
                        .clickable {
                            browser.openUri("https://github.com/sponsors/MMRLApp")
                        }
                        .card()
                        .padding(vertical = 16.dp)
                        .fillMaxWidth()
                ) {
                    scope.Item {
                        Title(
                            id = R.string.home_support_title,
                            styleTransform = {
                                val newStyle = it.copy(color = Color.Unspecified)
                                it.merge(newStyle)
                            },
                        )

                        Description(
                            id = R.string.home_support_content,
                            styleTransform = {
                                val newStyle = it.copy(color = Color.Unspecified)
                                it.merge(newStyle)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TopBar2(
    scrollBehavior: ToolbarScrollBehavior,
) {
    Toolbar(
        scrollBehavior = scrollBehavior,
        title = {
            dev.mmrlx.compose.ui.icon.Icon(
                modifier = Modifier.size(30.dp),
                painter = painterResource(R.drawable.launcher_outline),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.surfaceTint,
            )
        },
    )
}