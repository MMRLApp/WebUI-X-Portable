package com.dergoogler.mmrl.wx.util

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.dergoogler.mmrl.platform.Platform.Companion.getPlatform
import com.dergoogler.mmrl.platform.PlatformManager
import com.dergoogler.mmrl.wx.App.Companion.TAG
import java.io.File

class NonRootService : Service() {
    private val context by lazy {
        PlatformManager.context
    }

    override fun onBind(intent: Intent): IBinder {
        val platform = intent.getPlatform() ?: throw Exception("Platform not found")

        try {
            val filesDir = context.getBaseDir(platform)
            // KSU WebUI Demo by KOWX712 @ GitHub
            val output = File(filesDir, "modules/ksuwebui_demo")
            //  bad apple by xx (backslashxx) & KOWX712 @ GitHub
            val output2 = File(filesDir, "modules/bad_apple")
            context.extractZipFromAssets("webuix-demo.zip", output)
            context.extractZipFromAssets("webuix-demo2.zip", output2)
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(TAG, "onCreate: $e")
        }

        return NonServiceManager(context, platform)
    }
}