package com.dergoogler.mmrl.webui.model

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.BitmapFactory
import android.graphics.drawable.Icon
import android.os.Process
import android.util.Log
import android.widget.Toast
import com.dergoogler.mmrl.platform.PlatformManager
import com.dergoogler.mmrl.platform.content.LocalModule
import com.dergoogler.mmrl.platform.file.SuFile
import com.dergoogler.mmrl.platform.hiddenApi.HiddenPackageManager
import com.dergoogler.mmrl.platform.hiddenApi.HiddenUserManager
import com.dergoogler.mmrl.platform.model.ModId
import com.dergoogler.mmrl.platform.model.ModId.Companion.moduleConfigDir
import com.dergoogler.mmrl.platform.model.ModId.Companion.putModId
import com.dergoogler.mmrl.platform.model.ModId.Companion.webrootDir
import com.dergoogler.mmrl.webui.R
import com.dergoogler.mmrl.webui.activity.WXActivity
import com.dergoogler.mmrl.webui.interfaces.WXInterface
import com.dergoogler.mmrl.webui.moshi
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Types
import dalvik.system.BaseDexClassLoader
import dalvik.system.DexClassLoader
import dalvik.system.InMemoryDexClassLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap


object WebUIPermissions {
    const val PLUGIN_DEX_LOADER = "webui.permission.PLUGIN_DEX_LOADER"
    const val DSL_DEX_LOADING = "webui.permission.DSL_DEX_LOADING"
    const val WX_ROOT_PATH = "wx.permission.ROOT_PATH"
}

/**
 * Represents the required version information for interacting with the Web UI.
 *
 * This data class specifies the minimum version of the Web UI that the client must be using,
 * along with optional supporting text and a link for the user to get help or updates.
 *
 * @property required The minimum required version number (an integer). Defaults to 1.
 *                    Clients with a Web UI version lower than this value should be prompted to upgrade.
 * @property supportText Optional text providing additional context or instructions to the user.
 *                       For example: "Please update to the latest version for the best experience."
 * @property supportLink Optional URL link where the user can find more information about the
 *                       required version, such as download instructions or release notes.
 *                       For example: "https://example.com/webui-update"
 */
@JsonClass(generateAdapter = true)
data class WebUIConfigRequireVersion(
    val required: Int = 1,
    val supportText: String? = null,
    val supportLink: String? = null,
)

@JsonClass(generateAdapter = true)
data class WebUIConfigRequireVersionPackages(
    val code: Int = -1,
    val packageName: Any,
    val supportText: String? = null,
    val supportLink: String? = null,
) {
    val packageNames
        get(): List<String> {
            return when (packageName) {
                is String -> listOf(packageName)
                is List<*> -> packageName.filterIsInstance<String>()
                else -> emptyList()
            }
        }
}

/**
 * Represents the required configuration for the Web UI.
 *
 * This data class defines the minimum required configuration settings needed for the Web UI to function correctly.
 * Currently, it only includes the required version information.
 *
 * @property version The required version details for the Web UI. Defaults to a new [WebUIConfigRequireVersion] instance.
 */
@JsonClass(generateAdapter = true)
data class WebUIConfigRequire(
    val packages: List<WebUIConfigRequireVersionPackages> = emptyList(),
    val version: WebUIConfigRequireVersion = WebUIConfigRequireVersion(),
)

@JsonClass(generateAdapter = false)
enum class DexSourceType {
    @Json(name = "dex")
    DEX,

    @Json(name = "apk")
    APK
}

private val interfaceCache = ConcurrentHashMap<String, JavaScriptInterface<out WXInterface>>()

