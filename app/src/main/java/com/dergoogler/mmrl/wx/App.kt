package com.dergoogler.mmrl.wx

import android.util.Log
import com.dergoogler.mmrl.platform.PlatformManager
import com.dergoogler.mmrl.wx.app.utils.NotificationUtils
import com.dergoogler.mmrl.wx.util.extractZipFromAssets
import dagger.hilt.android.HiltAndroidApp
import dev.mmrlx.nio.SuFile
import dev.mmrlx.utilities.app.LifecycleApplication
import dev.mmrlx.utilities.coroutines.newCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

@HiltAndroidApp
class App : LifecycleApplication() {
    init {
        val scope = newCoroutineScope(Dispatchers.IO, false)
        scope.launch {
            SuFile.AutoInit(this@App)
        }
    }

    override fun onCreate() {
        super.onCreate()

        NotificationUtils.init(this)

        PlatformManager.setHiddenApiExemptions()

        try {
            val filesDir = applicationContext.filesDir
            // KSU WebUI Demo by KOWX712 @ GitHub
            val output = File(filesDir, "modules/ksuwebui_demo")
            // bad apple by xx (backslashxx) & KOWX712 @ GitHub
            val output2 = File(filesDir, "modules/bad_apple")
            applicationContext.extractZipFromAssets("webuix-demo.zip", output)
            applicationContext.extractZipFromAssets("webuix-demo2.zip", output2)
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(TAG, "onCreate: $e")
        }
    }

    companion object {
        const val TAG = "App"
    }
}