package com.dergoogler.mmrl.wx.model.module

import com.dergoogler.mmrl.wx.util.PathVarArgFunction
import dev.mmrlx.nio.Path
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import org.json.JSONObject
import org.luaj.LuaTable

@Parcelize
@Serializable
data class AdbPath(
    val baseDir: String,
) {
    val configDir get() = Path.resolve(baseDir, HIDDEN_CONFIG_DIR)
    val localDir get() = Path.resolve(baseDir, HIDDEN_LOCAL_DIR)
    val modulesDir get() = Path.resolve(baseDir, MODULES_DIR)

    fun toJSONObject() = JSONObject().apply {
        put("baseDir", baseDir)
        put("configDir", configDir)
        put("localDir", localDir)
        put("modulesDir", modulesDir)
    }

    companion object {
        const val MODULES_DIR = "modules"
        const val HIDDEN_CONFIG_DIR = ".config"
        const val HIDDEN_LOCAL_DIR = ".local"

        val Empty = AdbPath("/dev/null")
    }

    fun toLuaTable(): LuaTable {
        val table = LuaTable()

        table.set("baseDir", baseDir)
        table.set("configDir", configDir)
        table.set("localDir", localDir)
        table.set("modulesDir", modulesDir)
        table.set("resolve", PathVarArgFunction(baseDir))

        return table
    }
}

@Parcelize
@Serializable
data class ModulePath(
    private val adbPath: AdbPath,
    private val moduleId: String,
) {
    val serviceFiles
        get() =
            listOf(
                actionFile,
                serviceFile,
                postFsDataFile,
                postMountFile,
                webrootDir,
                bootCompletedFile,
                sepolicyFile,
            )

    val files
        get() =
            listOf(
                *serviceFiles.toTypedArray(),
                uninstallFile,
                systemPropFile,
                systemDir,
                propFile,
                disableFile,
                removeFile,
                updateFile,
            )

    val configDir get() = Path.resolve(adbPath.configDir, moduleId)
    val moduleDir get() = Path.resolve(adbPath.modulesDir, moduleId)
    val webrootDir get() = Path.resolve(moduleDir, WEBROOT_DIR)
    val webrootConfig get() = Path.resolve(moduleDir, WEBROOT_DIR, "config.json")
    val webrootLuaIndex get() = Path.resolve(moduleDir, WEBROOT_DIR, "index.lua")
    val propFile get() = Path.resolve(moduleDir, PROP_FILE)
    val actionFile get() = Path.resolve(moduleDir, ACTION_FILE)
    val serviceFile get() = Path.resolve(moduleDir, SERVICE_FILE)
    val postFsDataFile get() = Path.resolve(moduleDir, POST_FS_DATA_FILE)
    val postMountFile get() = Path.resolve(moduleDir, POST_MOUNT_FILE)
    val systemPropFile get() = Path.resolve(moduleDir, SYSTEM_PROP_FILE)
    val bootCompletedFile get() = Path.resolve(moduleDir, BOOT_COMPLETED_FILE)
    val sepolicyFile get() = Path.resolve(moduleDir, SE_POLICY_FILE)
    val uninstallFile get() = Path.resolve(moduleDir, UNINSTALL_FILE)
    val systemDir get() = Path.resolve(moduleDir, SYSTEM_DIR)
    val disableFile get() = Path.resolve(moduleDir, DISABLE_FILE)
    val removeFile get() = Path.resolve(moduleDir, REMOVE_FILE)
    val updateFile get() = Path.resolve(moduleDir, UPDATE_FILE)

    fun toJSONObject() = JSONObject().apply {
        put("moduleId", moduleId)
        put("configDir", configDir)
        put("moduleDir", moduleDir)
        put("webrootDir", webrootDir)
        put("webrootConfig", webrootConfig)
        put("webrootLuaIndex", webrootLuaIndex)
        put("propFile", propFile)
        put("actionFile", actionFile)
        put("serviceFile", serviceFile)
        put("postFsDataFile", postFsDataFile)
        put("postMountFile", postMountFile)
        put("systemPropFile", systemPropFile)
        put("bootCompletedFile", bootCompletedFile)
        put("sepolicyFile", sepolicyFile)
        put("uninstallFile", uninstallFile)
        put("systemDir", systemDir)
        put("disableFile", disableFile)
        put("removeFile", removeFile)
        put("updateFile", updateFile)
    }

    fun toLuaTable(): LuaTable {
        val table = LuaTable()

        table.set("moduleId", moduleId)
        table.set("configDir", configDir)
        table.set("moduleDir", moduleDir)
        table.set("webrootDir", webrootDir)
        table.set("webrootConfig", webrootConfig)
        table.set("webrootLuaIndex", webrootLuaIndex)
        table.set("propFile", propFile)
        table.set("actionFile", actionFile)
        table.set("serviceFile", serviceFile)
        table.set("postFsDataFile", postFsDataFile)
        table.set("postMountFile", postMountFile)
        table.set("systemPropFile", systemPropFile)
        table.set("bootCompletedFile", bootCompletedFile)
        table.set("sepolicyFile", sepolicyFile)
        table.set("uninstallFile", uninstallFile)
        table.set("systemDir", systemDir)
        table.set("disableFile", disableFile)
        table.set("removeFile", removeFile)
        table.set("updateFile", updateFile)
        table.set("webroot", PathVarArgFunction(webrootDir))
        table.set("mod", PathVarArgFunction(moduleDir))

        return table
    }

    companion object {
        const val WEBROOT_DIR = "webroot"

        const val PROP_FILE = "module.prop"

        const val ACTION_FILE = "action.sh"
        const val BOOT_COMPLETED_FILE = "boot-completed.sh"
        const val SERVICE_FILE = "service.sh"
        const val POST_FS_DATA_FILE = "post-fs-data.sh"
        const val POST_MOUNT_FILE = "post-mount.sh"
        const val SYSTEM_PROP_FILE = "system.prop"
        const val SE_POLICY_FILE = "sepolicy.rule"
        const val UNINSTALL_FILE = "uninstall.sh"
        const val SYSTEM_DIR = "system"

        // State files
        const val DISABLE_FILE = "disable"
        const val REMOVE_FILE = "remove"
        const val UPDATE_FILE = "update"
    }
}