@JsonClass(generateAdapter = true)
data class WebUIConfigDexFile(
    val type: DexSourceType = DexSourceType.DEX,
    val path: String? = null,
    val className: String? = null,
    val cache: Boolean = true,
    val sharedObjects: List<String> = emptyList(),
) {
    private companion object {
        const val TAG = "WebUIConfigDexFile"
    }

    @SuppressLint("DiscouragedPrivateApi")
    private fun loadLibraries(files: List<SuFile>) {
        val libs = files.map { file ->
            if (!file.exists()) return
            if (file.extension != "so") return

            try {
                file.setOwner(Process.myUid(), Process.myUid())
            } catch (e: Exception) {
                Log.e(TAG, "Error setting owner for ${file.path}", e)
            }

            file.absolutePath
        }

        SuFile.loadSharedObjects(*libs.toTypedArray())
    }

    /**
     * Loads and instantiates a JavaScript interface from a DEX or APK file.
     *
     * @param context The Android Context.
     * @param modId The ID of the mod providing the web root.
     * @param interfaceCache A thread-safe cache to store and retrieve loaded interfaces,
     * preventing redundant and expensive file operations.
     * @return The instantiated JavaScriptInterface, or null if loading fails.
     */
    fun getInterface(
        context: Context,
        modId: ModId,
    ): JavaScriptInterface<out WXInterface>? {
        // Use guard clauses for cleaner validation at the start.
        val currentClassName = className ?: return null
        val currentPath = path ?: return null

        if (cache) {
            // 1. Check cache first for immediate retrieval.
            interfaceCache[currentClassName]?.let { return it }
        }

        return try {
            // 2. Create the appropriate class loader.
            val loader = when (type) {
                DexSourceType.DEX -> createDexLoader(context, modId, currentPath)
                DexSourceType.APK -> createApkLoader(context, currentPath)
            } ?: return null // Return null if loader creation failed.

            // 3. Load the class and create an instance.
            val rawClass = loader.loadClass(currentClassName)
            if (!WXInterface::class.java.isAssignableFrom(rawClass)) {
                Log.e(TAG, "Loaded class $currentClassName does not implement WXInterface")
                return null
            }

            @Suppress("UNCHECKED_CAST") val clazz = rawClass as Class<out WXInterface>

            val sharedObjectFiles = sharedObjects.map { SuFile(modId.webrootDir, it) }
            loadLibraries(sharedObjectFiles)

            val instance = JavaScriptInterface(clazz)

            // 4. Cache the new instance and return it.
            interfaceCache.putIfAbsent(currentClassName, instance)
            instance
        } catch (e: ClassNotFoundException) {
            Log.e(TAG, "Class $currentClassName not found in path: $currentPath", e)
            null
        } catch (e: Exception) {
            // Generic catch for any other instantiation or loading errors.
            Log.e(TAG, "Error loading class $currentClassName from path: $currentPath", e)
            null
        }
    }

    /**
     * Creates a ClassLoader for a standalone .dex file.
     */
    private fun createDexLoader(
        context: Context,
        modId: ModId,
        dexPath: String,
    ): BaseDexClassLoader? {
        val file = SuFile(modId.webrootDir, dexPath)

        if (!file.isFile || file.extension != "dex") {
            Log.e(TAG, "Provided path is not a valid .dex file: ${file.path}")
            return null
        }

        // Using InMemoryDexClassLoader is efficient if DEX files are not excessively large.
        val dexFileBytes = file.readBytes()
        return InMemoryDexClassLoader(ByteBuffer.wrap(dexFileBytes), context.classLoader)
    }

    /**
     * Creates a ClassLoader for a class within an installed APK.
     */
    private fun createApkLoader(context: Context, packageName: String): BaseDexClassLoader? {
        return try {
            val pm: HiddenPackageManager = PlatformManager.packageManager
            val um: HiddenUserManager = PlatformManager.userManager
            val appInfo = pm.getApplicationInfo(packageName, um.myUserId, 0)
            val apkPath = appInfo.sourceDir
            val nativeLibPath = appInfo.nativeLibraryDir

            val optimizedDir = context.getDir("dex_opt", Context.MODE_PRIVATE).absolutePath

            DexClassLoader(
                apkPath,
                optimizedDir,
                nativeLibPath,
                context.classLoader
            )
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(TAG, "Could not find package: $packageName", e)
            null
        }
    }
}

