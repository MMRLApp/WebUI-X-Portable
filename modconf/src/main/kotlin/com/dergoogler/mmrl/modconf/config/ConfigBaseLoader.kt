package com.dergoogler.mmrl.modconf.config

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.dergoogler.mmrl.modconf.Kontext
import com.dergoogler.mmrl.platform.PlatformManager
import com.dergoogler.mmrl.platform.file.SuFile
import com.dergoogler.mmrl.platform.hiddenApi.HiddenPackageManager
import com.dergoogler.mmrl.platform.hiddenApi.HiddenUserManager
import com.dergoogler.mmrl.platform.model.ModId.Companion.modconfDir
import dalvik.system.BaseDexClassLoader
import dalvik.system.DexClassLoader
import dalvik.system.InMemoryDexClassLoader
import java.nio.ByteBuffer

internal open class ConfigBaseLoader() {
    /**
     * Creates a ClassLoader for a standalone .dex file.
     */
    fun createDexLoader(
        kontext: Kontext,
        dexPath: String,
    ): BaseDexClassLoader? {
        val file = SuFile(kontext.modId.modconfDir, dexPath)

        if (!file.isFile || file.extension != "dex") {
            Log.e(TAG, "Provided path is not a valid .dex file: ${file.path}")
            return null
        }

        // Using InMemoryDexClassLoader is efficient if DEX files are not excessively large.
        val dexFileBytes = file.readBytes()
        return InMemoryDexClassLoader(ByteBuffer.wrap(dexFileBytes), kontext.classLoader)
    }

    /**
     * Creates a ClassLoader for a class within an installed APK.
     */
    fun createApkLoader(context: Context, packageName: String): BaseDexClassLoader? {
        return try {
            val pm: HiddenPackageManager = PlatformManager.packageManager
            val um: HiddenUserManager = PlatformManager.userManager
            val appInfo = pm.getApplicationInfo(packageName, um.myUserId, 0)
            val apkPath = appInfo.sourceDir
            val nativeLibPath = appInfo.nativeLibraryDir

            val optimizedDir = context.getDir("dex_opt", Context.MODE_PRIVATE).absolutePath

            DexClassLoader(
                apkPath,
                optimizedDir,
                nativeLibPath,
                context.classLoader
            )
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(TAG, "Could not find package: $packageName", e)
            null
        }
    }

    private companion object {
        const val TAG = "ConfigBaseLoader"
    }
}