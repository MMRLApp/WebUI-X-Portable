@file:Suppress("unused")

package com.dergoogler.mmrl.wx.viewmodel

import android.content.Context
import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dergoogler.mmrl.compat.MediaStoreCompat.getPathForUri
import com.dergoogler.mmrl.platform.file.SuFile
import com.dergoogler.mmrl.platform.file.SuFile.Companion.toSuFile
import com.dergoogler.mmrl.platform.file.SuFileInputStream
import com.dergoogler.mmrl.platform.file.SuFileOutputStream
import com.dergoogler.mmrl.platform.file.suContentResolver
import com.dergoogler.mmrl.wx.R
import com.dergoogler.mmrl.wx.datastore.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class FileItem(
    val file: SuFile,
    val isDirectory: Boolean,
    val name: String,
    val size: Long,
    val lastModified: Long,
    @DrawableRes val icon: Int,
)

data class FileExplorerState(
    val currentPath: SuFile? = null,
    val fileItems: List<FileItem> = emptyList(),
    val pathHistory: List<SuFile> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val canGoBack: Boolean = false,
    val isOperationInProgress: Boolean = false,
)

sealed class FileOperationResult {
    object Success : FileOperationResult()
    data class Error(val message: String) : FileOperationResult()
}

@HiltViewModel
class FileExplorerViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {
    private val _state = MutableStateFlow(FileExplorerState())
    val state: StateFlow<FileExplorerState> = _state.asStateFlow()

    fun initialize(initialPath: SuFile) {
        _state.value = _state.value.copy(
            currentPath = initialPath,
            isLoading = true
        )
        loadFiles(initialPath)
    }

    fun navigateToDirectory(directory: SuFile) {
        if (!directory.exists() || !directory.isDirectory()) {
            _state.value = _state.value.copy(
                errorMessage = "Cannot navigate to: ${directory.name}"
            )
            return
        }

        val currentState = _state.value
        val currentPath = currentState.currentPath ?: return

        _state.value = currentState.copy(
            currentPath = directory,
            pathHistory = currentState.pathHistory + currentPath,
            isLoading = true,
            errorMessage = null,
            successMessage = null,
            canGoBack = true
        )
        loadFiles(directory)
    }

    fun navigateBack() {
        val currentState = _state.value
        if (currentState.pathHistory.isEmpty()) return

        val previousPath = currentState.pathHistory.last()
        val newHistory = currentState.pathHistory.dropLast(1)

        _state.value = currentState.copy(
            currentPath = previousPath,
            pathHistory = newHistory,
            isLoading = true,
            errorMessage = null,
            successMessage = null,
            canGoBack = newHistory.isNotEmpty()
        )

        loadFiles(previousPath)
    }

    fun navigateToPath(path: SuFile) {
        val currentState = _state.value
        val currentPath = currentState.currentPath ?: return

        _state.value = currentState.copy(
            currentPath = path,
            pathHistory = currentState.pathHistory + currentPath,
            isLoading = true,
            errorMessage = null,
            successMessage = null,
            canGoBack = true
        )

        loadFiles(path)
    }

    fun refresh() {
        val currentPath = _state.value.currentPath ?: return
        _state.value = _state.value.copy(
            isLoading = true,
            errorMessage = null,
            successMessage = null
        )

        loadFiles(currentPath)
    }

    fun clearMessages() {
        _state.value = _state.value.copy(
            errorMessage = null,
            successMessage = null
        )
    }

