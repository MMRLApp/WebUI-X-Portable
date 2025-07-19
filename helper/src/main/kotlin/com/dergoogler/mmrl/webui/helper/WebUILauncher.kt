package com.dergoogler.mmrl.webui.helper

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.annotation.RequiresPermission
import com.dergoogler.mmrl.platform.PLATFORM_KEY
import com.dergoogler.mmrl.platform.Platform
import com.dergoogler.mmrl.platform.Platform.Companion.putPlatform
import com.dergoogler.mmrl.platform.model.ModId
import com.dergoogler.mmrl.platform.model.ModId.Companion.putModId

const val PERMISSION_WEBUI_X = "com.dergoogler.mmrl.permission.WEBUI_X"
const val PERMISSION_WEBUI_X_DEBUG  = "com.dergoogler.mmrl.debug.permission.WEBUI_X"
const val PERMISSION_WEBUI_LEGACY  = "com.dergoogler.mmrl.permission.WEBUI_LEGACY"
const val PERMISSION_WEBUI_LEGACY_DEBUG  = "com.dergoogler.mmrl.debug.permission.WEBUI_LEGACY"

data class WebUIPermissions(
    private val debugPostFix: String,
) {
    val WEBUI_X = "com.dergoogler.mmrl$debugPostFix.permission.WEBUI_X"
    val WEBUI_LEGACY = "com.dergoogler.mmrl$debugPostFix.permission.WEBUI_LEGACY"
}

class WebUILauncher(
    private val debug: Boolean = false,
) {
    @RequiresPermission(anyOf = [PERMISSION_WEBUI_X, PERMISSION_WEBUI_X_DEBUG])
    fun launchWX(
        context: Context,
        modId: ModId,
        platform: Platform,
        transformer: (Intent.() -> Intent)? = null,
    ) {
        try {
            val intent = Intent().apply {
                component = ComponentName(packageName, X)

                putModId(modId.toString())
                putPlatform(platform)

                if (transformer != null) {
                    transformer()
                }
            }

            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(TAG, "launchWX: ${e.message}")
        }
    }

    @RequiresPermission(anyOf = [PERMISSION_WEBUI_LEGACY, PERMISSION_WEBUI_LEGACY_DEBUG])
    fun launchLegacy(
        context: Context,
        modId: ModId,
        platform: Platform,
        transformer: (Intent.() -> Intent)? = null,
    ) {
        try {
            val intent = Intent().apply {
                component = ComponentName(packageName, LEGACY)

                putModId(modId.toString())
                putPlatform(platform)

                if (transformer != null) {
                    transformer()
                }
            }

            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(TAG, "launchWX: ${e.message}")
        }
    }

    private val debugPostFix = if (debug) ".debug" else ""
    private val packageName = "com.dergoogler.mmrl.wx$debugPostFix"

    val permissions = WebUIPermissions(debugPostFix)

    private companion object {
        const val TAG = "WebUILauncher"

        const val X = "com.dergoogler.mmrl.wx.ui.activity.webui.WebUIActivity"
        const val LEGACY = "com.dergoogler.mmrl.wx.ui.activity.webui.KsuWebUIActivity"
    }
}