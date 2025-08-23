@file:Suppress("CanBeParameter", "PropertyName")

package com.dergoogler.mmrl.webui.model

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dergoogler.mmrl.platform.file.SuFile
import com.dergoogler.mmrl.platform.model.ModId
import com.dergoogler.mmrl.webui.moshi
import com.squareup.moshi.Json
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException
import java.lang.reflect.ParameterizedType
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Supplier

enum class ConfigFileMergeStrategy {
    REPLACE,
    APPEND,
    DEDUPLICATE
}

typealias ConfigFileSave<T> = (MutableConfig<Any?>.(T) -> Unit) -> Unit

interface IConfig {
    val moduleId: ModId
}

/**
 * Manages configuration files by layering a base config with a user-specific override,
 * using the SuFile class for potentially privileged file access.
 *
 * This class is thread-safe and now leverages Kotlin's coroutines for reactive state management.
 *
 * The `__module__identifier__` property is reversed for [ModId].
 *
 * @param T The data class type representing the configuration structure.
 */
abstract class ConfigFile<T>(
    @Json(ignore = true) private val configFile: SuFile,
    @Json(ignore = true) private val overrideConfigFile: SuFile,
    @Json(ignore = true) private val configType: Class<T>,
    @Json(ignore = true) private val defaultConfigFactory: Supplier<T>,
    @Json(ignore = true) private val mergeStrategy: ConfigFileMergeStrategy = ConfigFileMergeStrategy.REPLACE,
) : IConfig {
    // A cache to store loaded configurations as reactive StateFlows.
    @Json(ignore = true)
    private val configCache = mutableMapOf<ModId, MutableStateFlow<T>>()

    @Json(ignore = true)
    private val modConfigLocks = ConcurrentHashMap<ModId, Mutex>()

    @Json(ignore = true)
    private val configAdapter: JsonAdapter<T> = moshi.adapter(configType)

    @Json(ignore = true)
    private val mapAdapter: JsonAdapter<Map<String, Any?>> = moshi.adapter(mapType)

    // Using a companion object for static-like members in Kotlin
    companion object {
        private const val TAG = "ConfigFile"

        private val mapType: ParameterizedType =
            Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)

        /**
         * Public utility to deserialize a JSON string into a config object.
         * @param json The JSON string.
         * @param type The class of the target object.
         * @param T The generic type of the object.
         * @return The deserialized object, or null if parsing fails.
         */
        fun <T> fromJson(json: String, type: Class<T>): T? {
            return try {
                Moshi.Builder().build().adapter(type).fromJson(json)
            } catch (_: IOException) {
                // Log the exception if you have a logging framework
                null
            }
        }

        fun MutableMap<String, Any?>.toJson(intents: Int = 2): String {
            return moshi.adapter(Map::class.java).indent(" ".repeat(intents)).toJson(this)
        }
    }

    private class MutableConfigMap<V : Any?> : LinkedHashMap<String, V>(), MutableConfig<V> {
        override infix fun String.change(that: V): V? = put(this, that)
        override infix fun String.to(that: V): V? = change(that)
    }

    private fun <V : Any?> buildMutableConfig(builder: MutableConfig<V>.(T) -> Unit): Map<String, V> {
        val map = MutableConfigMap<V>()
        map.builder(getConfig())
        return map
    }

    /**
     * Serializes the current configuration to a JSON string.
     *
     * @param intents The number of spaces to use for indentation in the JSON output. Defaults to 2.
     * @return The JSON string representation of the configuration.
     */
    fun toJson(intents: Int = 2): String {
        return configAdapter.indent(" ".repeat(intents)).toJson(getConfig())
    }

    /**
     * Exposes the configuration for a specific module as a reactive [StateFlow].
     *
     * @return A [StateFlow] of the configuration instance.
     */
    fun getConfigStateFlow(): StateFlow<T> {
        return synchronized(configCache) {
            configCache.getOrPut(moduleId) {
                val initialConfig = loadConfigInternal()
                MutableStateFlow(initialConfig)
            }.asStateFlow()
        }
    }

    /**
     * Gets the current configuration snapshot for a specific module ID.
     *
     * @return The current configuration instance.
     * @param disableCache If true, bypasses the cache and reloads the configuration.
     */
    fun getConfig(disableCache: Boolean = false): T {
        if (disableCache) {
            return loadConfigInternal(forceNewInstance = true)
        }

        return synchronized(configCache) {
            val flow = configCache.getOrPut(moduleId) {
                val initialConfig = loadConfigInternal()
                MutableStateFlow(initialConfig)
            }
            flow.value
        }
    }

    suspend fun <V : Any?> save(
        builderAction: MutableConfig<V>.(T) -> Unit,
    ) {
        val updates = buildMutableConfig(builderAction)
        if (updates.isEmpty()) return

        val mutex = modConfigLocks.getOrPut(moduleId) { Mutex() }

        mutex.withLock {
            withContext(Dispatchers.IO) {
                val overrideText = overrideConfigFile.readText()
                val overrideMap = mapAdapter.fromJson(overrideText)?.toMutableMap()
                    ?: mutableMapOf()
                overrideMap.putAll(updates)
                overrideConfigFile.writeText(
                    data = mapAdapter.indent("  ").toJson(overrideMap)
                )

                // Load the new configuration
                val newConfig = loadConfigInternal(forceNewInstance = true)

                // Update the StateFlow - this is the key fix
                synchronized(configCache) {
                    val flow = configCache[moduleId]
                    if (flow != null) {
                        flow.value = newConfig
                    } else {
                        // If somehow the flow doesn't exist, create it
                        configCache[moduleId] = MutableStateFlow(newConfig)
                    }
                }
            }
        }
    }

    private fun loadConfigInternal(forceNewInstance: Boolean = false): T {
        return try {
            prepareOverrideFile()
            val baseJson = if (configFile.exists()) configFile.readText() else "{}"
            val overrideJson = overrideConfigFile.readText()

            val baseMap = jsonToMap(baseJson).apply { set("__module__identifier__", moduleId) }
            val overrideMap =
                jsonToMap(overrideJson).apply { set("__module__identifier__", moduleId) }

            val mergedMap = deepMerge(baseMap, overrideMap)
            val jsonMergedMap: String? = mapAdapter.toJson(mergedMap)
            if (jsonMergedMap == null) return defaultConfigFactory.get()

            val parsed = configAdapter.fromJson(jsonMergedMap) ?: defaultConfigFactory.get()

            val value = if (forceNewInstance) {
                // force a new reference
                configAdapter.fromJson(configAdapter.toJson(parsed))
            } else parsed

            if (value == null) {
                Log.e(TAG, "Failed to parse configuration", Exception("Adapter returned null"))
                return defaultConfigFactory.get()
            }

            value
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load configuration", e)
            defaultConfigFactory.get()
        }
    }

    private fun prepareOverrideFile() {
        if (!overrideConfigFile.exists()) {
            val parentDir = overrideConfigFile.parentFile
            parentDir?.let {
                SuFile(it.absolutePath).mkdirs()
            }
            overrideConfigFile.writeText("{}")
        }
    }

    private fun jsonToMap(json: String?): MutableMap<String, Any?> {
        if (json.isNullOrBlank()) {
            return mutableMapOf()
        }
        return try {
            mapAdapter.fromJson(json)?.toMutableMap() ?: mutableMapOf()
        } catch (_: IOException) {
            mutableMapOf()
        }
    }

    private fun deepMerge(
        base: Map<String, Any?>,
        other: Map<String, Any?>,
    ): MutableMap<String, Any?> {
        val result = base.toMutableMap()
        for ((key, overrideValue) in other) {
            val baseValue = result[key]
            result[key] = when {
                baseValue is Map<*, *> && overrideValue is Map<*, *> -> {
                    deepMerge(baseValue.asStringMap(), overrideValue.asStringMap())
                }

                baseValue is List<*> && overrideValue is List<*> -> {
                    when (mergeStrategy) {
                        ConfigFileMergeStrategy.REPLACE -> overrideValue
                        ConfigFileMergeStrategy.APPEND -> baseValue + overrideValue
                        ConfigFileMergeStrategy.DEDUPLICATE -> (baseValue + overrideValue).distinct()
                    }
                }

                overrideValue != null -> overrideValue
                else -> baseValue
            }
        }
        return result
    }


    private fun Any?.asStringMap(): Map<String, Any?> {
        val self = (this as? Map<*, *>)

        if (self == null) {
            return emptyMap()
        }

        val m = self.mapNotNull { (key, value) ->
            (key as? String)?.let { it to value }
        }

        return m.toMap()
    }
}

