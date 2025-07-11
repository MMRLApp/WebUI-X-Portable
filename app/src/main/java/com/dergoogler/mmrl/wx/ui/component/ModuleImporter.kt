package com.dergoogler.mmrl.wx.ui.component

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.dergoogler.mmrl.ext.systemBarsPaddingEnd
import com.dergoogler.mmrl.platform.PlatformManager
import com.dergoogler.mmrl.platform.content.LocalModule
import com.dergoogler.mmrl.platform.file.ExtFile
import com.dergoogler.mmrl.ui.component.dialog.ConfirmData
import com.dergoogler.mmrl.ui.component.dialog.rememberConfirm
import com.dergoogler.mmrl.wx.R
import java.io.BufferedInputStream
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

@Composable
fun ModuleImporter() {
    val context = LocalContext.current
    val interactionSource = remember { MutableInteractionSource() }
    val confirm = rememberConfirm()
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult

            val result = importZipToModules(context, uri)

            confirm(
                ConfirmData(
                    title = result.title,
                    description = result.message,
                    onConfirm = {},
                    onClose = {}
                )
            )
        }

    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            if (interaction is PressInteraction.Release) {
                launcher.launch("application/zip")
            }
        }
    }

    FloatingActionButton(
        modifier = Modifier.systemBarsPaddingEnd(),
        interactionSource = interactionSource,
        onClick = {},
        contentColor = MaterialTheme.colorScheme.onPrimary,
        containerColor = MaterialTheme.colorScheme.primary
    ) {
        Icon(
            painter = painterResource(id = R.drawable.package_import),
            contentDescription = null
        )
    }
}

data class ResultData(
    val title: String,
    val message: String,
)

fun importZipToModules(context: Context, zipUri: Uri): ResultData {
    val tempZipName = "temp_module_import_${System.currentTimeMillis()}.zip"
    val zipFile = ExtFile(context.cacheDir, tempZipName)

    try {
        context.contentResolver.openInputStream(zipUri)?.use { input ->
            FileOutputStream(zipFile).use { output ->
                input.copyTo(output)
            }
        } ?: run {
            return ResultData(
                title = "Failed",
                message = "Unable to open ZIP file from the provided URI. Check permissions or URI validity."
            )
        }

        val newModule: LocalModule? = PlatformManager.moduleManager.getModuleInfo(zipFile.path)
        if (newModule == null) {
            return ResultData(
                title = "Failed",
                message = "Could not parse module information from the ZIP file. It might be corrupted or not a valid module."
            )
        }

        val existingModule: LocalModule? = PlatformManager.moduleManager.getModuleById(newModule.id)
        if (existingModule != null) {
            return ResultData(
                title = "Failed",
                message = "A module with ID '${newModule.id}' already exists. Please remove the existing module first if you intend to replace it."
            )
        }

        val baseDir = context.getExternalFilesDir(null)
        if (baseDir == null) {
            return ResultData(
                title = "Failed",
                message = "Unable to access the external files directory. Storage may not be available or accessible."
            )
        }

        val targetDir = ExtFile(baseDir, "modules/${newModule.id}")
        if (targetDir.exists()) {
            if (!targetDir.delete()) {
                return ResultData(
                    title = "Failed",
                    message = "An old version of the module directory exists at '${targetDir.path}', but it could not be deleted."
                )
            }
        }

        if (!targetDir.mkdirs()) {
            if (!targetDir.exists() || !targetDir.isDirectory) {
                return ResultData(
                    title = "Failed",
                    message = "Failed to create module directory at '${targetDir.path}'. Check storage permissions and available space."
                )
            }
        }

        unzip(zipFile, targetDir)

        val successMessage = "Module '${newModule.name}' imported successfully."
        Log.i("ModuleImport", successMessage) // Log success
        return ResultData(
            title = "Success",
            message = successMessage
        )

    } catch (e: Exception) {
        Log.e("ModuleImport", "Failed to import module from URI: $zipUri", e)
        return ResultData(
            title = "Import Error",
            message = "An unexpected error occurred during module import: ${e.localizedMessage ?: "Unknown error. Check logs for details."}"
        )
    } finally {
        if (zipFile.exists()) {
            if (!zipFile.delete()) {
                Log.w("ModuleImport", "Failed to delete temporary ZIP file: ${zipFile.path}")
            }
        }
    }
}

fun unzip(zipFile: ExtFile, targetDir: ExtFile) {
    ZipInputStream(BufferedInputStream(FileInputStream(zipFile))).use { zis ->
        var entry: ZipEntry?
        while (zis.nextEntry.also { entry = it } != null) {
            val file = ExtFile(targetDir, entry!!.name)
            if (entry.isDirectory) {
                file.mkdirs()
            } else {
                file.parentFile?.mkdirs()
                FileOutputStream(file).use { out -> zis.copyTo(out) }
            }
        }
    }
}