@JsonClass(generateAdapter = true)
data class WebUIConfig(
    val modId: ModId = ModId.EMPTY,
    val require: WebUIConfigRequire = WebUIConfigRequire(),
    val permissions: List<String> = emptyList(),
    val historyFallback: Boolean = false,
    val title: String? = null,
    val icon: String? = null,
    val windowResize: Boolean = true,
    val backHandler: Boolean? = true,
    val backInterceptor: Any? = null,
    val refreshInterceptor: String? = null,
    val exitConfirm: Boolean = true,
    val pullToRefresh: Boolean = false,
    val historyFallbackFile: String = "index.html",
    val autoStatusBarsStyle: Boolean = true,
    val dexFiles: List<WebUIConfigDexFile> = emptyList(),
    val killShellWhenBackground: Boolean = true,
    val contentSecurityPolicy: String? = "default-src 'self' data: blob: {domain}; " +
            "script-src 'self' 'unsafe-inline' 'unsafe-eval' {domain}; " +
            "style-src 'self' 'unsafe-inline' {domain}; connect-src *",
    val caching: Boolean = true,
    val cachingMaxAge: Int = 86400,
) {
    val hasRootPathPermission get() = WebUIPermissions.WX_ROOT_PATH in permissions

    val useJavaScriptRefreshInterceptor get() = refreshInterceptor == "javascript"
    val useNativeRefreshInterceptor get() = refreshInterceptor == "native"

    private fun getIconFile() = if (icon != null) SuFile(modId.webrootDir, icon) else null
    private fun getShortcutId() = "shortcut_$modId"

    fun canAddWebUIShortcut(): Boolean {
        val iconFile = getIconFile()
        return title != null && iconFile != null && iconFile.exists() && iconFile.isFile
    }

    fun hasWebUIShortcut(context: Context): Boolean {
        val shortcutId = getShortcutId()
        val shortcutManager = context.getSystemService(ShortcutManager::class.java)
        return shortcutManager.pinnedShortcuts.any { it.id == shortcutId }
    }

    fun createShortcut(
        context: Context,
        cls: Class<out WXActivity>,
    ) {
        val shortcutManager = context.getSystemService(ShortcutManager::class.java)
        val shortcutId = getShortcutId()

        if (!canAddWebUIShortcut()) {
            return
        }

        val iconFile = getIconFile()

        // Paranoia check
        if (iconFile == null) {
            return
        }

        if (shortcutManager.isRequestPinShortcutSupported) {
            if (shortcutManager.pinnedShortcuts.any { it.id == shortcutId }) {
                Toast.makeText(
                    context, context.getString(R.string.shortcut_already_exists), Toast.LENGTH_SHORT
                ).show()
                return
            }

            val shortcutIntent = Intent(context, cls).apply {
                putModId(modId.toString())
            }

            shortcutIntent.action = Intent.ACTION_VIEW

            val bitmap = iconFile.newInputStream().buffered().use { BitmapFactory.decodeStream(it) }

            val shortcut =
                ShortcutInfo.Builder(context, shortcutId).setShortLabel(title!!).setLongLabel(title)
                    .setIcon(Icon.createWithAdaptiveBitmap(bitmap)).setIntent(shortcutIntent)
                    .build()

            shortcutManager.requestPinShortcut(shortcut, null)
        }
    }

    fun toJson(intents: Int = 2): String = jsonAdapter.indent(" ".repeat(intents)).toJson(this)

    fun Map<String, Any?>.toJson(intents: Int = 2): String =
        mapAdapter.indent(" ".repeat(intents)).toJson(this)

    suspend fun <V : Any?> save(builderAction: MutableConfig<V>.() -> Unit) {
        val updates = buildMutableConfig(builderAction)
        if (updates.isEmpty()) return

        withContext(Dispatchers.IO) {
            val (_, overrideFile) = modId.configFiles
            val overrideMap = overrideFile.readConfig().toConfigMap()?.toMutableMap()
            overrideMap?.putAll(updates)
            overrideFile.writeText(mapAdapter.indent("  ").toJson(overrideMap))

            val newConfig = modId.loadConfig()
            _configState.update { it + (modId to newConfig) }

            synchronized(configFlows) {
                val flow = configFlows[modId]
                if (flow != null) {
                    flow.value = newConfig
                }
            }
        }
    }

    companion object {
        const val TAG = "WebUIConfig"

        private val mapType =
            Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
        private val mapAdapter = moshi.adapter<Map<String, Any?>>(mapType)
        private val jsonAdapter = moshi.adapter(WebUIConfig::class.java)

        fun fromJson(json: String): WebUIConfig? = jsonAdapter.fromJson(json)

        private val _configState = MutableStateFlow<Map<ModId, WebUIConfig>>(emptyMap())

        private val configFlows = mutableMapOf<ModId, MutableStateFlow<WebUIConfig>>()

        private val ModId.configFiles: Pair<SuFile?, SuFile>
            get() {
                val webrootConfig = webrootDir.fromPaths("config.json", "config.mmrl.json")
                val moduleConfigConfig = SuFile(moduleConfigDir, "config.webroot.json")

                if (!moduleConfigConfig.exists()) {
                    moduleConfigDir.mkdirs()
                    moduleConfigConfig.writeText("{}")
                }

                return Pair(
                    webrootConfig, moduleConfigConfig
                )
            }

        val ModId.asWebUIConfigFlow: StateFlow<WebUIConfig>
            get() = synchronized(configFlows) {
                configFlows.getOrPut(this) {
                    val initialConfig = loadConfig()
                    _configState.update { it + (this to initialConfig) }
                    MutableStateFlow(initialConfig)
                }
            }

        val ModId.asWebUIConfig: WebUIConfig
            get() = _configState.value[this] ?: loadConfig().also { config ->
                _configState.update { current -> current + (this to config) }
            }

        val LocalModule.webUiConfig: WebUIConfig
            get() = id.asWebUIConfig

        private fun ModId.loadConfig(): WebUIConfig {
            val (baseFile, overrideFile) = configFiles
            val baseJson = baseFile.readConfig()
            val overrideJson = overrideFile.readText()
            val override = overrideJson.toConfigMap() ?: mutableMapOf()
            val mergedMap = baseJson.toConfigMap()?.deepMerge(override)
            return mergedMap?.let { jsonAdapter.fromJson(mapAdapter.toJson(it)) }
                ?.copy(modId = this) ?: WebUIConfig(modId = this)
        }

        private fun SuFile?.readConfig(): String? = try {
            this?.takeIf { it.exists() }?.readText()
        } catch (e: Exception) {
            Log.e(TAG, "Error reading config file ${this?.path}", e)
            null
        }

        private fun String?.toConfigMap(): Map<String, Any?>? {
            return this?.let { json ->
                runCatching { mapAdapter.fromJson(json) }.getOrNull()
            }
        }

        private fun Map<String, Any?>.deepMerge(
            other: Map<String, Any?>,
            listMergeStrategy: ListMergeStrategy = ListMergeStrategy.REPLACE,
        ): Map<String, Any?> {
            val result = this.toMutableMap()
            for ((key, overrideValue) in other) {
                val baseValue = result[key]
                result[key] = when {
                    baseValue is Map<*, *> && overrideValue is Map<*, *> -> {
                        baseValue.asStringMap()
                            ?.deepMerge(
                                overrideValue.asStringMap() ?: emptyMap(),
                                listMergeStrategy
                            )
                    }

                    baseValue is List<*> && overrideValue is List<*> -> {
                        when (listMergeStrategy) {
                            ListMergeStrategy.REPLACE -> overrideValue
                            ListMergeStrategy.APPEND -> baseValue + overrideValue
                            ListMergeStrategy.DEDUPLICATE -> (baseValue + overrideValue).distinct()
                        }
                    }

                    overrideValue != null -> overrideValue
                    else -> baseValue
                }
            }
            return result
        }

        private fun Any?.asStringMap(): Map<String, Any?>? {
            return (this as? Map<*, *>)?.mapNotNull { (key, value) ->
                (key as? String)?.let { it to value }
            }?.toMap()
        }

        enum class ListMergeStrategy {
            REPLACE,
            APPEND,
            DEDUPLICATE
        }

        private class MutableConfigMap<V : Any?>() : LinkedHashMap<String, V>(), MutableConfig<V> {
            override infix fun String.change(that: V): V? = put(this, that)
        }

        private inline fun <V : Any?> buildMutableConfig(builder: MutableConfig<V>.() -> Unit): Map<String, V> {
            val map = MutableConfigMap<V>()
            map.builder()
            return map
        }
    }
}

interface MutableConfig<V> : MutableMap<String, V> {
    infix fun String.change(that: V): V?
}