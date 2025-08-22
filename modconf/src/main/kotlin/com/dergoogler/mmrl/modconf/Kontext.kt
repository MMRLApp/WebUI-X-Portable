package com.dergoogler.mmrl.modconf

import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import com.dergoogler.mmrl.modconf.config.ModConfConfig.Companion.asModconfConfig
import com.dergoogler.mmrl.platform.file.SuFile
import com.dergoogler.mmrl.platform.model.ModId
import com.dergoogler.mmrl.platform.model.ModId.Companion.modconfDependenciesDir
import com.dergoogler.mmrl.platform.model.ModId.Companion.modconfDir
import dalvik.system.InMemoryDexClassLoader
import java.nio.ByteBuffer

class Kontext(
    context: Context,
    val modId: ModId,
) : ContextWrapper(context) {

    var dexLoader: ClassLoader? = null
        private set

    var dexLoadedSuccessfully: Boolean = false
        private set

    init {
        dexLoader = createDexLoader()
        dexLoadedSuccessfully = dexLoader != null
    }

    val config get() = modId.asModconfConfig

    private fun createDexLoader(): ClassLoader? {
        val entryPointPath: String? = config.entryPoint
        val className: String? = config.className

        if (entryPointPath == null || className == null) {
            Log.e(TAG, "Missing entryPoint or className in config")
            return null
        }

        val entryPointFile = SuFile(modId.modconfDir, entryPointPath)
        val dependenciesDir = modId.modconfDependenciesDir

        var parentClassLoader: ClassLoader = classLoader

        if (config.dependencies.isNotEmpty() && dependenciesDir.exists() && dependenciesDir.isDirectory()) {
            dependenciesDir.listFiles()?.sorted()?.forEach { depFile ->
                if (depFile.isFile && depFile.extension == "dex") {
                    try {
                        val depBytes = SuFile(dependenciesDir, depFile).readBytes()
                        parentClassLoader =
                            InMemoryDexClassLoader(ByteBuffer.wrap(depBytes), parentClassLoader)
                        Log.i(TAG, "Loaded dependency: ${depFile.name}")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to load dependency: ${depFile.name}", e)
                    }
                }
            }
        }

        if (!entryPointFile.isFile || entryPointFile.extension != "dex") {
            Log.e(TAG, "Provided entryPoint is not a valid .dex file: ${entryPointFile.path}")
            return null
        }

        return try {
            val dexFileBytes = entryPointFile.readBytes()
            InMemoryDexClassLoader(ByteBuffer.wrap(dexFileBytes), parentClassLoader)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load entryPoint dex: ${entryPointFile.path}", e)
            null
        }
    }

    private companion object {
        const val TAG = "Kontext"
    }
}
