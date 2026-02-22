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
import com.dergoogler.mmrl.platform.file.SuFileInputStream
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
import com.dergoogler.mmrl.webui.util.WXUPublicKey
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import dalvik.system.BaseDexClassLoader
import dalvik.system.DexClassLoader
import dalvik.system.InMemoryDexClassLoader
import kotlinx.coroutines.flow.StateFlow
import org.jf.dexlib2.dexbacked.DexBackedDexFile
import org.jf.dexlib2.iface.instruction.ReferenceInstruction
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.PublicKey
import java.security.Signature
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern


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

        if (!file.isFile || !file.extension.equals("dex", ignoreCase = true)) {
            Log.e(TAG, "Invalid .dex file: ${file.path}")
            return null
        }

        val dexFile = loadSignedDex(file, WXUPublicKey)

        if (!dexFile.official) {
            SuFileInputStream(file).use { stream ->
                if (isBlocked(stream.buffered())) {
                    Log.w(TAG, "Blocked dex loading: ${file.path}")
                    return null
                }
            }
        }

        return InMemoryDexClassLoader(
            ByteBuffer.wrap(dexFile.dexBytes),
            context.classLoader
        )
    }

    @Throws(Exception::class)
    private fun isBlocked(stream: InputStream): Boolean {
        val blockedPackages = listOf(
            "(Lcom/dergoogler/mmrl/platform/)?(.+)?/?KsuNative",
            "Lcom/dergoogler/mmrl/webui/util/WXUPublicKeyKt"
        )

        val dexFile = DexBackedDexFile.fromInputStream(null, stream)

        for (classDef in dexFile.classes) {
            for (method in classDef.methods) {
                if (method.implementation?.instructions == null) return false
                for (instr in method.implementation!!.instructions) {
                    if (instr is ReferenceInstruction) {
                        val ref = instr.reference.toString()
                        for (pkg in blockedPackages) {
                            if (Regex(pkg).containsMatchIn(ref)) {
                                Log.wtf(TAG, "Blocked import detected: $ref")
                                return true
                            }
                        }
                    }
                }
            }
        }

        return false
    }

    private data class VerifiedDex(
        val dexBytes: ByteArray,
        val official: Boolean,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as VerifiedDex

            if (official != other.official) return false
            if (!dexBytes.contentEquals(other.dexBytes)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = official.hashCode()
            result = 31 * result + dexBytes.contentHashCode()
            return result
        }
    }

    private fun loadSignedDex(file: SuFile, publicKey: PublicKey): VerifiedDex {
        val allBytes = file.readBytes()
        val totalSize = allBytes.size

        if (totalSize < 8) { // must fit signature size + some bytes
            Log.e(TAG, "File too small to contain signature: ${file.path}")
            return VerifiedDex(allBytes, false)
        }

        return try {
            val sigSizeBytes = allBytes.copyOfRange(totalSize - 4, totalSize)
            val sigSize = ByteBuffer.wrap(sigSizeBytes)
                .order(ByteOrder.BIG_ENDIAN)
                .int

            if (sigSize <= 0 || sigSize > totalSize - 4) {
                Log.e(TAG, "Invalid signature size=$sigSize for ${file.path}")
                return VerifiedDex(allBytes, false)
            }

            val sigStart = totalSize - 4 - sigSize
            val sigBytes = allBytes.copyOfRange(sigStart, totalSize - 4)
            val dexBytes = allBytes.copyOfRange(0, sigStart)

            val signature = Signature.getInstance("SHA256withRSA")
            signature.initVerify(publicKey)
            signature.update(dexBytes)

            val isOfficial = try {
                signature.verify(sigBytes)
            } catch (e: Exception) {
                Log.e(TAG, "Error verifying signature", e)
                false
            }

            VerifiedDex(dexBytes, isOfficial)
        } catch (e: Exception) {
            Log.e(TAG, "Unable to verify signature for ${file.path}", e)
            VerifiedDex(allBytes, false)
        }
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

operator fun <T, R> ConfigFile<T>.invoke(block: T.() -> R): R = block(getConfig())

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
    @Deprecated("")
    val allowUrls: MutableList<String> = mutableListOf(),
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

    @Deprecated("")
    internal val allowUrlsPatterns =
        allowUrls.mapNotNull { Pattern.compile(it, Pattern.CASE_INSENSITIVE) }

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