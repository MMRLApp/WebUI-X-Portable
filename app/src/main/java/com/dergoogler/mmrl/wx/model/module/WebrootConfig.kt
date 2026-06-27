@file:Suppress("PropertyName")

package com.dergoogler.mmrl.wx.model.module

import androidx.compose.runtime.mutableStateMapOf
import com.dergoogler.mmrl.webui.model.DexSourceType
import com.dergoogler.mmrl.webui.model.WebUIConfigDexFile
import dev.mmrlx.nio.SuFile
import dev.mmrlx.nio.readText
import dev.mmrlx.nio.writeText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.serialization.Contextual
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.float
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long

class WebrootConfig(
    private val module: Module,
) {

    @Contextual
    @IgnoredOnParcel
    private val json = Json { ignoreUnknownKeys = true }

    private val sourceConfigFile: SuFile
        get() = SuFile(module.path.webrootConfig)

    private val destConfigFile: SuFile
        get() = SuFile(module.path.configDir, "config.webroot.json")

    /**
     * Compose observable map.
     *
     * Any reads inside composition automatically trigger recomposition
     * when values change.
     */
    @PublishedApi
    internal val _map = mutableStateMapOf<String, JsonElement>()

    init {
        loadSync()
    }

    operator fun get(key: String): JsonElement? = resolvePath(key)

    inline fun <reified T> get(key: String, default: T): T =
        resolvePath(key).getOrDefault(default)

    inline fun <reified T> JsonElement?.getOrDefault(default: T): T {
        if (this == null) return default

        @Suppress("UNCHECKED_CAST")
        return runCatching {
            when (T::class) {

                JsonObject::class -> this as? JsonObject ?: return default
                JsonArray::class -> this as? JsonArray ?: return default
                JsonPrimitive::class -> this as? JsonPrimitive ?: return default

                String::class -> jsonPrimitive.content
                Boolean::class -> jsonPrimitive.boolean
                Int::class -> jsonPrimitive.int
                Long::class -> jsonPrimitive.long
                Float::class -> jsonPrimitive.float
                Double::class -> jsonPrimitive.double
                Short::class -> jsonPrimitive.int.toShort()
                Byte::class -> jsonPrimitive.int.toByte()

                else -> return default
            } as T
        }.getOrDefault(default)
    }

    operator fun set(key: String, value: JsonElement) {
        _map[key] = value
        saveSync()
    }

    inline fun <reified T> set(key: String, value: T) {
        when (T::class) {
            String::class -> set(key, JsonPrimitive(value as String))
            Boolean::class -> set(key, JsonPrimitive(value as Boolean))
            Int::class -> set(key, JsonPrimitive(value as Int))
            Long::class -> set(key, JsonPrimitive(value as Long))
            Float::class -> set(key, JsonPrimitive(value as Float))
            Double::class -> set(key, JsonPrimitive(value as Double))
            Short::class -> set(key, JsonPrimitive(value as Short))
            Byte::class -> set(key, JsonPrimitive(value as Byte))
            else -> throw IllegalArgumentException("Unsupported type: ${T::class}")
        }
    }

    fun remove(key: String): JsonElement? {
        val prev = _map.remove(key)

        if (prev != null) {
            saveSync()
        }

        return prev
    }

    operator fun contains(key: String): Boolean = key in _map

    fun putAll(map: Map<String, JsonElement>) {
        _map.clear()
        _map.putAll(map)
        saveSync()
    }

    fun clear() {
        _map.clear()
        saveSync()
    }

    suspend fun reload(): Unit = withContext(Dispatchers.IO) {
        loadSync()
    }

    suspend fun save(): Unit = withContext(Dispatchers.IO) {
        saveSync()
    }

    @PublishedApi
    internal fun resolvePath(path: String): JsonElement? {
        val segments = path.split(".")

        if (segments.size == 1) {
            return _map[path]
        }

        var current: JsonElement = JsonObject(_map.toMap())

        for (i in segments.indices) {
            if (current !is JsonObject) {
                return null
            }

            val remaining = segments.drop(i).joinToString(".")

            val literal = current[remaining]
            if (literal != null) {
                return literal
            }

            current = current[segments[i]] ?: return null
        }

        return current
    }

    private fun loadSync() {
        runCatching {
            val merged = mutableMapOf<String, JsonElement>()

            val source = sourceConfigFile

            if (source.exists() && source.isFile) {
                json.parseToJsonElement(source.readText())
                    .let { it as? JsonObject }
                    ?.forEach { (key, value) ->
                        merged[key] = value
                    }
            }

            val dest = destConfigFile

            if (dest.exists() && dest.isFile) {
                json.parseToJsonElement(dest.readText())
                    .let { it as? JsonObject }
                    ?.forEach { (key, value) ->
                        merged[key] = value
                    }
            }

            _map.clear()
            _map.putAll(merged)

        }.onFailure {
            it.printStackTrace()
        }
    }

    private fun saveSync() {
        runCatching {
            val file = destConfigFile

            val parent = file.parentFile

            if (parent != null && !parent.exists()) {
                parent.mkdirs()
            }

            val jsonObject = buildJsonObject {
                _map.forEach { (k, v) ->
                    put(k, v)
                }
            }

            file.writeText(jsonObject.toString())
        }.onFailure {
            it.printStackTrace()
        }
    }
}

