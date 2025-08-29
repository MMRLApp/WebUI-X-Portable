@file:Suppress("PropertyName")

package com.dergoogler.mmrl.webui.model

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.BitmapFactory
import android.graphics.drawable.Icon
import android.util.Log
import android.widget.Toast
import com.dergoogler.mmrl.ext.toBooleanOrNull
import com.dergoogler.mmrl.ext.toIntOrNull
import com.dergoogler.mmrl.ext.toStringOrNull
import com.dergoogler.mmrl.platform.PlatformManager
import com.dergoogler.mmrl.platform.file.SuFile
import com.dergoogler.mmrl.platform.file.config.ConfigFile
import com.dergoogler.mmrl.platform.file.config.JSONArray
import com.dergoogler.mmrl.platform.file.config.JSONCollection
import com.dergoogler.mmrl.platform.file.config.JSONString
import com.dergoogler.mmrl.platform.file.config.toTypedList
import com.dergoogler.mmrl.platform.hiddenApi.HiddenPackageManager
import com.dergoogler.mmrl.platform.hiddenApi.HiddenUserManager
import com.dergoogler.mmrl.platform.model.ModId
import com.dergoogler.mmrl.platform.model.ModId.Companion.moduleConfigDir
import com.dergoogler.mmrl.platform.model.ModId.Companion.putModId
import com.dergoogler.mmrl.platform.model.ModId.Companion.webrootDir
import com.dergoogler.mmrl.webui.R
import com.dergoogler.mmrl.webui.__webui__adapters__
import com.dergoogler.mmrl.webui.activity.WXActivity
import com.dergoogler.mmrl.webui.interfaces.WXInterface
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import dalvik.system.BaseDexClassLoader
import dalvik.system.DexClassLoader
import dalvik.system.InMemoryDexClassLoader
import kotlinx.coroutines.flow.StateFlow
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
    val packageName: JSONCollection,
    val supportText: String? = null,
    val supportLink: String? = null,
) {
    val packageNames: List<String>?
        get() = when (packageName) {
            is JSONString -> listOf(packageName.string)
            is JSONArray -> packageName.toTypedList<String>()
            else -> null
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
    val copySharedObjects: Boolean = true,
    val registerSharedObjects: Boolean = true,
    val sharedObjects: List<String> = emptyList(),
) : WebUIConfigBaseLoader() {
    private companion object {
        const val TAG = "WebUIConfigDexFile"
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

            val instance = JavaScriptInterface(clazz, dexConfig = this)

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
}

open class WebUIConfigBaseLoader() {
    /**
     * Creates a ClassLoader for a standalone .dex file.
     */
    fun createDexLoader(
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
    fun createApkLoader(context: Context, packageName: String): BaseDexClassLoader? {
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

    private companion object {
        const val TAG = "WebUIConfigBaseLoader"
    }
}

@JsonClass(generateAdapter = true)
data class WebUIConfigAdditionalConfig(
    val key: String,
    val type: WebUIConfigAdditionalConfigType,
    val value: JSONCollection,
    val label: String? = null,
    val desc: String? = null,
    val values: List<String>? = null,
) {
    fun Map<String, Any?>.toJson(intents: Int = 2): String =
        __webui__adapters__.MapAdapter.indent(" ".repeat(intents)).toJson(this)

    fun toJson(intents: Int = 2): String =
        __webui__adapters__.AdditionalConfigAdapter.indent(" ".repeat(intents)).toJson(this)

    companion object {
        fun fromJson(json: String): WebUIConfigAdditionalConfig? =
            __webui__adapters__.AdditionalConfigAdapter.fromJson(json)

        /**
         * Converts a list of [WebUIConfigAdditionalConfig] to a map of key-value pairs.
         *
         * Each item in the list is transformed into an entry in the map, where the key is
         * `opt.key` and the value is `opt.value` (or `opt.defaultValue` if `opt.value` is null).
         * The value is then converted to the appropriate type based on `opt.type`.
         *
         * @return A map where keys are strings and values are of type `Any?`.
         *         Returns an empty map if the input list is null.
         */
        fun List<WebUIConfigAdditionalConfig>?.toValueMap(): Map<String, Any?> {
            if (this == null) return emptyMap()
            return this.associate { opt ->
                val gg: Any? = when (opt.type) {
                    WebUIConfigAdditionalConfigType.NUMBER -> opt.value.toIntOrNull()
                    WebUIConfigAdditionalConfigType.SWITCH -> opt.value.toBooleanOrNull()
                    WebUIConfigAdditionalConfigType.EDITTEXT -> opt.value.toStringOrNull()
                }

                opt.key to gg
            }
        }
    }
}

@JsonClass(generateAdapter = false)
enum class WebUIConfigAdditionalConfigType {
    EDITTEXT,
    NUMBER,
    SWITCH,
}

@JsonClass(generateAdapter = true)
data class WebUIConfig(
    val __module__identifier__: ModId,
    val require: WebUIConfigRequire = WebUIConfigRequire(),
    val permissions: MutableList<String> = mutableListOf(),
    val historyFallback: Boolean = false,
    val title: String? = null,
    val icon: String? = null,
    val windowResize: Boolean = true,
    val backHandler: Boolean? = true,
    val backInterceptor: Any? = null,
    val refreshInterceptor: String? = null,
    val exitConfirm: Boolean = true,
    val pullToRefresh: Boolean = false,
    val pullToRefreshHelper: Boolean = true,
    val historyFallbackFile: String = "index.html",
    val autoStatusBarsStyle: Boolean = true,
    val dexFiles: MutableList<WebUIConfigDexFile> = mutableListOf(),
    val killShellWhenBackground: Boolean = true,
    val contentSecurityPolicy: String = "default-src 'self' data: blob: {domain}; " +
            "script-src 'self' 'unsafe-inline' 'unsafe-eval' {domain}; " +
            "style-src 'self' 'unsafe-inline' {domain}; connect-src *",
    val caching: Boolean = true,
    val cachingMaxAge: Int = 86400,
    val extra: MutableMap<String, Any?> = mutableMapOf(),
    val additionalConfig: MutableList<WebUIConfigAdditionalConfig> = mutableListOf(),
) : ConfigFile<WebUIConfig>() {
    override fun getModuleId(): ModId = __module__identifier__
    override fun getConfigFile(id: ModId): SuFile = SuFile(id.webrootDir, "config.json")
    override fun getOverrideConfigFile(id: ModId): SuFile? =
        SuFile(id.moduleConfigDir, "config.webroot.json")

    override fun getConfigType(): Class<WebUIConfig> = WebUIConfig::class.java
    override fun getDefaultConfigFactory(id: ModId): WebUIConfig = WebUIConfig(id)

    val hasRootPathPermission get() = WebUIPermissions.WX_ROOT_PATH in permissions

    val useJavaScriptRefreshInterceptor get() = refreshInterceptor == "javascript"
    val useNativeRefreshInterceptor get() = refreshInterceptor == "native"

    private fun getIconFile() =
        if (icon != null) SuFile(__module__identifier__.webrootDir, icon) else null

    private fun getShortcutId() = "shortcut_$__module__identifier__"

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
                putModId(__module__identifier__.toString())
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
}

private val webUIConfigCache = ConcurrentHashMap<ModId, WebUIConfig>()

val ModId.WebUIConfig: ConfigFile<WebUIConfig>
    get() = webUIConfigCache.getOrPut(this) { WebUIConfig(this) }

fun ModId.toWebUIConfigState(): StateFlow<WebUIConfig> {
    return WebUIConfig.getConfigStateFlow()
}

fun ModId.toWebUIConfig(disableCache: Boolean = false): WebUIConfig {
    return WebUIConfig.getConfig(disableCache)
}