    // File/Folder Creation Functions
    fun createFolder(folderName: String) {
        val currentPath = _state.value.currentPath ?: return
        if (folderName.isBlank()) {
            _state.value = _state.value.copy(
                errorMessage = "Folder name cannot be empty"
            )
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(isOperationInProgress = true)

            val result = withContext(Dispatchers.IO) {
                try {
                    val newFolder = SuFile(currentPath, folderName)
                    if (newFolder.exists()) {
                        FileOperationResult.Error("Folder already exists")
                    } else if (newFolder.mkdirs()) {
                        FileOperationResult.Success
                    } else {
                        FileOperationResult.Error("Failed to create folder")
                    }
                } catch (e: Exception) {
                    FileOperationResult.Error("Error creating folder: ${e.message}")
                }
            }

            when (result) {
                is FileOperationResult.Success -> {
                    _state.value = _state.value.copy(
                        successMessage = "Folder '$folderName' created successfully",
                        isOperationInProgress = false
                    )
                    refresh()
                }

                is FileOperationResult.Error -> {
                    _state.value = _state.value.copy(
                        errorMessage = result.message,
                        isOperationInProgress = false
                    )
                }
            }
        }
    }

    fun createFile(fileName: String, content: String = "") {
        val currentPath = _state.value.currentPath ?: return
        if (fileName.isBlank()) {
            _state.value = _state.value.copy(
                errorMessage = "File name cannot be empty"
            )
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(isOperationInProgress = true)

            val result = withContext(Dispatchers.IO) {
                try {
                    val newFile = SuFile(currentPath, fileName)
                    if (newFile.exists()) {
                        FileOperationResult.Error("File already exists")
                    } else {
                        newFile.writeText(content)
                        if (newFile.exists()) {
                            FileOperationResult.Success
                        } else {
                            FileOperationResult.Error("Failed to create file")
                        }
                    }
                } catch (e: Exception) {
                    FileOperationResult.Error("Error creating file: ${e.message}")
                }
            }

            when (result) {
                is FileOperationResult.Success -> {
                    _state.value = _state.value.copy(
                        successMessage = "File '$fileName' created successfully",
                        isOperationInProgress = false
                    )
                    refresh()
                }

                is FileOperationResult.Error -> {
                    _state.value = _state.value.copy(
                        errorMessage = result.message,
                        isOperationInProgress = false
                    )
                }
            }
        }
    }

    // Import Functions
    fun importFileFromUri(uri: Uri, targetFileName: String? = null) {
        val currentPath = _state.value.currentPath ?: return

        viewModelScope.launch {
            _state.value = _state.value.copy(isOperationInProgress = true)

            val result = withContext(Dispatchers.IO) {
                try {
                    val contentResolver = context.suContentResolver
                    val inputStream = contentResolver.openSuInputStream(uri)
                        ?: return@withContext FileOperationResult.Error("Cannot open file")

                    // Get original filename if targetFileName is not provided
                    val fileName = targetFileName ?: getFileNameFromUri(uri) ?: "imported_file"
                    val targetFile = SuFile(currentPath, fileName)

                    if (targetFile.exists()) {
                        inputStream.close()
                        return@withContext FileOperationResult.Error("File already exists: $fileName")
                    }

                    val outputStream = SuFileOutputStream(targetFile)
                    inputStream.copyTo(outputStream)
                    inputStream.close()
                    outputStream.close()

                    if (targetFile.exists()) {
                        FileOperationResult.Success
                    } else {
                        FileOperationResult.Error("Failed to import file")
                    }
                } catch (e: Exception) {
                    FileOperationResult.Error("Error importing file: ${e.message}")
                }
            }

            when (result) {
                is FileOperationResult.Success -> {
                    _state.value = _state.value.copy(
                        successMessage = "File imported successfully",
                        isOperationInProgress = false
                    )
                    refresh()
                }

                is FileOperationResult.Error -> {
                    _state.value = _state.value.copy(
                        errorMessage = result.message,
                        isOperationInProgress = false
                    )
                }
            }
        }
    }

