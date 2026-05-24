// ModulesViewModel.kt
package com.dergoogler.mmrl.wx.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dergoogler.mmrl.datastore.model.ModulesMenu
import com.dergoogler.mmrl.datastore.model.Option
import com.dergoogler.mmrl.platform.content.State
import com.dergoogler.mmrl.wx.datastore.UserPreferencesRepository
import com.dergoogler.mmrl.wx.model.module.AdbPath
import com.dergoogler.mmrl.wx.model.module.Module
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.mmrlx.nio.SuFile
import dev.mmrlx.nio.SuFileSystemManager
import dev.mmrlx.nio.inputStream
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ModulesViewModel @Inject constructor(
    private val application: Application,
    private val userPreferencesRepository: UserPreferencesRepository,
) : AndroidViewModel(application) {

    val context: Context get() = application.applicationContext

    private val sourceFlow = MutableStateFlow<List<Module>>(emptyList())

    private val _isLoaded = MutableStateFlow(false)
    val isLoaded: StateFlow<Boolean> = _isLoaded.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _refreshDone = Channel<Unit>(Channel.CONFLATED)
    val refreshDone = _refreshDone.receiveAsFlow()

    private val keyFlow = MutableStateFlow("")
    val query: StateFlow<String> = keyFlow.asStateFlow()

    private val _isSearch = MutableStateFlow(false)
    val isSearch: StateFlow<Boolean> = _isSearch.asStateFlow()

    private val modulesMenu = userPreferencesRepository.data.map { it.modulesMenu }

    val local: StateFlow<List<Module>> = keyFlow
        .combine(sourceFlow) { key, source -> key to source }
        .combine(modulesMenu) { (key, source), menu ->
            val sorted = source
                .sortedWith(comparator(menu.option, menu.descending))
                .let { if (menu.pinEnabled) it.sortedByDescending { m -> m.state == State.ENABLE } else it }
                .let { if (menu.pinWebUI) it.sortedByDescending { m -> m.hasWebUI } else it }

            if (key.isBlank()) sorted
            else {
                val (newKey, prefix) = parseSearchKey(key)
                sorted.filter { module ->
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
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    init {
        viewModelScope.launch { SuFile.AutoInit(context) }
        SuFileSystemManager.isAliveFlow
            .onEach { if (it) getLocalAll() }
            .launchIn(viewModelScope)
    }

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

    private suspend fun getLocalAll() {
        _isRefreshing.value = true
        try {
            runCatching { getLocalModules() }
                .onSuccess { sourceFlow.value = it }
                .onFailure { Log.e(TAG, "Error fetching modules", it) }
        } finally {
            _isRefreshing.value = false
            _isLoaded.value = true
            _refreshDone.trySend(Unit)
        }
    }

    private fun parseSearchKey(raw: String): Pair<String, String?> {
        val prefix = listOf("id", "name", "author")
            .firstOrNull { raw.startsWith("$it:", ignoreCase = true) }
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
        val basePath = prefs.getAdbPath(context)
        val adbPath = AdbPath(basePath)

        Log.d(TAG, "BasePath=$basePath")
//        Log.d(TAG, "ModulesDir=${adbPath.modulesDir}")

        val modulesDir = SuFile(adbPath.modulesDir)

        if (!modulesDir.exists()) {
            Log.e(TAG, "Modules directory does not exist")
            return emptyList()
        }

        Log.d(TAG, "ModulesDir=${modulesDir}")

        val dirs = modulesDir.listFiles() ?: run {
            Log.e(TAG, "listFiles() returned null")
            return emptyList()
        }

        Log.d(TAG, "Dirs=${dirs}")


        return dirs.mapNotNull { dir ->
            runCatching {
                val propFile = SuFile(dir, "module.prop")
                if (!propFile.exists()) {
                    Log.w(TAG, "Missing module.prop in ${dir.path}")
                    return@mapNotNull null
                }
                Module(adbPath, Module.readProps(propFile.inputStream()))
            }.onFailure {
                Log.e(TAG, "Failed parsing module ${dir.path}", it)
            }.getOrNull()
        }
    }

    companion object {
        private const val TAG = "ModulesViewModel"
    }
}