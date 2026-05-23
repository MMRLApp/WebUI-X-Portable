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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dergoogler.mmrl.ext.iconSize
import com.dergoogler.mmrl.ext.none
import com.dergoogler.mmrl.wx.R
import com.dergoogler.mmrl.wx.ui.component.LocalModule
import com.dergoogler.mmrl.wx.ui.component.ModuleScope
import com.dergoogler.mmrl.wx.ui.component.NavigateUpToolbar
import com.dergoogler.mmrl.wx.ui.providable.LocalDestinationsNavigator
import com.dergoogler.mmrl.wx.util.toFormattedDateSafely
import com.dergoogler.mmrl.wx.viewmodel.FileExplorerViewModel
import com.dergoogler.mmrl.wx.viewmodel.FileItem
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import dev.mmrlx.compose.layout.card
import dev.mmrlx.compose.ui.CircularProgressIndicator
import dev.mmrlx.compose.ui.LocalTextStyle
import dev.mmrlx.compose.ui.Text
import dev.mmrlx.compose.ui.button.Button
import dev.mmrlx.compose.ui.button.ButtonVariant
import dev.mmrlx.compose.ui.dialog.Content
import dev.mmrlx.compose.ui.dialog.DialogScope
import dev.mmrlx.compose.ui.dialog.Footer
import dev.mmrlx.compose.ui.dialog.Title
import dev.mmrlx.compose.ui.dialog.rememberDialog
import dev.mmrlx.compose.ui.ext.with
import dev.mmrlx.compose.ui.icon.Icon
import dev.mmrlx.compose.ui.icon.IconButton
import dev.mmrlx.compose.ui.list.component.RawItem
import dev.mmrlx.compose.ui.list.component.item.Description
import dev.mmrlx.compose.ui.list.component.item.Start
import dev.mmrlx.compose.ui.list.component.item.Title
import dev.mmrlx.compose.ui.text.OutlinedInput
import dev.mmrlx.compose.ui.text.rememberInputState
import dev.mmrlx.compose.ui.theme.LocalContentColor
import dev.mmrlx.compose.ui.theme.MMRLXTheme
import dev.mmrlx.compose.ui.theme.ripple
import dev.mmrlx.compose.ui.toolbar.ToolbarDefaults
import dev.mmrlx.nio.SuFile
import dev.mmrlx.nio.toFormattedFileSize
import com.ramcosta.composedestinations.generated.destinations.FileEditorScreenDestination

@Destination<RootGraph>
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileExplorerScreen(
    moduleId: String,
) {
    ModuleScope(moduleId) {
        FileExplorerContent()
    }
}