/**
 * A Composable function that remembers a [ConfigFile] instance and provides its current state
 * along with a function to update it.
 *
 * This function utilizes [rememberCoroutineScope] to launch save operations and
 * [collectAsStateWithLifecycle] to observe changes in the configuration state reactively.
 *
 * @param T The type of the configuration data.
 * @param config The [ConfigFile] instance to remember and manage.
 * @return A [Pair] containing:
 *   - The current configuration state of type [T].
 *   - A [ConfigFileSave] lambda function to update the configuration. This function
 *     takes a builder action that operates on a [MutableConfig] of the configuration type.
 *
 * @see ConfigFile
 * @see ConfigFileSave
 * @see rememberCoroutineScope
 * @see collectAsStateWithLifecycle
 */
@Composable
inline fun <reified T> rememberConfigFile(config: ConfigFile<T>): Pair<T, ConfigFileSave<T>> {
    val scope = rememberCoroutineScope()
    val configFile = remember { config }
    val config by configFile.getConfigStateFlow().collectAsStateWithLifecycle()

    val updater: ConfigFileSave<T> = fun(builderAction) {
        scope.launch {
            configFile.save(builderAction)
        }
    }

    return config to updater
}

interface MutableConfig<V> : MutableMap<String, V> {
    infix fun String.change(that: V): V?
    infix fun String.to(that: V): V?
}