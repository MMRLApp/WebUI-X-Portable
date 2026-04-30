package com.dergoogler.mmrl.wx

import com.dergoogler.mmrl.platform.PlatformManager
import com.dergoogler.mmrl.wx.app.utils.NotificationUtils
import dagger.hilt.android.HiltAndroidApp
import dev.mmrlx.utilities.app.LifecycleApplication

@HiltAndroidApp
class App : LifecycleApplication() {
    override fun onCreate() {
        super.onCreate()

        NotificationUtils.init(this)

        PlatformManager.setHiddenApiExemptions()
    }

    companion object {
        const val TAG = "App"
    }
}