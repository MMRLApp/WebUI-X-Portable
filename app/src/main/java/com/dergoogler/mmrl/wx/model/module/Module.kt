package com.dergoogler.mmrl.wx.model.module

import dev.mmrlx.nio.SuFile
import dev.mmrlx.utilities.obj.asOrDefault
import java.io.Serializable

data class Module(
    private val adbPath: AdbPath,
    private val properties: Map<String, String>,
) : Serializable, Comparable<Module> {
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

    val lastUpdated: Long by lazy {
        path.files.map { SuFile(it) }.forEach {
            if (it.exists()) {
                return@lazy it.lastModified()
            }
        }

        return@lazy 0L
    }


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
}