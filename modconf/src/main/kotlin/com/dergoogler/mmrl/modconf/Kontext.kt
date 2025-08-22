package com.dergoogler.mmrl.modconf

import android.content.Context
import android.content.ContextWrapper
import android.content.res.AssetManager
import android.content.res.Resources
import android.util.Log
import com.dergoogler.mmrl.modconf.config.ModConfConfig.Companion.asModconfConfig
import com.dergoogler.mmrl.platform.file.ExtFile
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

    private var dexLoader: ClassLoader? = null
    var dexLoadedSuccessfully: Boolean = false
        private set

    init {
        dexLoader = createDexLoader()
        dexLoadedSuccessfully = dexLoader != null
    }

    val config get() = modId.asModconfConfig

    val modconf: ModConfClass.Instance? by lazy {
        val loader = dexLoader ?: return@lazy null
        try {
            val rawClass = loader.loadClass(config.className)
            if (!ModConfModule::class.java.isAssignableFrom(rawClass)) {
                Log.e(TAG, "Loaded class ${config.className} does not extend ModConfModule")
                null
            } else {
                @Suppress("UNCHECKED_CAST")
                val clazz = rawClass as Class<out ModConfModule>
                ModConfClass(clazz).createNew(this).also {
                    Log.i(TAG, "Successfully loaded ModConfModule: ${clazz.name}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load class ${config.className}", e)
            null
        }
    }

    override fun getResources(): Resources? {
        return try {
            val resFilePath = config.dependencies.find { it.endsWith("resources.arsc") }
                ?: return null
            val resFile = SuFile(modId.modconfDependenciesDir, resFilePath)
            if (!resFile.isFile) return null

            // Copy to a temporary file in app cache to ensure AssetManager can access it
            val tmpFile = ExtFile(cacheDir, "temp_resources.arsc").apply {
                outputStream().use { out ->
                    resFile.newInputStream().use { input -> input.copyTo(out) }
                }
            }

            // Create AssetManager and add temp file path
            val assetManager =
                AssetManager::class.java.getDeclaredConstructor().newInstance().apply {
                    AssetManager::class.java.getMethod("addAssetPath", String::class.java)
                        .invoke(this, tmpFile.absolutePath)
                }

            // Create Resources using current app resources for metrics/config
            Resources(assetManager, resources?.displayMetrics, resources?.configuration).also {
                Log.i(TAG, "Loaded resources.arsc from: ${resFile.absolutePath} (via temp file)")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load resources.arsc", e)
            null
        }
    }

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
