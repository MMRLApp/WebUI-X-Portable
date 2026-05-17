package com.dergoogler.mmrl.wx.model.module

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import com.dergoogler.mmrl.wx.datastore.model.WorkingMode
import com.dergoogler.mmrl.wx.datastore.providable.LocalUserPreferences
import dev.mmrlx.nio.SuFile
import dev.mmrlx.nio.inputStream
import dev.mmrlx.utilities.obj.asOrDefault
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import java.io.InputStream

@Parcelize
@Serializable
data class Module(
    val adbPath: AdbPath,
    private val properties: Map<String, String>,
) : Comparable<Module> {
    val id: String = properties["id"].asOrDefault<String>("")
    val path: ModulePath = ModulePath(adbPath, id)
    val name: String = properties["name"].asOrDefault<String>("")
    val version: String = properties["version"].asOrDefault<String>("")
    val versionCode: Int = properties["VersionCode"].asOrDefault<Int>(-1)
    val author: String = properties["author"].asOrDefault<String>("")
    val description: String = properties["description"].asOrDefault<String>("")
    val metaModule: Boolean = properties["metamodule"].asOrDefault<Boolean>(false)
    val banner: String? = properties["banner"].asOrDefault<String?>(null)
    val iconPath: String? = properties["iconPath"].asOrDefault<String?>(null)

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

    companion object {
        internal fun readProps(input: InputStream): Map<String, String> =
            input.bufferedReader().useLines { lines ->
                lines.mapNotNull { line ->
                    val idx = line.indexOf('=')
                    if (idx > 0) line.substring(0, idx).trim() to line.substring(idx + 1).trim()
                    else null
                }.toMap()
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

                val isNonRoot = prefs.workingMode == WorkingMode.MODE_NON_ROOT

                val basePath =
                    if (isNonRoot) context.filesDir.path else prefs.adbPath

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

    }
}