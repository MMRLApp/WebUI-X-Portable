@file:Suppress("PropertyName", "unused", "CanBeParameter")

package com.dergoogler.mmrl.modconf.helper

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.annotation.RequiresPermission
import com.dergoogler.mmrl.platform.Platform
import com.dergoogler.mmrl.platform.Platform.Companion.putPlatform
import com.dergoogler.mmrl.platform.model.ModId
import com.dergoogler.mmrl.platform.model.ModId.Companion.putModId

const val PERMISSION_MODCONF = "com.dergoogler.mmrl.permission.MODCONF"

data class ModConfPermissions(
    private val debugPostFix: String,
) {
    val MODCONF = "com.dergoogler.mmrl$debugPostFix.permission.MODCONF"
}

class WebUILauncher(
    private val debug: Boolean = false,
    private val debugPostFix: String = if (debug) ".debug" else "",
    private val packageName: String = "com.dergoogler.mmrl.wx$debugPostFix",
) {
    @RequiresPermission(anyOf = [PERMISSION_MODCONF])
    fun launch(
        context: Context,
        modId: ModId,
        platform: Platform,
        transformer: (Intent.() -> Intent)? = null,
    ) {
        try {
            val intent = Intent().apply {
                component = ComponentName(packageName, MC)

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

    val permissions = ModConfPermissions(debugPostFix)

    private companion object {
        const val TAG = "ModConfLauncher"

        const val MC = "com.dergoogler.mmrl.wx.ui.activity.modconf.ModConfActivity"
    }
}