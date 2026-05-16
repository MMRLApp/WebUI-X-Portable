package com.dergoogler.mmrl.wx.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import com.dergoogler.mmrl.datastore.model.ModulesMenu
import com.dergoogler.mmrl.datastore.model.Option
import com.dergoogler.mmrl.platform.content.State
import com.dergoogler.mmrl.wx.datastore.UserPreferencesRepository
import com.dergoogler.mmrl.wx.datastore.model.WorkingMode
import com.dergoogler.mmrl.wx.model.module.AdbPath
import com.dergoogler.mmrl.wx.model.module.Module
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.mmrlx.nio.SuFile
import dev.mmrlx.nio.SuFileSystemManager
import dev.mmrlx.nio.inputStream
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.InputStream
import javax.inject.Inject

data class ModulesScreenState(
    val items: List<Module> = listOf(),
    val isRefreshing: Boolean = false,
    val isFirstLoad: Boolean = true,
)

@HiltViewModel
class ModulesViewModel @Inject constructor(
    application: Application,
    private val userPreferencesRepository: UserPreferencesRepository,
) : AndroidViewModel(application) {

    val context: Context get() = application.applicationContext

    private val sourceFlow = MutableStateFlow<List<Module>>(emptyList())
    private val cacheFlow = MutableStateFlow<List<Module>>(emptyList())
    private val localFlow = MutableStateFlow<List<Module>>(emptyList())
    val local = localFlow.asStateFlow()

    private val keyFlow = MutableStateFlow("")
    val query = keyFlow.asStateFlow()

    private val isLoadingFlow = MutableStateFlow(false)
    val isLoading = isLoadingFlow.asStateFlow()

    private val _isSearch = MutableStateFlow(false)
    val isSearch = _isSearch.asStateFlow()

    private val modulesMenu = userPreferencesRepository.data.map { it.modulesMenu }

    val screenState: StateFlow<ModulesScreenState> = sourceFlow
        .combine(isLoadingFlow) { items, isRefreshing ->
            ModulesScreenState(
                items = items,
                isRefreshing = isRefreshing,
                isFirstLoad = isRefreshing && items.isEmpty(),
            )
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, ModulesScreenState(isFirstLoad = true))

    init {
        providerObserver()
        dataObserver()
        keyObserver()
    }

    // Observers

    private fun providerObserver() {
        SuFileSystemManager.isAliveFlow
            .onEach { if (it) getLocalAll() }
            .launchIn(viewModelScope)
    }

    private fun dataObserver() {
        sourceFlow
            .combine(modulesMenu) { list, menu ->
                if (list.isEmpty()) return@combine

                cacheFlow.value = list
                    .sortedWith(comparator(menu.option, menu.descending))
                    .let { sorted ->
                        if (menu.pinEnabled) sorted.sortedByDescending { it.state == State.ENABLE }
                        else sorted
                    }
                    .let { sorted ->
                        if (menu.pinWebUI) sorted.sortedByDescending { it.hasWebUI }
                        else sorted
                    }
            }
            .launchIn(viewModelScope)
    }

    private fun keyObserver() {
        keyFlow
            .combine(cacheFlow) { key, source ->
                val (newKey, prefix) = parseSearchKey(key)
                localFlow.value = if (key.isBlank()) {
                    source
                } else {
                    source.filter { module ->
                        when (prefix) {
                            "id" -> module.id.equals(newKey, ignoreCase = true)
                            "name" -> module.name.equals(newKey, ignoreCase = true)
                            "author" -> module.author.equals(newKey, ignoreCase = true)
                            else -> module.name.contains(key, ignoreCase = true) ||
                                    module.author.contains(key, ignoreCase = true) ||
                                    module.description.contains(key, ignoreCase = true)
                        }
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    // Public API

    fun search(key: String) {
        keyFlow.value = key
    }

    fun openSearch() {
        _isSearch.value = true
    }

    fun closeSearch() {
        _isSearch.value = false; keyFlow.value = ""
    }

    fun setModulesMenu(value: ModulesMenu) {
        viewModelScope.launch { userPreferencesRepository.setModulesMenu(value) }
    }

    fun refreshModules() {
        viewModelScope.launch { getLocalAll() }
    }

    suspend fun getLocalAll() {
        withRefreshing {
            runCatching { getLocalModules() }
                .onSuccess { sourceFlow.value = it }
                .onFailure { Log.e(TAG, "Error fetching modules", it) }
        }
    }

    // Private helpers

    private suspend fun withRefreshing(block: suspend () -> Unit) {
        isLoadingFlow.update { true }
        try {
            block()
        } finally {
            isLoadingFlow.update { false }
        }
    }

    private fun parseSearchKey(raw: String): Pair<String, String?> {
        val prefixes = listOf("id", "name", "author")
        val prefix = prefixes.firstOrNull { raw.startsWith("$it:", ignoreCase = true) }
        return if (prefix != null) raw.removePrefix("$prefix:").trim() to prefix
        else raw.trim() to null
    }

    private fun comparator(option: Option, descending: Boolean): Comparator<Module> {
        val base: Comparator<Module> = when (option) {
            Option.Name -> compareBy { it.name.lowercase() }
            Option.UpdatedTime -> compareByDescending { it.lastUpdated }
            Option.Size -> compareByDescending { it.size }
        }
        return if (descending) base.reversed() else base
    }

    suspend fun getLocalModules(): List<Module> {
        val prefs = userPreferencesRepository.data.first()
        val isNonRoot = prefs.workingMode == WorkingMode.MODE_NON_ROOT

        val basePath =
            (if (isNonRoot) context.filesDir.path else prefs.adbPath) ?: return emptyList()
        val adbPath = AdbPath(basePath)
        val modulesDir = SuFile.async(adbPath.modulesDir)

        if (!modulesDir.exists()) {
            modulesDir.mkdirs()
        }

        return modulesDir
            .listFiles()
            .map { dir ->
                val props = SuFile.async(dir, "module.prop")
                    .inputStream()
                    .use { readProps(it) }
                Module(adbPath, props)
            }
    }

    private fun readProps(input: InputStream): Map<String, String> =
        input.bufferedReader().useLines { lines ->
            lines.mapNotNull { line ->
                val idx = line.indexOf('=')
                if (idx > 0) line.substring(0, idx).trim() to line.substring(idx + 1).trim()
                else null
            }.toMap()
        }

    companion object {
        private const val TAG = "ModulesViewModel"
    }
}