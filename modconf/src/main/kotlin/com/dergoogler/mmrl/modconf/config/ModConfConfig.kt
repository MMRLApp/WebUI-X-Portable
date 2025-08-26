@file:Suppress("PropertyName")

package com.dergoogler.mmrl.modconf.config

import com.dergoogler.mmrl.platform.file.SuFile
import com.dergoogler.mmrl.platform.file.config.ConfigFile
import com.dergoogler.mmrl.platform.model.ModId
import com.dergoogler.mmrl.platform.model.ModId.Companion.modconfDir
import com.dergoogler.mmrl.platform.model.ModId.Companion.moduleConfigDir
import com.squareup.moshi.JsonClass
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.ConcurrentHashMap

@JsonClass(generateAdapter = true)
data class ModConfConfig(
    val __module__identifier__: ModId,
    val entryPoints: List<String>? = null,
    val className: String? = null,
): ConfigFile<ModConfConfig>() {
    override fun getModuleId(): ModId = __module__identifier__
    override fun getConfigFile(id: ModId): SuFile = SuFile(id.modconfDir, "modconf.json")
    override fun getOverrideConfigFile(id: ModId): SuFile? =
        SuFile(id.moduleConfigDir, "config.modconf.json")

    override fun getConfigType(): Class<ModConfConfig> = ModConfConfig::class.java
    override fun getDefaultConfigFactory(id: ModId): ModConfConfig = ModConfConfig(id)
}


private val modconfConfigCache = ConcurrentHashMap<ModId, ModConfConfig>()

val ModId.ModConfConfig: ConfigFile<ModConfConfig>
    get() = modconfConfigCache.getOrPut(this) { ModConfConfig(this) }

fun ModId.toModConfConfigState(): StateFlow<ModConfConfig> {
    return ModConfConfig.getConfigStateFlow()
}

fun ModId.toModConfConfig(disableCache: Boolean = false): ModConfConfig {
    return ModConfConfig.getConfig(disableCache)
}