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

    private var dexLoader: ClassLoader? = null

    /**
     * Indicates whether the dex file was loaded successfully.
     * This is set to true if [dexLoader] is not null after initialization.
     */
    var dexLoadedSuccessfully: Boolean = false
        private set

    init {
        dexLoader = createDexLoader()
        dexLoadedSuccessfully = dexLoader != null
    }

    /**
     * Retrieves the ModConfConfig associated with this Kontext's modId.
     * This config contains metadata about the ModConf module, such as its entry point and dependencies.
     */
    val config get() = modId.asModconfConfig

    /**
     * Lazily initializes and returns an instance of the `ModConfModule` defined in the module's configuration.
     *
     * This property attempts to:
     * 1. Access the `dexLoader`. If it's null (meaning DEX loading failed or wasn't attempted), it returns `null`.
     * 2. Load the class specified by `config.className` using the `dexLoader`.
     * 3. Check if the loaded class is a subclass of `ModConfModule`. If not, an error is logged, and `null` is returned.
     * 4. If the class is valid, it creates a new instance of it using `ModConfClass(clazz).createNew(this)`.
     * 5. Logs a success message with the loaded class name.
     *
     * If any exception occurs during this process (e.g., `ClassNotFoundException`, issues during instantiation),
     * an error is logged, and `null` is returned.
     *
     * @return An instance of the loaded `ModConfModule` if successful, or `null` otherwise.
     */
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
