@file:Suppress("RedundantNullableReturnType")

package com.dergoogler.mmrl.wx.model.module

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import com.dergoogler.mmrl.wx.datastore.providable.LocalUserPreferences
import com.dergoogler.mmrl.wx.model.module.ModulePath.Companion.PROP_FILE
import com.dergoogler.mmrl.wx.util.set
import dev.mmrlx.nio.SuFile
import dev.mmrlx.nio.inputStream
import dev.mmrlx.utilities.obj.asOrDefault
import kotlinx.parcelize.IgnoredOnParcel
import org.apache.commons.compress.archivers.zip.ZipFile
import org.luaj.LuaTable
import java.io.File
import java.io.InputStream

data class Module(
    val adbPath: AdbPath,
    private val properties: Map<String, Any>,
) : Comparable<Module> {
    val id: String = properties.get("", "id")
    val path: ModulePath = ModulePath(adbPath, id)
    val name: String = properties.get(NA, "name")
    val version: String = properties.get(NA, "version")
    val versionCode: Int = properties.get(-1, "versionCode")
    val author: String = properties.get(NA, "author")
    val description: String = properties.get(NA, "description")

    private val metaModuleBoolean = properties.get(false, "metamodule")
    private val metaModuleInt = properties.get(0, "metamodule")
    val metaModule: Boolean = metaModuleBoolean || metaModuleInt != 0

    val banner: SuFile? = properties.get<String?>(null, "banner", "cover").relativeModuleDir
    val icon: SuFile? = properties.get<String?>(null, "webuiIcon", "icon").relativeModuleDir

    val webrootConfig: WebrootConfig = WebrootConfig(this)

    @IgnoredOnParcel
    val hasWebUI: Boolean by lazy {
        val webroot = SuFile(path.webrootDir)
        val index = webroot.resolve("index.html")
        webroot.exists() && index.isFile
    }
//
//    val state: ModuleState by lazy {
//        SuFile(path.removeFile).apply {
//            if (exists()) return@lazy ModuleState.REMOVE
//        }
//
//        SuFile(path.disableFile).apply {
//            if (exists()) return@lazy ModuleState.DISABLE
//        }
//
//        SuFile(path.updateFile).apply {
//            if (exists()) return@lazy ModuleState.UPDATE
//        }
//
//        return@lazy ModuleState.ENABLE
//    }


    @IgnoredOnParcel
    val state: com.dergoogler.mmrl.platform.content.State by lazy {
        SuFile(path.removeFile).apply {
            if (exists()) return@lazy com.dergoogler.mmrl.platform.content.State.REMOVE
        }

        SuFile(path.disableFile).apply {
            if (exists()) return@lazy com.dergoogler.mmrl.platform.content.State.DISABLE
        }

        SuFile(path.updateFile).apply {
            if (exists()) return@lazy com.dergoogler.mmrl.platform.content.State.UPDATE
        }

        return@lazy com.dergoogler.mmrl.platform.content.State.ENABLE
    }

    @IgnoredOnParcel
    val lastUpdated: Long by lazy {
        path.files.map { SuFile(it) }.forEach {
            if (it.exists()) {
                return@lazy it.lastModified()
            }
        }

        return@lazy 0L
    }


    @IgnoredOnParcel
    val size: Long by lazy {
        val directory = SuFile(adbPath.modulesDir, id)
        calculateSizeFast(directory)
    }

    private fun calculateSizeFast(file: SuFile): Long {
        // skip symbolic links entirely
        if (file.isSymlink()) return 0L
        if (file.isFile) return file.length()

        var totalSize = 0L
        val children = file.listFiles()

        for (child in children) {
            // skip symlinks before checking if it's a file or directory
            if (child.isSymlink()) continue

            totalSize += if (child.isFile) {
                child.length()
            } else {
                calculateSizeFast(child)
            }
        }
        return totalSize
    }

    override fun compareTo(other: Module): Int = id.compareTo(other.id)

    private val String?.relativeModuleDir: SuFile?
        get() = this?.let { SuFile(path.moduleDir, it) }

    private inline fun <reified T> Map<String, Any>.get(
        defaultValue: T,
        vararg aliases: String,
    ): T {
        if (aliases.size == 1) return this[aliases.first()].asOrDefault(defaultValue)
        for (alias in aliases) {
            val value = this[alias]
            if (value != null) return value.asOrDefault(defaultValue)
        }

        return defaultValue
    }

    fun toLuaTable(): LuaTable {
        val table = LuaTable()

        table.set("adbPath", adbPath.toLuaTable())
        table.set("id", id)
        table.set("name", name)
        table.set("version", version)
        table.set("versionCode", versionCode)
        table.set("author", author)
        table.set("description", description)
        table.set("metamodule", metaModule)
        table.set("hasWebUI", hasWebUI)
        table.set("path", path.toLuaTable())
        // table.set("banner", banner)
        // table.set("icon", icon)
        // table.set("webrootConfig", webrootConfig.toLuaTable())

        return table
    }

    companion object {
        private const val NA = "N/A"
        internal fun readProps(input: InputStream): Map<String, Any> {
            val result = linkedMapOf<String, Any>()

            input.bufferedReader().useLines { lines ->
                lines.forEach { raw ->
                    val line = raw.trim()

                    if (line.isEmpty()) return@forEach
                    if (line.startsWith("#") || line.startsWith("!")) return@forEach

                    var separatorIndex = -1
                    var escaped = false

                    for (i in line.indices) {
                        val c = line[i]

                        if (escaped) {
                            escaped = false
                            continue
                        }

                        if (c == '\\') {
                            escaped = true
                            continue
                        }

                        if (c == '=' || c == ':') {
                            separatorIndex = i
                            break
                        }
                    }

                    val key: String
                    val rawValue: String

                    if (separatorIndex >= 0) {
                        key = line.substring(0, separatorIndex)
                            .replace("\\=", "=")
                            .replace("\\:", ":")
                            .trim()

                        rawValue = line.substring(separatorIndex + 1).trim()
                    } else {
                        key = line
                        rawValue = ""
                    }

                    result[key] = parseValue(rawValue)
                }
            }

            return result
        }

        private fun parseValue(value: String): Any {
            val v = value.trim()

            return when {
                v.equals("true", ignoreCase = true) -> true
                v.equals("false", ignoreCase = true) -> false

                v.toIntOrNull() != null -> v.toInt()

                v.toLongOrNull() != null -> v.toLong()

                v.toDoubleOrNull() != null -> v.toDouble()

                else -> v
            }
        }

        val Empty = Module(AdbPath.Empty, emptyMap())

        @Composable
        fun rememberBasePath(): State<ModuleUIState> {
            val prefs = LocalUserPreferences.current
            val context = LocalContext.current

            return produceState<ModuleUIState>(
                initialValue = ModuleUIState.Loading,
                prefs.workingMode
            ) {
                val initialized = SuFile.AutoInit(context)

                if (!initialized) {
                    value = ModuleUIState.Error.SuInitFailed()
                    return@produceState
                }

                val basePath: String? = prefs.getAdbPath(context)

                if (basePath == null) {
                    value = ModuleUIState.Error.MissingAdbPath()
                    return@produceState
                }

                val baseFileDir = SuFile.async(basePath)

                if (!baseFileDir.exists()) {
                    value = ModuleUIState.Error.ModuleNotFound()
                    return@produceState
                }

                value = ModuleUIState.ReadyBasePath(baseFileDir)
            }
        }

        @Composable
        fun rememberCreate(id: String): State<ModuleUIState> {
            val prefs = LocalUserPreferences.current
            val context = LocalContext.current

            return produceState<ModuleUIState>(
                initialValue = ModuleUIState.Loading,
                id,
                prefs.workingMode
            ) {
                val initialized = SuFile.AutoInit(context)

                if (!initialized) {
                    value = ModuleUIState.Error.SuInitFailed()
                    return@produceState
                }

                val basePath: String? = prefs.getAdbPath(context)

                if (basePath == null) {
                    value = ModuleUIState.Error.MissingAdbPath()
                    return@produceState
                }

                val adbPath = AdbPath(basePath)

                val moduleDir = SuFile.async(adbPath.modulesDir, id)

                if (!moduleDir.exists()) {
                    value = ModuleUIState.Error.ModuleNotFound()
                    return@produceState
                }

                val propsFile = SuFile.async(moduleDir, "module.prop")

                if (!propsFile.exists()) {
                    value = ModuleUIState.Error.InvalidModule()
                    return@produceState
                }

                val props = propsFile.inputStream()
                    .use { readProps(it) }

                value = ModuleUIState.Ready(
                    Module(adbPath, props)
                )
            }
        }

        fun fromZip(adbPath: AdbPath, file: File): Module? {
            val zipFile = ZipFile.Builder().setFile(file).get()
            val entry = zipFile.getEntry(PROP_FILE) ?: return null
            return zipFile.getInputStream(entry).use {
                Module(adbPath, readProps(it))
            }
        }
    }
}