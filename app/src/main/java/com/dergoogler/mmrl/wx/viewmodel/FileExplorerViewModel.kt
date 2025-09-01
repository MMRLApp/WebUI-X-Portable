@file:Suppress("unused")

package com.dergoogler.mmrl.wx.viewmodel

import androidx.annotation.DrawableRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dergoogler.mmrl.platform.file.SuFile
import com.dergoogler.mmrl.wx.R
import com.dergoogler.mmrl.wx.datastore.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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
    val canGoBack: Boolean = false,
)

@HiltViewModel
class FileExplorerViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
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
            canGoBack = true
        )

        loadFiles(path)
    }

    fun refresh() {
        val currentPath = _state.value.currentPath ?: return
        _state.value = _state.value.copy(
            isLoading = true,
            errorMessage = null
        )

        loadFiles(currentPath)
    }

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

val SuFile.parentSuFile: SuFile?
    get() = try {
        val parent = this.parentFile
        if (parent != null && parent.path != this.path) {
            SuFile(parent)
        } else {
            null
        }
    } catch (e: Exception) {
        null
    }