@file:SuppressLint("ViewConstructor")

package com.dergoogler.mmrl.modconf

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.pm.PackageInfoCompat
import com.dergoogler.mmrl.modconf.component.ErrorScreen
import com.dergoogler.mmrl.platform.PlatformManager

@Composable
fun ModConfView(kontext: Kontext) {
    val instance = kontext.modconf
    if (instance == null) {
        ErrorScreen(
            title = "Failed to load module",
            description = "Failed to load module: ${kontext.config.className}",
            errorCode = "WX_MODULE_LOAD_FAILED"
        )
        return
    }

    val targetPackages = instance.targetPackages
    if (targetPackages.isNotEmpty()) {
        val myUserId = try {
            PlatformManager.userManager.myUserId
        } catch (_: Exception) {
            0
        }
        val info = PlatformManager.packageManager.getPackageInfo(kontext.packageName, 0, myUserId)
        val versionCode = PackageInfoCompat.getLongVersionCode(info)

        val isSupported = targetPackages.any { target ->
            target.name.matches(kontext.packageName) && versionCode >= target.minVersion
        }

        if (!isSupported) {
            ErrorScreen(
                title = "Unsupported",
                description = "This module does not support the current package or version",
                errorCode = "WX_MODULE_NOT_SUPPORTED"
            )
            return
        }
    }
    // Old
    val context = LocalAppContext.current
    CompositionLocalProvider(
        LocalContext provides kontext,
        LocalAppContext provides context,
        LocalKontext provides kontext,
    ) {
        instance.Content()
    }
}
