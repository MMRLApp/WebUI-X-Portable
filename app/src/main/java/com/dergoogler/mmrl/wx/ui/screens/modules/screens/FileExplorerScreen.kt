package com.dergoogler.mmrl.wx.ui.screens.modules.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dergoogler.mmrl.ext.none
import com.dergoogler.mmrl.platform.content.LocalModule
import com.dergoogler.mmrl.platform.file.SuFile
import com.dergoogler.mmrl.platform.file.SuFile.Companion.toFormattedFileSize
import com.dergoogler.mmrl.platform.model.ModId.Companion.moduleDir
import com.dergoogler.mmrl.ui.component.listItem.dsl.List
import com.dergoogler.mmrl.ui.component.listItem.dsl.ListScope
import com.dergoogler.mmrl.ui.component.listItem.dsl.component.ButtonItem
import com.dergoogler.mmrl.ui.component.listItem.dsl.component.item.Description
import com.dergoogler.mmrl.ui.component.listItem.dsl.component.item.Start
import com.dergoogler.mmrl.ui.component.listItem.dsl.component.item.Title
import com.dergoogler.mmrl.ui.component.scaffold.Scaffold
import com.dergoogler.mmrl.ui.component.toolbar.Toolbar
import com.dergoogler.mmrl.ui.component.toolbar.ToolbarTitle
import com.dergoogler.mmrl.wx.R
import com.dergoogler.mmrl.wx.ui.providable.LocalDestinationsNavigator
import com.dergoogler.mmrl.wx.util.toFormattedDateSafely
import com.dergoogler.mmrl.wx.viewmodel.FileExplorerViewModel
import com.dergoogler.mmrl.wx.viewmodel.FileItem
import com.dergoogler.mmrl.wx.viewmodel.parentSuFile
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph

@Destination<RootGraph>
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileExplorerScreen(
    module: LocalModule,
) {
    val navigator = LocalDestinationsNavigator.current
    val viewModel = hiltViewModel<FileExplorerViewModel>()
    val state by viewModel.state.collectAsState()
    val initialPath = SuFile(module.id.moduleDir)

    LaunchedEffect(module.id) {
        viewModel.initialize(initialPath)
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    val backClick: () -> Unit = remember(state) {
        {
            if (state.canGoBack) {
                viewModel.navigateBack()
            } else {
                navigator.popBackStack()
            }
        }
    }

    BackHandler(onBack = backClick)

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            Toolbar(
                title = {
                    ToolbarTitle(
                        title = module.name,
                    )
                },
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    IconButton(
                        onClick = backClick
                    ) {
                        Icon(
                            painter = painterResource(id = com.dergoogler.mmrl.ui.R.drawable.arrow_left),
                            contentDescription = "Back"
                        )
                    }

                },
                actions = {
                    IconButton(
                        onClick = { viewModel.refresh() }
                    ) {
                        Icon(
                            painter = painterResource(com.dergoogler.mmrl.webui.R.drawable.refresh),
                            contentDescription = "Refresh"
                        )
                    }
                }
            )
        },
        contentWindowInsets = WindowInsets.none
    ) { innerPadding ->
        List(
            modifier = Modifier.padding(innerPadding),
        ) {
            // Path breadcrumb
            state.currentPath?.let { currentPath ->
                PathBreadcrumb(
                    currentPath = currentPath,
                    onPathClick = { path ->
                        viewModel.navigateToPath(path)
                    }
                )
            }

            // Error message
            state.errorMessage?.let { error ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(state.fileItems) { fileItem ->
                        FileItemRow(
                            fileItem = fileItem,
                            onClick = {
                                if (fileItem.isDirectory) {
                                    viewModel.navigateToDirectory(fileItem.file)
                                    return@FileItemRow
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PathBreadcrumb(
    currentPath: SuFile,
    onPathClick: (SuFile) -> Unit,
) {
    val pathParts = mutableListOf<SuFile>()
    var tempPath: SuFile? = currentPath

    // Build path hierarchy
    while (tempPath != null) {
        pathParts.add(0, tempPath)
        tempPath = tempPath.parentSuFile
    }

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            vertical = 8.dp,
            horizontal = 16.dp
        ),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        itemsIndexed(
            items = pathParts,
            key = { index, item -> item.path + index }

        ) { index, path ->
            if (index > 0) {
                Icon(
                    painter = painterResource(R.drawable.chevron_right),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = if (index == 0 && path.name.isEmpty()) "Root" else path.name,
                style = MaterialTheme.typography.bodySmall,
                color = if (index == pathParts.lastIndex) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.clickable {
                    if (index != pathParts.lastIndex) {
                        onPathClick(path)
                    }
                }
            )
        }
    }
}

@Composable
private fun ListScope.FileItemRow(
    fileItem: FileItem,
    onClick: () -> Unit,
) {
    ButtonItem(onClick) {
        Start {
            Icon(
                painter = painterResource(fileItem.icon),
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = if (fileItem.isDirectory) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }

        Title {
            Text(
                text = fileItem.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = if (fileItem.isDirectory) FontWeight.Medium else FontWeight.Normal
            )
        }

        Description {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (!fileItem.isDirectory && fileItem.size > 0) {
                    Text(
                        text = fileItem.size.toFormattedFileSize(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = fileItem.lastModified.toFormattedDateSafely,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
