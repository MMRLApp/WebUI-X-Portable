package com.dergoogler.mmrl.wx.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dergoogler.mmrl.datastore.model.ModulesMenu
import com.dergoogler.mmrl.datastore.model.Option
import com.dergoogler.mmrl.platform.Platform
import com.dergoogler.mmrl.platform.PlatformManager
import com.dergoogler.mmrl.platform.file.Path
import com.dergoogler.mmrl.wx.datastore.UserPreferencesRepository
import com.dergoogler.mmrl.wx.model.module.Module
import com.dergoogler.mmrl.wx.model.module.ModulePaths
import com.dergoogler.mmrl.wx.model.module.ModuleState
import com.dergoogler.mmrl.wx.util.getNonRootBaseDir
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.mmrlx.thread.RootCallable
import dev.mmrlx.thread.ktx.asThread
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class ModulesScreenState(
    val items: List<Module> = emptyList(),
    val isLoading: Boolean = false,
)

@HiltViewModel
class ModulesViewModel @Inject constructor(
    private val application: Application,
    private val userPreferencesRepository: UserPreferencesRepository,
) : AndroidViewModel(application) {

    val context: Context get() = application.applicationContext

    val isProviderAlive get() = PlatformManager.isAlive

    val platform get() = PlatformManager.get(Platform.Unknown) { platform }

    private val sourceFlow = MutableStateFlow<List<Module>>(emptyList())
    private val sortedFlow = MutableStateFlow<List<Module>>(emptyList())
    private val filteredFlow = MutableStateFlow<List<Module>>(emptyList())

    val modules: StateFlow<List<Module>> = filteredFlow

    private val modulesMenu = userPreferencesRepository.data.map { it.modulesMenu }

    var isSearch by mutableStateOf(false)
        private set

    private val keyFlow = MutableStateFlow("")
    val query: StateFlow<String> get() = keyFlow

    private val isLoadingFlow = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> get() = isLoadingFlow

    init {
        observePlatform()
        observeSourceAndMenu()
        observeKeyAndSorted()
    }

    private fun observePlatform() {
        viewModelScope.launch {
            with(PlatformManager) {
                // Eagerly load non-root modules on start
                if (platform.isNonRoot) {
                    runCatching { getLocalAll() }
                        .onFailure { Log.e(TAG, "Initial non-root load failed", it) }
                }

                isAliveFlow
                    .onEach { if (it) getLocalAll() }
                    .launchIn(viewModelScope)
            }
        }
    }

    private fun observeSourceAndMenu() {
        sourceFlow
            .combine(modulesMenu) { list, menu ->
                if (list.isEmpty()) return@combine

                val sorted = list.sortedWith(comparator(menu.option, menu.descending))
                sortedFlow.value = if (menu.pinEnabled) {
                    sorted.sortedByDescending { it.state == ModuleState.Enable }
                } else {
                    sorted
                }
            }
            .launchIn(viewModelScope)
    }

    private fun observeKeyAndSorted() {
        keyFlow
            .combine(sortedFlow) { key, source ->
                filteredFlow.value = if (key.isBlank()) {
                    source
                } else {
                    source.filter { it.matchesQuery(key) }
                }
            }
            .launchIn(viewModelScope)
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

    private fun comparator(option: Option, descending: Boolean): Comparator<Module> =
        when (option) {
            Option.Name -> compareBy<Module> { it.name?.lowercase() }
            Option.UpdatedTime -> compareBy { it.lastUpdated }
            Option.Size -> compareBy { it.size }
        }.let { if (descending) reverseOrder<Module>().then(it).reversed() else it }
            // Simpler, correct way:
            .let { base -> if (descending) Comparator { a, b -> base.compare(b, a) } else base }

    fun setModulesMenu(value: ModulesMenu) {
        viewModelScope.launch { userPreferencesRepository.setModulesMenu(value) }
    }

    fun getLocalAll(scope: CoroutineScope = viewModelScope) = scope.launch {
        isLoadingFlow.update { true }
        try {
            sourceFlow.value = when {
                platform.isNonRoot -> loadNonRootModules(context)
                else -> loadRootModules.asThread()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching modules", e)
        } finally {
            // Always clear loading, even on exception
            isLoadingFlow.update { false }
        }
    }

    val screenState: StateFlow<ModulesScreenState> =
        filteredFlow.combine(isLoadingFlow) { items, loading ->
            ModulesScreenState(items = items, isLoading = loading)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ModulesScreenState(),
        )

    companion object {
        @PublishedApi
        internal const val TAG = "ModulesViewModel"

        private fun Module.matchesQuery(raw: String): Boolean {
            val (prefix, term) = raw.parseSearchQuery()
            return when (prefix) {
                "id:" -> id.equals(term, ignoreCase = true)
                "name:" -> name.equals(term, ignoreCase = true)
                "author:" -> author.equals(term, ignoreCase = true)
                else -> name?.contains(raw, ignoreCase = true) == true
                        || author?.contains(raw, ignoreCase = true) == true
                        || description?.contains(raw, ignoreCase = true) == true
            }
        }

        private fun String.parseSearchQuery(): Pair<String, String> {
            val knownPrefixes = listOf("id:", "name:", "author:")
            val prefix = knownPrefixes.firstOrNull { startsWith(it, ignoreCase = true) } ?: ""
            return prefix to removePrefix(prefix).trim()
        }

        inline fun <reified T> Map<String, String?>?.prop(
            key: String,
            vararg alias: String,
            default: T,
        ): T {
            val value = (sequenceOf(key) + alias.asSequence())
                .firstNotNullOfOrNull { this?.get(it) }
                ?: return default

            @Suppress("UNCHECKED_CAST")
            return try {
                when (T::class) {
                    Int::class -> value.toInt() as T
                    Boolean::class -> value.toBoolean() as T
                    Double::class -> value.toDouble() as T
                    String::class -> value as T
                    else -> {
                        Log.e(TAG, "Unsupported prop type: ${T::class}"); default
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Prop parse failed for '$key': ${e.message}")
                default
            }
        }

        val loadRootModules: RootCallable<List<Module>> = RootCallable {
            listModules(File("/data/adb"))
        }

        val loadNonRootModules: (Context) -> List<Module> = { context ->
            listModules(context.getNonRootBaseDir())
        }

        fun listModules(baseDir: File): List<Module> {
            val modulesDir = File(baseDir, "modules")
            return modulesDir.listFiles()
                ?.filter { it.isDirectory }
                ?.mapNotNull { dir -> dir.toModule(baseDir) }
                .orEmpty()
        }

        private fun File.resolve(pathname: String?): String? {
            if (pathname == null) return null
            val resolvedPath = Path.resolve(absolutePath, pathname)
            val file = File(resolvedPath)
            if (file.exists()) return file.absolutePath
            return null
        }

        private fun File.toModule(baseDir: File): Module? {
            val propFile = File(this, "module.prop")
            if (!propFile.exists()) return null

            val prop: Map<String, String?> = propFile.readLines()
                .mapNotNull { line ->
                    val idx = line.indexOf('=')
                    if (idx < 0) null else line.substring(0, idx).trim() to line.substring(idx + 1)
                        .trim()
                }
                .toMap()

            val state = when {
                File(this, "remove").exists() -> ModuleState.Remove
                File(this, "disable").exists() -> ModuleState.Disable
                File(this, "update").exists() -> ModuleState.Update
                else -> ModuleState.Enable
            }

            val id = prop.prop("id", default = name)
            val paths = ModulePaths(baseDir.path, id)
            val lastUpdated = (paths.files + paths.serviceFiles)
                .map(::File)
                .filter(File::exists)
                .maxOfOrNull(File::lastModified) ?: 0L

            val bannerPath: String? = prop.prop("banner", alias = arrayOf("cover"), default = null)
            val iconPath: String? =
                prop.prop("webUiIconPath", alias = arrayOf("iconPath"), default = null)

            return Module(
                id = id,
                name = prop.prop("name", default = "<no-name>"),
                version = prop.prop("version", default = "<no-version>"),
                versionCode = prop.prop("versionCode", default = -2),
                author = prop.prop("author", default = "<no-author>"),
                description = prop.prop("description", default = "<no-description>"),
                path = absolutePath,
                state = state,
                metaModule = prop.prop("metamodule", default = 0) != 0
                        || prop.prop("metamodule", default = false),
                banner = resolve(bannerPath),
                iconPath = resolve(iconPath),
                size = folderSize(),
                paths = paths,
                hasWebUI = File(paths.webrootDir, "index.html").exists(),
                lastUpdated = lastUpdated,
            )
        }

        fun File.folderSize(): Long =
            takeIf { exists() && isDirectory }
                ?.walkTopDown()
                ?.filter(File::isFile)
                ?.sumOf(File::length)
                ?: 0L

        val loadModuleBanner = RootCallable<ByteArray?> {
            val bannerPath = it.args.get<String>("bannerPath") ?: return@RootCallable null
            val bannerFile = File(bannerPath)

            return@RootCallable if (bannerFile.exists()) {
                bannerFile.readBytes()
            } else {
                null
            }
        }
    }
}