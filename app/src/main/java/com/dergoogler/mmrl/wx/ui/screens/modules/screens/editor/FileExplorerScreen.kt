package com.dergoogler.mmrl.wx.ui.screens.modules.screens.editor

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
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
import com.dergoogler.mmrl.ui.component.listItem.dsl.component.Item
import com.dergoogler.mmrl.ui.component.listItem.dsl.component.item.Description
import com.dergoogler.mmrl.ui.component.listItem.dsl.component.item.Start
import com.dergoogler.mmrl.ui.component.listItem.dsl.component.item.Title
import com.dergoogler.mmrl.ui.component.scaffold.Scaffold
import com.dergoogler.mmrl.ui.component.toolbar.Toolbar
import com.dergoogler.mmrl.ui.component.toolbar.ToolbarTitle
import com.dergoogler.mmrl.wx.R
import com.dergoogler.mmrl.wx.ui.providable.LocalDestinationsNavigator
import com.dergoogler.mmrl.wx.ui.screens.modules.screens.parentSuFile
import com.dergoogler.mmrl.wx.util.toFormattedDateSafely
import com.dergoogler.mmrl.wx.viewmodel.FileExplorerViewModel
import com.dergoogler.mmrl.wx.viewmodel.FileItem
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.FileEditorScreenDestination

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
    val snackbarHostState = remember { SnackbarHostState() }

    // FAB state management
    var isFabExpanded by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var createDialogType by remember { mutableStateOf(CreateType.FOLDER) }

    // File selection state
    var selectedFiles by remember { mutableStateOf<Set<FileItem>>(emptySet()) }
    var isSelectionMode by remember { mutableStateOf(false) }

    // Activity result launchers
    val importFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.importFileFromUri(it) }
    }

    val importMultipleFilesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.importMultipleFiles(uris)
        }
    }

    val exportFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { uri: Uri? ->
        uri?.let { targetUri ->
            selectedFiles.firstOrNull()?.let { fileItem ->
                viewModel.exportFile(fileItem.file, targetUri)
                selectedFiles = emptySet()
                isSelectionMode = false
            }
        }
    }

    LaunchedEffect(module.id) {
        viewModel.initialize(initialPath)
    }

    // Show snackbar for messages
    LaunchedEffect(state.errorMessage, state.successMessage) {
        state.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessages()
        }
        state.successMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessages()
        }
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    val backClick: () -> Unit = remember(state, isSelectionMode) {
        {
            when {
                isSelectionMode -> {
                    isSelectionMode = false
                    selectedFiles = emptySet()
                }
                state.canGoBack -> viewModel.navigateBack()
                else -> navigator.popBackStack()
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
                        title = if (isSelectionMode) "${selectedFiles.size} selected" else module.name,
                    )
                },
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    IconButton(onClick = backClick) {
                        Icon(
                            painter = painterResource(
                                id = if (isSelectionMode) R.drawable.x
                                else com.dergoogler.mmrl.ui.R.drawable.arrow_left
                            ),
                            contentDescription = if (isSelectionMode) "Cancel selection" else "Back"
                        )
                    }
                },
                actions = {
                    if (isSelectionMode) {
                        // Selection mode actions
                        if (selectedFiles.size == 1 && !selectedFiles.first().isDirectory) {
                            IconButton(
                                onClick = {
                                    exportFileLauncher.launch(selectedFiles.first().name)
                                }
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.upload),
                                    contentDescription = "Export file"
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                selectedFiles.forEach { fileItem ->
                                    viewModel.deleteFile(fileItem.file)
                                }
                                selectedFiles = emptySet()
                                isSelectionMode = false
                            }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.trash),
                                contentDescription = "Delete selected",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    } else {
                        // Normal mode actions
                        IconButton(
                            onClick = { viewModel.refresh() }
                        ) {
                            Icon(
                                painter = painterResource(com.dergoogler.mmrl.webui.R.drawable.refresh),
                                contentDescription = "Refresh"
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (!isSelectionMode) {
                ExpandableFab(
                    isExpanded = isFabExpanded,
                    onExpandedChange = { isFabExpanded = it },
                    onCreateFolder = {
                        createDialogType = CreateType.FOLDER
                        showCreateDialog = true
                        isFabExpanded = false
                    },
                    onCreateFile = {
                        createDialogType = CreateType.FILE
                        showCreateDialog = true
                        isFabExpanded = false
                    },
                    onImportFile = {
                        importFileLauncher.launch("*/*")
                        isFabExpanded = false
                    },
                    onImportMultipleFiles = {
                        importMultipleFilesLauncher.launch("*/*")
                        isFabExpanded = false
                    },
                    isLoading = state.isOperationInProgress
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets.none
    ) { innerPadding ->
        Box {
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

                // Success message
                state.successMessage?.let { success ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Text(
                            text = success,
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
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
                                isSelected = fileItem in selectedFiles,
                                isSelectionMode = isSelectionMode,
                                onClick = {
                                    if (isSelectionMode) {
                                        selectedFiles = if (fileItem in selectedFiles) {
                                            selectedFiles - fileItem
                                        } else {
                                            selectedFiles + fileItem
                                        }
                                        if (selectedFiles.isEmpty()) {
                                            isSelectionMode = false
                                        }
                                    } else {
                                        if (fileItem.isDirectory) {
                                            viewModel.navigateToDirectory(fileItem.file)
                                        } else {
                                            navigator.navigate(
                                                FileEditorScreenDestination(
                                                    module,
                                                    fileItem.file.path
                                                )
                                            )
                                        }
                                    }
                                },
                                onLongClick = {
                                    if (!isSelectionMode) {
                                        isSelectionMode = true
                                        selectedFiles = setOf(fileItem)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Semi-transparent overlay when FAB is expanded
            if (isFabExpanded) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color.Black.copy(alpha = 0.3f))
                        .clickable { isFabExpanded = false }
                )
            }
        }

        // Create dialog
        if (showCreateDialog) {
            CreateDialog(
                type = createDialogType,
                onDismiss = { showCreateDialog = false },
                onConfirm = { name, content ->
                    when (createDialogType) {
                        CreateType.FOLDER -> viewModel.createFolder(name)
                        CreateType.FILE -> viewModel.createFile(name, content ?: "")
                    }
                    showCreateDialog = false
                }
            )
        }
    }
}

@Composable
private fun ExpandableFab(
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onCreateFolder: () -> Unit,
    onCreateFile: () -> Unit,
    onImportFile: () -> Unit,
    onImportMultipleFiles: () -> Unit,
    isLoading: Boolean
) {
    val rotation by animateFloatAsState(
        targetValue = if (isExpanded) 45f else 0f,
        animationSpec = tween(300),
        label = "fab_rotation"
    )

    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Mini FABs (shown when expanded)
        if (isExpanded) {
            listOf(
                Triple("Create Folder", R.drawable.folder_plus, onCreateFolder),
                Triple("Create File", R.drawable.file_plus, onCreateFile),
                Triple("Import File", R.drawable.download, onImportFile),
                Triple("Import Multiple", R.drawable.filter_2_down, onImportMultipleFiles)
            ).forEach { (label, icon, action) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                    ) {
                        Text(
                            text = label,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    SmallFloatingActionButton(
                        onClick = action,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ) {
                        Icon(
                            painter = painterResource(icon),
                            contentDescription = label
                        )
                    }
                }
            }
        }

        // Main FAB
        ExtendedFloatingActionButton(
            onClick = { onExpandedChange(!isExpanded) },
            text = {
                Text(
                    text = if (isExpanded) "Close" else "Actions",
                    style = MaterialTheme.typography.labelLarge
                )
            },
            icon = {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                } else {
                    Icon(
                        painter = painterResource(R.drawable.plus),
                        contentDescription = "File actions",
                        modifier = Modifier.rotate(rotation)
                    )
                }
            },
            expanded = isExpanded || isLoading
        )
    }
}

enum class CreateType {
    FOLDER, FILE
}

@Composable
private fun CreateDialog(
    type: CreateType,
    onDismiss: () -> Unit,
    onConfirm: (String, String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    val isFile = type == CreateType.FILE

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Create ${if (isFile) "File" else "Folder"}")
        },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("${if (isFile) "File" else "Folder"} name") },
                    singleLine = !isFile,
                    modifier = Modifier.fillMaxWidth()
                )

                if (isFile) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        label = { Text("Content (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 5
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name, if (isFile) content else null) },
                enabled = name.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
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
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    val interactionSource = remember { MutableInteractionSource() }

    Item(
        modifier = Modifier
            .combinedClickable(
                enabled = true,
                interactionSource = interactionSource,
                role = Role.Button,
                indication = ripple(),
                onClick = onClick,
                onLongClick = onLongClick
            ),
    ) {
        Start {
            Box {
                Icon(
                    painter = painterResource(fileItem.icon),
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = when {
                        isSelected -> MaterialTheme.colorScheme.primary
                        fileItem.isDirectory -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )

                if (isSelected) {
                    Icon(
                        painter = painterResource(R.drawable.circle_check),
                        contentDescription = "Selected",
                        modifier = Modifier
                            .size(16.dp)
                            .align(Alignment.BottomEnd),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Title {
            Text(
                text = fileItem.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = if (fileItem.isDirectory) FontWeight.Medium else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
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