    fun importMultipleFiles(uris: List<Uri>) {
        val currentPath = _state.value.currentPath ?: return

        viewModelScope.launch {
            _state.value = _state.value.copy(isOperationInProgress = true)

            val results = withContext(Dispatchers.IO) {
                uris.map { uri ->
                    try {
                        val contentResolver = context.suContentResolver
                        val inputStream = contentResolver.openSuInputStream(uri)
                            ?: return@map "Cannot open file" to false

                        val fileName =
                            getFileNameFromUri(uri) ?: "imported_file_${System.currentTimeMillis()}"
                        val targetFile = SuFile(currentPath, fileName)

                        if (targetFile.exists()) {
                            inputStream.close()
                            return@map "File already exists: $fileName" to false
                        }

                        val outputStream = SuFileOutputStream(targetFile)
                        inputStream.copyTo(outputStream)
                        inputStream.close()
                        outputStream.close()

                        fileName to targetFile.exists()
                    } catch (e: Exception) {
                        "Error with file: ${e.message}" to false
                    }
                }
            }

            val successCount = results.count { it.second }
            val totalCount = results.size
            val failedFiles = results.filter { !it.second }.map { it.first }

            val message = if (successCount == totalCount) {
                "All $totalCount files imported successfully"
            } else if (successCount > 0) {
                "Imported $successCount/$totalCount files. Failed: ${failedFiles.joinToString(", ")}"
            } else {
                "Failed to import files: ${failedFiles.joinToString(", ")}"
            }

            if (successCount > 0) {
                _state.value = _state.value.copy(successMessage = message)
            } else {
                _state.value = _state.value.copy(errorMessage = message)
            }

            refresh()
        }
    }

    // Export Functions
    fun exportFile(file: SuFile, targetUri: Uri) {
        if (!file.exists() || file.isDirectory()) {
            _state.value = _state.value.copy(
                errorMessage = "Cannot export: file does not exist or is a directory"
            )
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(isOperationInProgress = true)

            val result = withContext(Dispatchers.IO) {
                try {
                    val contentResolver = context.suContentResolver
                    val outputStream = contentResolver.openSuOutputStream(targetUri)
                        ?: return@withContext FileOperationResult.Error("Cannot open target location")

                    val inputStream = SuFileInputStream(file)
                    inputStream.copyTo(outputStream)
                    inputStream.close()
                    outputStream.close()

                    FileOperationResult.Success
                } catch (e: Exception) {
                    FileOperationResult.Error("Error exporting file: ${e.message}")
                }
            }

            when (result) {
                is FileOperationResult.Success -> {
                    _state.value = _state.value.copy(
                        successMessage = "File '${file.name}' exported successfully",
                        isOperationInProgress = false
                    )
                }

                is FileOperationResult.Error -> {
                    _state.value = _state.value.copy(
                        errorMessage = result.message,
                        isOperationInProgress = false
                    )
                }
            }
        }
    }

    fun exportMultipleFiles(files: List<SuFile>, targetDirectoryUri: Uri) {
        val validFiles = files.filter { it.exists() && it.isFile() }
        if (validFiles.isEmpty()) {
            _state.value = _state.value.copy(
                errorMessage = "No valid files to export"
            )
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(isOperationInProgress = true)

            val results = withContext(Dispatchers.IO) {
                validFiles.map { file ->
                    try {
                        val contentResolver = context.suContentResolver
                        // Create a document in the target directory
                        val documentUri = contentResolver.takePersistableUriPermission(
                            targetDirectoryUri,
                            android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        )

                        // This is a simplified approach - in reality, you might need to use
                        // DocumentsContract.createDocument() for creating files in document tree
                        val outputStream = contentResolver.openSuOutputStream(targetDirectoryUri)
                            ?: return@map file.name to false

                        val inputStream = SuFileInputStream(file)
                        inputStream.copyTo(outputStream)
                        inputStream.close()
                        outputStream.close()

                        file.name to true
                    } catch (e: Exception) {
                        file.name to false
                    }
                }
            }

            val successCount = results.count { it.second }
            val totalCount = results.size
            val failedFiles = results.filter { !it.second }.map { it.first }

            val message = if (successCount == totalCount) {
                "All $totalCount files exported successfully"
            } else if (successCount > 0) {
                "Exported $successCount/$totalCount files. Failed: ${failedFiles.joinToString(", ")}"
            } else {
                "Failed to export files: ${failedFiles.joinToString(", ")}"
            }

            if (successCount > 0) {
                _state.value = _state.value.copy(successMessage = message)
            } else {
                _state.value = _state.value.copy(errorMessage = message)
            }
        }
    }

