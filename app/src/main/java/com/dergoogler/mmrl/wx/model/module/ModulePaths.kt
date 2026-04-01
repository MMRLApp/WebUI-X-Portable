package com.dergoogler.mmrl.wx.model.module

import com.dergoogler.mmrl.platform.file.Path
import java.io.Serializable

data class ModulePaths(
    private val baseDir: String,
    private val moduleId: String,
) : Serializable {
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

    val adbDir get() = baseDir
    val configDir get() = Path.resolve(adbDir, HIDDEN_CONFIG_DIR)
    val moduleConfigDir get() = Path.resolve(configDir, moduleId)
    val modulesDir get() = Path.resolve(adbDir, MODULES_DIR)
    val moduleDir get() = Path.resolve(modulesDir, moduleId)
    val webrootDir get() = Path.resolve(moduleDir, WEBROOT_DIR)
    val modconfDir get() = Path.resolve(moduleDir, MODCONF_DIR)
    val modconfDependenciesDir
        get() = Path.resolve(
            modconfDir,
            MODCONF_DEPENDENCIES_DIR
        )
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

    companion object {
        const val WEBROOT_DIR = "webroot"
        const val MODCONF_DIR = "modconf"
        const val MODCONF_DEPENDENCIES_DIR = "dependencies"
        const val MODULES_DIR = "modules"
        const val HIDDEN_CONFIG_DIR = ".config"

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