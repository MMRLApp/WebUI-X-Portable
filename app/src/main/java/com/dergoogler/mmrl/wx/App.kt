package com.dergoogler.mmrl.wx

import android.app.Application
import com.dergoogler.mmrl.platform.PlatformManager
import com.dergoogler.mmrl.wx.app.utils.NotificationUtils
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class App : Application() {
    override fun onCreate() {
        super.onCreate()

        NotificationUtils.init(this)

        PlatformManager.setHiddenApiExemptions()
    }

    companion object {
        const val TAG = "App"
    }
}