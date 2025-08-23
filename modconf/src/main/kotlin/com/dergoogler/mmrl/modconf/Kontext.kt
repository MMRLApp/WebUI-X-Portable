package com.dergoogler.mmrl.modconf

import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.util.Log
import com.dergoogler.mmrl.modconf.config.ModConfConfig.Companion.asModconfConfig
import com.dergoogler.mmrl.platform.file.SuFile
import com.dergoogler.mmrl.platform.model.ModId
import com.dergoogler.mmrl.platform.model.ModId.Companion.modconfDir
import dalvik.system.InMemoryDexClassLoader
import java.nio.ByteBuffer

class Kontext(
    private val context: Context,
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

    private fun createDexLoader(): ClassLoader? {
        val entryPointPaths: List<String>? = config.entryPoints
        val className: String? = config.className
        if (entryPointPaths == null || className == null) {
            Log.e(TAG, "Missing entryPoints or className in config")
            return null
        }

        val entryPointFiles = entryPointPaths.map { SuFile(modId.modconfDir, it) }

        if (entryPointFiles.any { !it.isFile || it.extension != "dex" }) {
            Log.e(TAG, "Invalid entryPoint file(s)")
            return null
        }

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                val dexFilesBytes = entryPointFiles.map { ByteBuffer.wrap(it.readBytes()) }
                InMemoryDexClassLoader(dexFilesBytes.toTypedArray(), classLoader)
            } else {
                val dexFileBytes = entryPointFiles.first().readBytes()
                InMemoryDexClassLoader(ByteBuffer.wrap(dexFileBytes), classLoader)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load entryPoints", e)
            null
        }
    }

    private companion object {
        const val TAG = "Kontext"
    }
}
