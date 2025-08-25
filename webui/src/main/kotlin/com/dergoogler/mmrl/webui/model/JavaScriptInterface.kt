package com.dergoogler.mmrl.webui.model

import android.os.Process
import android.util.Log
import com.dergoogler.mmrl.platform.file.SuFile
import com.dergoogler.mmrl.platform.file.SuFilePermissions
import com.dergoogler.mmrl.platform.model.ModId.Companion.webrootDir
import com.dergoogler.mmrl.webui.interfaces.WXInterface
import com.dergoogler.mmrl.webui.interfaces.WXOptions
import com.sun.jna.Native

/**
 * Represents a JavaScript interface that can be exposed to a web view.
 *
 * This class encapsulates the necessary information to create and manage an instance of a
 * Kotlin class that will be accessible from JavaScript running within a web view.
 *
 * @param T The type of the WebUIInterface that this interface will create.
 * @property clazz The Class object representing the Kotlin class implementing WebUIInterface.
 * @property initargs Optional arguments to be passed to the constructor of the class. If null, the constructor taking a WXOptions object will be used.
 * @property parameterTypes Optional array of parameter types for the constructor. If null, the constructor taking a WXOptions object will be used.
 */
data class JavaScriptInterface<T : WXInterface>(
    val clazz: Class<T>,
    val initargs: Array<Any>? = null,
    val parameterTypes: Array<Class<*>>? = null,
    val dexConfig: WebUIConfigDexFile? = null,
) {
    data class Instance(
        private val inst: WXInterface,
    ) {
        val name: String = inst.name
        val instance: WXInterface = inst

        fun unregister() {
            Native.unregister(inst.javaClass)
        }
    }

    fun createNew(wxOptions: WXOptions): Instance {
        val constructor = if (parameterTypes != null) {
            clazz.getDeclaredConstructor(WXOptions::class.java, *parameterTypes)
        } else {
            clazz.getDeclaredConstructor(WXOptions::class.java)
        }

        val newInstance = if (initargs != null) {
            constructor.newInstance(wxOptions, *initargs)
        } else {
            constructor.newInstance(wxOptions)
        }

        val modId = wxOptions.options.modId
        val context = wxOptions.options.context

        if (wxOptions.options.pluginsEnabled && dexConfig?.copySharedObjects == true) {
            try {
                val libraries = dexConfig.sharedObjects.mapNotNull {
                    val sharedObjectFile = SuFile(modId.webrootDir, it)
                    val sharedObjectDestinationFile =
                        SuFile(context.applicationInfo.nativeLibraryDir, sharedObjectFile.name)

                    if (!sharedObjectFile.exists()) return@mapNotNull null

                    val uid = Process.myUid()

                    sharedObjectFile.setOwner(uid, uid)
                    sharedObjectFile.setPermissions(SuFilePermissions.PERMISSION_755)
                    sharedObjectFile.copyTo(sharedObjectDestinationFile, overwrite = true)

                    sharedObjectDestinationFile.name.substringBeforeLast(".")
                        .replace(Regex("^lib"), "")
                }

                for (library in libraries) {
                    Native.register(newInstance.javaClass, library)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error copying shared objects", e)
            }
        }

        return Instance((newInstance as WXInterface))
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as JavaScriptInterface<*>

        if (clazz != other.clazz) return false
        if (!initargs.contentEquals(other.initargs)) return false
        if (!parameterTypes.contentEquals(other.parameterTypes)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = clazz.hashCode()
        result = 31 * result + initargs.contentHashCode()
        result = 31 * result + parameterTypes.contentHashCode()
        return result
    }

    private companion object {
        const val TAG = "JavaScriptInterface"
    }
}