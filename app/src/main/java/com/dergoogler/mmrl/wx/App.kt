package com.dergoogler.mmrl.wx

import com.dergoogler.mmrl.platform.PlatformManager
import com.dergoogler.mmrl.wx.app.utils.NotificationUtils
import dagger.hilt.android.HiltAndroidApp
import dev.mmrlx.nio.SuFile
import dev.mmrlx.utilities.app.LifecycleApplication
import dev.mmrlx.utilities.coroutines.newCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
    }

    companion object {
        const val TAG = "App"
    }
}