package com.dergoogler.mmrl.wx.util

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavOptionsBuilder
import com.dergoogler.mmrl.datastore.model.WorkingMode
import com.dergoogler.mmrl.ext.exception.BrickException
import com.dergoogler.mmrl.ext.navigateSingleTopTo
import com.dergoogler.mmrl.ext.toFormattedDateSafely
import com.dergoogler.mmrl.platform.Platform
import com.dergoogler.mmrl.platform.PlatformManager
import com.dergoogler.mmrl.platform.content.LocalModule
import com.dergoogler.mmrl.platform.file.SuFile
import com.dergoogler.mmrl.platform.model.ModId
import com.dergoogler.mmrl.platform.model.ModId.Companion.putBaseDir
import com.dergoogler.mmrl.platform.model.ModuleConfig.Companion.asModuleConfig
import com.dergoogler.mmrl.platform.stub.IServiceManager
import com.dergoogler.mmrl.webui.activity.WXActivity.Companion.launchWebUI
import com.dergoogler.mmrl.webui.activity.WXActivity.Companion.launchWebUIX
import com.dergoogler.mmrl.wx.datastore.model.UserPreferences
import com.dergoogler.mmrl.wx.datastore.model.WebUIEngine
import com.dergoogler.mmrl.wx.datastore.providable.LocalUserPreferences
import com.dergoogler.mmrl.wx.ui.activity.webui.KsuWebUIActivity
import com.dergoogler.mmrl.wx.ui.activity.webui.WebUIActivity
import kotlinx.coroutines.CoroutineScope
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

fun Context.extractZipFromAssets(
    assetName: String,
    outputDir: File,
) {
    if (!outputDir.exists()) {
        outputDir.mkdirs()
    }

    val buffer = ByteArray(4096)

    this.assets.open(assetName).use { inputStream ->
        ZipInputStream(BufferedInputStream(inputStream)).use { zipStream ->
            var entry: ZipEntry? = zipStream.nextEntry
            while (entry != null) {
                val filePath = File(outputDir, entry.name)

                if (entry.isDirectory) {
                    filePath.mkdirs()
                } else {
                    filePath.parentFile?.mkdirs()

                    FileOutputStream(filePath).use { output ->
                        var len: Int
                        while (zipStream.read(buffer).also { len = it } > 0) {
                            output.write(buffer, 0, len)
                        }
                    }
                }
                zipStream.closeEntry()
                entry = zipStream.nextEntry
            }
        }
    }
}

val LocalModule.versionDisplay
    get(): String {
        val included = "\\(.*?${versionCode}.*?\\)".toRegex()
            .containsMatchIn(version)

        return if (included) {
            version
        } else {
            "$version (${versionCode})"
        }
    }

val Float.toFormattedDateSafely: String
    @Composable
    get() {
        val prefs = LocalUserPreferences.current
        return this.toFormattedDateSafely(prefs.datePattern)
    }

val Long.toFormattedDateSafely: String
    @Composable
    get() {
        val prefs = LocalUserPreferences.current
        return this.toFormattedDateSafely(prefs.datePattern)
    }

fun Platform.toWorkingMode() = when (this) {
    Platform.Magisk -> WorkingMode.MODE_MAGISK
    Platform.KernelSU -> WorkingMode.MODE_KERNEL_SU
    Platform.KsuNext -> WorkingMode.MODE_KERNEL_SU_NEXT
    Platform.APatch -> WorkingMode.MODE_APATCH
    Platform.NonRoot -> WorkingMode.MODE_NON_ROOT
    else -> throw BrickException("Unsupported Platform")
}

private suspend fun init(
    platform: Platform,
    context: Context,
    self: PlatformManager,
): IServiceManager? {
    if (platform.isNonRoot) {
        return self.from(
            NonRootProvider(
                context,
                platform,
            )
        )
    }

    return self.from(
        RootProvider(
            context,
            platform,
        )
    )
}

suspend fun initPlatform(
    context: Context,
    platform: Platform,
) = PlatformManager.init {
    init(platform, context, this)
}

suspend fun initPlatform(
    scope: CoroutineScope,
    context: Context,
    platform: Platform,
) = PlatformManager.init(scope) {
    init(platform, context, this)
}

fun UserPreferences.launchWebUI(context: Context, modId: ModId) {
    val config = modId.asModuleConfig

    val baseDir = context.getBaseDir().path

    val applyIntent: Intent.() -> Unit = {
        putBaseDir(baseDir)
    }

    if (webuiEngine == WebUIEngine.PREFER_MODULE) {
        val configEngine = config.getWebuiEngine(context)

        if (configEngine == null) {
            context.launchWebUIX<WebUIActivity>(modId)
            return
        }

        when (configEngine) {
            "wx" -> context.launchWebUIX<WebUIActivity>(modId, baseDir)
            "ksu" -> context.launchWebUI<KsuWebUIActivity>(modId.id, applyIntent)
            else -> Toast.makeText(context, "Unknown WebUI engine", Toast.LENGTH_SHORT).show()
        }

        return
    }


    if (webuiEngine == WebUIEngine.WX) {
        context.launchWebUIX<WebUIActivity>(modId, baseDir)
        return

    }

    if (webuiEngine == WebUIEngine.KSU) {
        context.launchWebUI<KsuWebUIActivity>(modId.id, applyIntent)
        return
    }

    Toast.makeText(context, "Unknown WebUI engine", Toast.LENGTH_SHORT).show()
}

fun Map<String, Any?>?.getBoolProp(key: String, def: Boolean = false): Boolean {
    val value = this?.get(key)

    return if (value is Boolean) {
        value
    } else {
        def
    }
}


inline fun <reified T> Map<String, Any?>?.getProp(key: String, def: T): T {
    val value = this?.get(key)
    return if (value is T) {
        value
    } else {
        def
    }
}

inline fun <reified T> Map<String, Any?>?.getPropOrNull(key: String): T? = getProp(key, null)

fun Context.getBaseDir(
    platform: Platform = PlatformManager.platform,
): SuFile {
    val filesDir = getExternalFilesDir(null)

    return if (platform.isNonRoot) {
        if (filesDir == null) {
            throw BrickException("Failed to get filesDir")
        }

        SuFile(filesDir)
    } else {
        SuFile(ModId.ADB_DIR)
    }
}

fun <T : Any> NavController.navigateSingleTopTo(
    route: T,
    launchSingleTop: Boolean = true,
    builder: NavOptionsBuilder.() -> Unit = {},
) = navigate(
    route = route
) {
    this.launchSingleTop = launchSingleTop
    restoreState = true
    builder()
}

fun <T : Any> NavController.navigatePopUpTo(
    route: T,
    launchSingleTop: Boolean = true,
    restoreState: Boolean = true,
    inclusive: Boolean = true,
) = navigateSingleTopTo(
    route = route
) {
    popUpTo(
        id = currentDestination?.parent?.id ?: graph.findStartDestination().id
    ) {
        this.saveState = restoreState
        this.inclusive = inclusive
    }
    this.launchSingleTop = launchSingleTop
    this.restoreState = restoreState
}