val WebrootConfig.historyFallback
    get() = get("historyFallback", false)

val WebrootConfig.historyFallbackFile
    get() = get("historyFallbackFile", "index.html")

const val DEFAULT_CSP = "default-src 'self' data: blob: {domain}; " +
        "script-src 'self' 'unsafe-inline' 'unsafe-eval' {domain}; " +
        "img-src 'self' ksu://icon; " +
        "style-src 'self' 'unsafe-inline' {domain}; " +
        "connect-src *; "

val WebrootConfig.contentSecurityPolicy
    get() = get(
        "contentSecurityPolicy",
        DEFAULT_CSP
    )

val WebrootConfig.autoStatusBarsStyle
    get() = get("autoStatusBarsStyle", true)

val WebrootConfig.autoAddInsets
    get() = get("autoAddInsets", true)

val WebrootConfig.windowResize
    get() = get("windowResize", true)

val WebrootConfig.caching
    get() = get("caching", true)

val WebrootConfig.exitConfirm
    get() = get("exitConfirm", true)

val WebrootConfig.pullToRefresh
    get() = get("pullToRefresh", false)

val WebrootConfig.cachingMaxAge
    get() = get("cachingMaxAge", 86400)

val WebrootConfig.killShellWhenBackground
    get() = get("killShellWhenBackground", true)

val WebrootConfig.title
    get() = get<String?>("title", null)

val WebrootConfig.icon
    get() = get<String?>("icon", null)

val WebrootConfig.refreshInterceptor
    get() = get<String?>("refreshInterceptor", "native")

val WebrootConfig.backInterceptor
    get() = get<String>("backInterceptor", "native")

val WebrootConfig.backHandler
    get() = get<Boolean?>("backHandler", true)

val WebrootConfig.permissions
    get() = get("permissions", JsonArray(emptyList()))

@Deprecated("Kept for backwards compatibility.")
val WebrootConfig.dexFiles: List<WebUIConfigDexFile>
    get() {
        val entries = get("dexFiles", JsonArray(emptyList()))

        return entries.map { e ->
            val entry = e as JsonObject

            val type: DexSourceType =
                when (entry["type"].getOrDefault<String?>(null)) {
                    "apk" -> DexSourceType.APK
                    "dex" -> DexSourceType.DEX
                    else -> DexSourceType.DEX
                }

            val path: String? =
                entry["path"].getOrDefault(null)

            val className: String? =
                entry["className"].getOrDefault(null)

            val cache: Boolean =
                entry["cache"].getOrDefault(true)

            WebUIConfigDexFile(
                type = type,
                path = path,
                className = className,
                cache = cache
            )
        }
    }