@Composable
fun FileExplorerContent() {
    val module = LocalModule.current
    val navigator = LocalDestinationsNavigator.current
    val viewModel = hiltViewModel<FileExplorerViewModel>()
    val state by viewModel.state.collectAsState()
    val createDialog = rememberDialog()

    val initialPath = SuFile(module.path.moduleDir)
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

    LaunchedEffect(module) {
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

    val scrollBehavior = ToolbarDefaults.pinnedScrollBehavior()

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

    dev.mmrlx.compose.ui.scaffold.Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        toolbar = {
            dev.mmrlx.compose.ui.toolbar.Toolbar(
                title = {
                    dev.mmrlx.compose.ui.toolbar.ToolbarTitle(
                        titleContent = {
                            Text(
                                text = if (isSelectionMode) "${selectedFiles.size} selected" else module.name,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = LocalContentColor.current
                            )
                        },
                        subtitleContent = {
                            state.currentPath?.let { currentPath ->
                                PathBreadcrumb(
                                    currentPath = currentPath,
                                    onPathClick = { path ->
                                        viewModel.navigateToPath(path)
                                    }
                                )
                            }
                        }
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
                                tint = MMRLXTheme.colors.destructive
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
                        createDialog.open()
                        isFabExpanded = false
                    },
                    onCreateFile = {
                        createDialogType = CreateType.FILE
                        createDialog.open()
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
    ) {
        Box {
            dev.mmrlx.compose.ui.list.List {
                state.errorMessage?.let { error ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .card()
                            .background(MMRLXTheme.colors.destructive)
                            .padding(bottom = 8.dp),
                    ) {
                        Text(
                            text = error,
                            modifier = Modifier.padding(16.dp),
                            color = MMRLXTheme.colors.destructiveForeground
                        )
                    }
                }

                state.successMessage?.let { success ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .card()
                            .background(MMRLXTheme.colors.primary)
                            .padding(bottom = 8.dp),
                    ) {
                        Text(
                            text = success,
                            modifier = Modifier.padding(16.dp),
                            color = MMRLXTheme.colors.primaryForeground
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
                        modifier = Modifier
                            .with(this@Scaffold) {
                                it.scaffoldHazeSource("licenses")
                            },
                        contentPadding = this@Scaffold.contentPadding,
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
                                                    module.id,
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

        createDialog {
            CreateDialog(
                type = createDialogType,
                onDismiss = { createDialog.close() },
                onConfirm = { name, content ->
                    when (createDialogType) {
                        CreateType.FOLDER -> viewModel.createFolder(name)
                        CreateType.FILE -> viewModel.createFile(name, content ?: "")
                    }
                    createDialog.close()
                }
            )
        }
    }
}


@Composable
private fun ContentWrapper(
    title: String = "Error",
    content: @Composable dev.mmrlx.compose.ui.scaffold.ScaffoldScope.() -> Unit,
) {
    val navigator = LocalDestinationsNavigator.current
    dev.mmrlx.compose.ui.scaffold.Scaffold(
        toolbar = {
            NavigateUpToolbar(
                title = title,
                onBack = { navigator.popBackStack() },
            )
        },
        contentWindowInsets = WindowInsets.none,
        content = content
    )
}


@Composable
private fun ExpandableFab(
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onCreateFolder: () -> Unit,
    onCreateFile: () -> Unit,
    onImportFile: () -> Unit,
    onImportMultipleFiles: () -> Unit,
    isLoading: Boolean,
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
                    Box(
                        modifier = Modifier.card()
                    ) {
                        Text(
                            text = label,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    dev.mmrlx.compose.ui.fab.SmallFloatingActionButton(
                        onClick = action,
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
        dev.mmrlx.compose.ui.fab.ExtendedFloatingActionButton(
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
private fun DialogScope.CreateDialog(
    type: CreateType,
    onDismiss: () -> Unit,
    onConfirm: (String, String?) -> Unit,
) {

    val name = rememberInputState("")
    val content = rememberInputState("")
    val isFile = type == CreateType.FILE

    Title {
        Text(text = "Create ${if (isFile) "File" else "Folder"}")
    }

    Content {
        Column {
            OutlinedInput(
                label = { Text("${if (isFile) "File" else "Folder"} name") },
                state = name,
                modifier = Modifier
                    .fillMaxWidth(),
                lineLimits = TextFieldLineLimits.SingleLine,
            )

            if (isFile) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedInput(
                    label = { Text("Content (optional)") },
                    state = content,
                    modifier = Modifier
                        .fillMaxWidth(),
                    lineLimits = TextFieldLineLimits.MultiLine(3, 5),
                )
            }
        }
    }

    Footer {
        Button(onClick = onDismiss, variant = ButtonVariant.Outline) {
            Text("Cancel")
        }

        Button(
            onClick = {
                onConfirm(
                    name.text.toString(),
                    if (isFile) content.text.toString() else null
                )
            },
            enabled = name.text.isNotBlank(), variant = ButtonVariant.Default
        ) {
            Text("Create")
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

    while (tempPath != null) {
        pathParts.add(0, tempPath)
        tempPath = tempPath.parentSuFile
    }

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        itemsIndexed(
            items = pathParts,
            key = { index, item -> item.path + index }
        ) { index, path ->

            val density = LocalDensity.current
            val textStyle = LocalTextStyle.current

            val iconSize = Modifier.iconSize(
                density = density,
                textStyle = textStyle,
                scaling = 1.0f
            )

            if (index > 0) {
                Icon(
                    painter = painterResource(R.drawable.chevron_right),
                    contentDescription = null,
                    modifier = iconSize,
                    tint = MMRLXTheme.colors.mutedForeground
                )

                Spacer(Modifier.width(4.dp))
            }

            Text(
                text = if (index == 0 && path.name.isEmpty()) {
                    "Root"
                } else {
                    path.name
                },
                color = if (index == pathParts.lastIndex) {
                    MMRLXTheme.colors.primary
                } else {
                    MMRLXTheme.colors.mutedForeground
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
private fun dev.mmrlx.compose.ui.list.ListScope.FileItemRow(
    fileItem: FileItem,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
) {
    val interactionSource = remember { MutableInteractionSource() }

    val titleColor = when {
        isSelected -> MMRLXTheme.colors.primary
        else -> MMRLXTheme.colors.foreground
    }

    val descriptionColor = MMRLXTheme.colors.mutedForeground

    val iconTint = when {
        isSelected -> MMRLXTheme.colors.primary
        fileItem.isDirectory -> MMRLXTheme.colors.primary
        else -> MMRLXTheme.colors.mutedForeground
    }

    val itemBackground = when {
        isSelected -> MMRLXTheme.colors.accent
        else -> Color.Transparent
    }

    RawItem(
        modifier = Modifier
            .background(itemBackground)
            .combinedClickable(
                enabled = true,
                interactionSource = interactionSource,
                role = Role.Button,
                indication = ripple(),
                onClick = onClick,
                onLongClick = onLongClick
            )
            .contentPadding(),
    ) {
        Start {
            Box {
                Icon(
                    painter = painterResource(fileItem.icon),
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = iconTint
                )

                if (isSelected) {
                    Icon(
                        painter = painterResource(R.drawable.circle_check),
                        contentDescription = "Selected",
                        modifier = Modifier
                            .size(16.dp)
                            .align(Alignment.BottomEnd),
                        tint = MMRLXTheme.colors.primary
                    )
                }
            }
        }

        Title {
            Text(
                text = fileItem.name,
                style = MMRLXTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = if (fileItem.isDirectory) {
                    FontWeight.Medium
                } else {
                    FontWeight.Normal
                },
                color = titleColor
            )
        }

        Description {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (!fileItem.isDirectory && fileItem.size > 0) {
                    Text(
                        text = fileItem.size.toFormattedFileSize(),
                        style = MMRLXTheme.typography.bodySmall,
                        color = descriptionColor
                    )
                }

                Text(
                    text = fileItem.lastModified.toFormattedDateSafely,
                    style = MMRLXTheme.typography.bodySmall,
                    color = descriptionColor
                )
            }
        }
    }
}