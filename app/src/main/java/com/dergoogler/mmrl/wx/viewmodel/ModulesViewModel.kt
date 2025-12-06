package com.dergoogler.mmrl.wx.viewmodel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dergoogler.mmrl.datastore.model.ModulesMenu
import com.dergoogler.mmrl.datastore.model.Option
import com.dergoogler.mmrl.wx.datastore.UserPreferencesRepository
import com.dergoogler.mmrl.platform.Platform
import com.dergoogler.mmrl.platform.PlatformManager
import com.dergoogler.mmrl.platform.content.LocalModule
import com.dergoogler.mmrl.platform.content.LocalModule.Companion.hasAction
import com.dergoogler.mmrl.platform.content.LocalModule.Companion.hasWebUI
import com.dergoogler.mmrl.platform.content.State
import com.dergoogler.mmrl.wx.service.PlatformService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ModulesScreenState(
    val items: List<LocalModule> = listOf(),
    val isRefreshing: Boolean = false,
)

@HiltViewModel
class ModulesViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    val isProviderAlive get() = PlatformManager.isAlive

    val platform
        get() = PlatformManager.get(Platform.Unknown) {
            platform
        }

    private val sourceFlow = MutableStateFlow(listOf<LocalModule>())
    private val cacheFlow = MutableStateFlow(listOf<LocalModule>())
    private val localFlow = MutableStateFlow(listOf<LocalModule>())
    val local get() = localFlow.asStateFlow()

    private val modulesMenu
        get() = userPreferencesRepository.data.map { it.modulesMenu }

    var isSearch by mutableStateOf(false)
        private set

    private val keyFlow = MutableStateFlow("")
    val query get() = keyFlow.asStateFlow()

    init {
        providerObserver()
        dataObserver()
        keyObserver()
    }

    private fun providerObserver() {
        if (PlatformService.isActive) return

        viewModelScope.launch {
            with(PlatformManager) {
                if (platform.isNonRoot) {
                    try {
                        getLocalAll()
                    } catch (e: Exception) {
                        Log.e(TAG, "Initial load failed", e)
                    }
                }

                isAliveFlow
                    .onEach {
                        if (it) getLocalAll()

                    }.launchIn(viewModelScope)
            }
        }
    }

    private fun dataObserver() {
        sourceFlow
            .combine(modulesMenu) { list, menu ->
                if (list.isEmpty()) {
                    isLoadingFlow.update { false }
                    return@combine
                }

                cacheFlow.value = list.sortedWith(
                    comparator(menu.option, menu.descending)
                ).let { v ->
                    val a = if (menu.pinEnabled) {
                        v.sortedByDescending { it.state == State.ENABLE }
                    } else v

                    val b = if (menu.pinAction) {
                        a.sortedByDescending { it.hasAction }
                    } else a

                    if (menu.pinWebUI) {
                        b.sortedByDescending { it.hasWebUI }
                    } else b
                }

                isLoadingFlow.update { false }
            }
            .launchIn(viewModelScope)
    }

    private fun keyObserver() {
        keyFlow.combine(cacheFlow) { key, source ->
            val newKey = when {
                key.startsWith("id:", ignoreCase = true) -> key.removePrefix("id:")
                key.startsWith("name:", ignoreCase = true) -> key.removePrefix("name:")
                key.startsWith("author:", ignoreCase = true) -> key.removePrefix("author:")
                else -> key
            }.trim()

            localFlow.value = source.filter {
                if (key.isNotBlank() || newKey.isNotBlank()) {
                    when {
                        key.startsWith("id:", ignoreCase = true) ->
                            it.id.equals(newKey, ignoreCase = true)

                        key.startsWith("name:", ignoreCase = true) ->
                            it.name.equals(newKey, ignoreCase = true)

                        key.startsWith("author:", ignoreCase = true) ->
                            it.author.equals(newKey, ignoreCase = true)

                        else ->
                            it.name.contains(key, ignoreCase = true) ||
                                    it.author.contains(key, ignoreCase = true) ||
                                    it.description.contains(key, ignoreCase = true)
                    }
                } else {
                    true
                }
            }
        }.launchIn(viewModelScope)
    }

    fun search(key: String) {
        keyFlow.value = key
    }

    fun openSearch() {
        isSearch = true
    }

    fun closeSearch() {
        isSearch = false
        keyFlow.value = ""
    }

    private fun comparator(option: Option, descending: Boolean): Comparator<LocalModule> =
        if (descending) {
            when (option) {
                Option.Name -> compareByDescending { it.name.lowercase() }
                Option.UpdatedTime -> compareBy { it.lastUpdated }
                Option.Size -> compareBy { it.size }
            }
        } else {
            when (option) {
                Option.Name -> compareBy { it.name.lowercase() }
                Option.UpdatedTime -> compareByDescending { it.lastUpdated }
                Option.Size -> compareByDescending { it.size }
            }
        }

    fun setModulesMenu(value: ModulesMenu) {
        viewModelScope.launch {
            userPreferencesRepository.setModulesMenu(value)
        }
    }

    private val isLoadingFlow = MutableStateFlow(false)
    val isLoading get() = isLoadingFlow.asStateFlow()

    private inline fun <T> T.refreshing(callback: T.() -> Unit) {
        isLoadingFlow.update { true }
        callback()
        isLoadingFlow.update { false }
    }

    private fun getDefaultList() = if (PlatformManager.platform.isNonRoot) {
        PlatformManager.moduleManager.modules
    } else {
        emptyList()
    }

    fun getModules() = PlatformManager.getAsyncDeferred(
        viewModelScope,
        getDefaultList()
    ) {
        with(moduleManager) {
            modules
        }
    }

    fun getLocalAll(scope: CoroutineScope = viewModelScope) = scope.launch {
        refreshing {
            try {
                val modules = getModules()
                sourceFlow.value = modules.await()
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching modules", e)
            }
        }
    }

    private fun getLocalAllAsFlow(): StateFlow<List<LocalModule>> {
        return sourceFlow
    }

    val screenState: StateFlow<ModulesScreenState> = getLocalAllAsFlow()
        .combine(isLoadingFlow) { items, isRefreshing ->
            ModulesScreenState(items = items, isRefreshing = isRefreshing)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = ModulesScreenState()
        )

    companion object {
        private const val TAG = "ModulesViewModel"
    }
}