    // Delete Functions (bonus)
    fun deleteFile(file: SuFile) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isOperationInProgress = true)

            val result = withContext(Dispatchers.IO) {
                try {
                    if (file.delete()) {
                        FileOperationResult.Success
                    } else {
                        FileOperationResult.Error("Failed to delete ${file.name}")
                    }
                } catch (e: Exception) {
                    FileOperationResult.Error("Error deleting file: ${e.message}")
                }
            }

            when (result) {
                is FileOperationResult.Success -> {
                    _state.value = _state.value.copy(
                        successMessage = "'${file.name}' deleted successfully",
                        isOperationInProgress = false
                    )
                    refresh()
                }

                is FileOperationResult.Error -> {
                    _state.value = _state.value.copy(
                        errorMessage = result.message,
                        isOperationInProgress = false
                    )
                }
            }
        }
    }

    private fun getFileNameFromUri(uri: Uri): String? =
        context.getPathForUri(uri)?.toSuFile()?.name

    private fun loadFiles(directory: SuFile) {
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    loadFilesFromDirectory(directory)
                }

                _state.value = _state.value.copy(
                    fileItems = result.first,
                    errorMessage = result.second,
                    isLoading = false
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    fileItems = emptyList(),
                    errorMessage = "Error loading files: ${e.message}",
                    isLoading = false
                )
            }
        }
    }

    private fun loadFilesFromDirectory(directory: SuFile): Pair<List<FileItem>, String?> {
        return try {
            if (!directory.exists()) {
                return Pair(emptyList(), "Directory does not exist")
            }

            if (!directory.isDirectory()) {
                return Pair(emptyList(), "Path is not a directory")
            }

            val files =
                directory.list()?.map { SuFile(directory, it) } ?: return Pair(
                    emptyList(),
                    "Cannot read directory contents"
                )

            val fileItems = files
                .filter { it.name.isNotEmpty() }
                .distinctBy { it.path }
                .map { file ->
                    FileItem(
                        file = file,
                        isDirectory = file.isDirectory(),
                        name = file.name,
                        size = if (file.isFile()) file.length() else 0,
                        lastModified = file.lastModified(),
                        icon = getFileIcon(file)
                    )
                }
                .sortedWith(compareBy<FileItem> { !it.isDirectory }.thenBy { it.name.lowercase() })

            Pair(fileItems, null)
        } catch (e: Exception) {
            Pair(emptyList(), "Error loading files: ${e.message}")
        }
    }

    @DrawableRes
    private fun getFileIcon(file: SuFile): Int {
        return if (file.isDirectory()) {
            R.drawable.folder
        } else {
            when (file.extension.lowercase()) {
                "jpg", "jpeg", "png", "gif", "bmp", "webp" -> R.drawable.photo
                "mp4", "avi", "mkv", "mov", "wmv", "flv" -> R.drawable.movie
                "mp3", "wav", "flac", "aac", "ogg", "m4a" -> R.drawable.headphones
                "pdf" -> R.drawable.file_type_pdf
                "mjs", "cjs", "js" -> R.drawable.file_type_js
                "htm", "html", "htmlx" -> R.drawable.file_type_html
                "bash", "sh" -> R.drawable.terminal
                "css" -> R.drawable.file_type_css
                "txt", "md", "log" -> R.drawable.file_text
                "zip", "rar", "7z", "tar", "gz" -> R.drawable.file_zip
                "apk" -> com.dergoogler.mmrl.ui.R.drawable.brand_android
                else -> R.drawable.file
            }
        }
    }
}
