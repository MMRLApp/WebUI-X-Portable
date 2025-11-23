@file:Suppress("PropertyName")

package com.dergoogler.mmrl.modconf.config

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.BitmapFactory
import android.graphics.drawable.Icon
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.dergoogler.mmrl.modconf.R
import com.dergoogler.mmrl.platform.file.SuFile
import com.dergoogler.mmrl.platform.file.config.ConfigFile
import com.dergoogler.mmrl.platform.model.ModId
import com.dergoogler.mmrl.platform.model.ModId.Companion.modconfDir
import com.dergoogler.mmrl.platform.model.ModId.Companion.moduleConfigDir
import com.dergoogler.mmrl.platform.model.ModId.Companion.putModId
import com.dergoogler.mmrl.platform.model.ModId.Companion.webrootDir
import com.squareup.moshi.JsonClass
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.ConcurrentHashMap

@JsonClass(generateAdapter = true)
data class ModConfConfig(
    val __module__identifier__: ModId,
    val entryPoints: List<String>? = null,
    val className: String? = null,
    val title: String? = null,
    val icon: String? = null
) : ConfigFile<ModConfConfig>() {
    override fun getModuleId(): ModId = __module__identifier__
    override fun getConfigFile(id: ModId): SuFile = SuFile(id.modconfDir, "modconf.json")
    override fun getOverrideConfigFile(id: ModId): SuFile? =
        SuFile(id.moduleConfigDir, "config.modconf.json")

    override fun getConfigType(): Class<ModConfConfig> = ModConfConfig::class.java
    override fun getDefaultConfigFactory(id: ModId): ModConfConfig = ModConfConfig(id)


    private fun getIconFile() =
        if (icon != null) SuFile(__module__identifier__.modconfDir, icon) else null

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
        cls: Class<out ComponentActivity>,
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


private val modconfConfigCache = ConcurrentHashMap<ModId, ModConfConfig>()

val ModId.ModConfConfig: ConfigFile<ModConfConfig>
    get() = modconfConfigCache.getOrPut(this) { ModConfConfig(this) }

fun ModId.toModConfConfigState(): StateFlow<ModConfConfig> {
    return ModConfConfig.getConfigStateFlow()
}

fun ModId.toModConfConfig(disableCache: Boolean = false): ModConfConfig {
    return ModConfConfig.getConfig(disableCache)
}