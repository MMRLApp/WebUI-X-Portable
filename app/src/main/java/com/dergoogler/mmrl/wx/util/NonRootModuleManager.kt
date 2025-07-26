package com.dergoogler.mmrl.wx.util

import android.content.Context
import com.dergoogler.mmrl.platform.Platform
import com.dergoogler.mmrl.platform.content.LocalModule
import com.dergoogler.mmrl.platform.content.ModuleCompatibility
import com.dergoogler.mmrl.platform.content.NullableBoolean
import com.dergoogler.mmrl.platform.file.ExtFile
import com.dergoogler.mmrl.platform.manager.BaseModuleManager
import com.dergoogler.mmrl.platform.model.ModId
import com.dergoogler.mmrl.platform.model.ModId.Companion.propFile
import com.dergoogler.mmrl.platform.stub.IModuleOpsCallback
import kotlin.collections.mapNotNull
import kotlin.collections.orEmpty

class NonRootModuleManager(
    private val context: Context,
    private val platform: Platform,
) : BaseModuleManager() {
    override fun getManagerName(): String = "NonRoot"

    override fun getVersion(): String? = null

    override fun getVersionCode(): Int = 0

    override fun getModules(): List<LocalModule> {
        val baseDir = context.getBaseDir(platform)

        val modulesDir = ExtFile(baseDir, ModId.MODULES_DIR)

        if (!modulesDir.exists()) modulesDir.mkdir()

        return modulesDir.listFiles()
            .orEmpty()
            .mapNotNull { dir ->
                val id = ModId(dir.name, baseDir.path)
                val propsFile = id.propFile.toExtFile()
                val props = readProps(propsFile.readText())
                props.toModule(baseDir.path)
            }
    }

    override fun getModuleById(id: ModId): LocalModule? {
        val baseDir = context.getBaseDir(platform).path
        return id.readProps?.toModule(baseDir)
    }

    override fun getModuleCompatibility(): ModuleCompatibility = ModuleCompatibility(
        hasMagicMount = false,
        canRestoreModules = false
    )

    override fun enable(
        p0: ModId?,
        p1: Boolean,
        p2: IModuleOpsCallback?,
    ) = Unit

    override fun disable(
        p0: ModId?,
        p1: Boolean,
        p2: IModuleOpsCallback?,
    ) = Unit

    override fun remove(
        p0: ModId?,
        p1: Boolean,
        p2: IModuleOpsCallback?,
    ) = Unit

    override fun getInstallCommand(p0: String?): String = ""

    override fun getActionCommand(p0: ModId?): String = ""

    override fun getActionEnvironment(): List<String?> = emptyList()

    override fun getSuperUserCount(): Int = -1

    override fun isLkmMode(): NullableBoolean = NullableBoolean(null)

    override fun isSafeMode(): Boolean = false

    override fun setSuEnabled(p0: Boolean): Boolean = false

    override fun isSuEnabled(): Boolean = false

    override fun uidShouldUmount(p0: Int): Boolean = false
}