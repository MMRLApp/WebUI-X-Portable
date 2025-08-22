package com.dergoogler.mmrl.modconf.config

import com.dergoogler.mmrl.modconf.__modconf__adapters__
import com.dergoogler.mmrl.modconf.readText
import com.dergoogler.mmrl.modconf.writeText
import com.dergoogler.mmrl.platform.content.LocalModule
import com.dergoogler.mmrl.platform.file.SuFile
import com.dergoogler.mmrl.platform.model.ModId
import com.dergoogler.mmrl.platform.model.ModId.Companion.modconfDir
import com.dergoogler.mmrl.platform.model.ModId.Companion.moduleConfigDir
import com.squareup.moshi.JsonClass
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import java.util.concurrent.ConcurrentHashMap

@JsonClass(generateAdapter = true)
data class ModConfConfig(
    val entryPoint: String? = null,
    val className: String? = null,
    val resources: String? = null,
    val dependencies: List<String> = emptyList(),
) {
    companion object {
        const val TAG = "ModConfConfig"

        fun fromJson(json: String): ModConfConfig? =
            __modconf__adapters__.ConfigAdapter.fromJson(json)

        private val _configState = MutableStateFlow<Map<ModId, ModConfConfig>>(emptyMap())

        private val modConfigLocks = ConcurrentHashMap<ModId, Mutex>()
        private val configFlows = mutableMapOf<ModId, MutableStateFlow<ModConfConfig>>()

        private val ModId.configFiles: Pair<SuFile?, SuFile>
            get() {
                // Do not write to this file
                val modconfConfig = SuFile(modconfDir, "modconf.json")
                val moduleConfigConfig = SuFile(moduleConfigDir, "config.modconf.json")

                if (!moduleConfigConfig.exists()) {
                    moduleConfigDir.mkdirs()
                    moduleConfigConfig.writeText("{}", Charsets.UTF_8)
                }

                return Pair(
                    modconfConfig, moduleConfigConfig
                )
            }

        val ModId.asModconfConfigFlow: MutableStateFlow<ModConfConfig>
            get() = synchronized(configFlows) {
                configFlows.getOrPut(this) {
                    val initialConfig = loadConfig()
                    _configState.update { it + (this to initialConfig) }
                    MutableStateFlow(initialConfig)
                }
            }

        val ModId.asModconfConfig: ModConfConfig
            get() = _configState.value[this] ?: loadConfig().also { config ->
                _configState.update { current -> current + (this to config) }
            }

        val LocalModule.modconfConfig: ModConfConfig
            get() = id.asModconfConfig

        private fun ModId.loadConfig(): ModConfConfig {
            val (baseFile, overrideFile) = configFiles
            val baseJson = baseFile?.readText(Charsets.UTF_8) ?: "{}"
            val overrideJson = overrideFile.readText(Charsets.UTF_8)
            val override = overrideJson.toJsonMap() ?: mutableMapOf()
            val mergedMap = baseJson.toJsonMap()?.deepMerge(override)
            return mergedMap?.let {
                __modconf__adapters__.ConfigAdapter.fromJson(
                    __modconf__adapters__.MapAdapter.toJson(
                        it
                    )
                )
            }
                ?: ModConfConfig()
        }

        fun String?.toJsonMap(): Map<String, Any?>? {
            return this?.let { json ->
                runCatching { __modconf__adapters__.MapAdapter.fromJson(json) }.getOrNull()
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
            override infix fun String.to(that: V): V? = change(that)
        }

        private inline fun <V : Any?> ModConfConfig.buildMutableConfig(builder: MutableConfig<V>.(ModConfConfig) -> Unit): Map<String, V> {
            val map = MutableConfigMap<V>()
            map.builder(this)
            return map
        }
    }
}

interface MutableConfig<V> : MutableMap<String, V> {
    infix fun String.change(that: V): V?
    infix fun String.to(that: V): V?
}