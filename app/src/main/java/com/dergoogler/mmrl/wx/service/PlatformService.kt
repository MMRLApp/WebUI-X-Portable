package com.dergoogler.mmrl.wx.service

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.dergoogler.mmrl.platform.Platform
import com.dergoogler.mmrl.platform.Platform.Companion.createPlatformIntent
import com.dergoogler.mmrl.platform.Platform.Companion.getPlatform
import com.dergoogler.mmrl.wx.R
import com.dergoogler.mmrl.wx.app.utils.NotificationUtils.CHANNEL_ID_PLATFORM
import com.dergoogler.mmrl.wx.app.utils.NotificationUtils.GROUP_KEY_PLATFORM
import com.dergoogler.mmrl.wx.app.utils.NotificationUtils.NOTIFICATION_ID_PLATFORM
import com.dergoogler.mmrl.wx.util.initPlatform
import kotlinx.coroutines.launch


class PlatformService : LifecycleService() {
    override fun onCreate() {
        super.onCreate()
        isActive = true
    }

    override fun onDestroy() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        isActive = false
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        if (intent == null) {
            return START_NOT_STICKY
        }

        setForeground()

        lifecycleScope.launch {
            isActive = initPlatform(baseContext, intent.getPlatform() ?: return@launch)
        }

        return START_STICKY
    }

    private fun baseNotificationBuilder() =
        NotificationCompat.Builder(this, CHANNEL_ID_PLATFORM)
            .setSmallIcon(R.drawable.launcher_outline)

    private fun setForeground() {
        val notification = baseNotificationBuilder()
            .setContentTitle("Platform Service is running")
            .setSilent(true)
            .setOngoing(true)
            .setGroup(GROUP_KEY_PLATFORM)
            .setGroupSummary(true)
            .build()

        startForeground(NOTIFICATION_ID_PLATFORM, notification)
    }

    companion object {
        var isActive by mutableStateOf(false)
            private set

        fun start(
            context: Context,
            mode: Platform,
        ) {
            val intent = context.createPlatformIntent<PlatformService>(mode)
            context.startForegroundService(intent)
        }

        fun stop(
            context: Context,
        ) {
            val intent = Intent(context, PlatformService::class.java)
            context.stopService(intent)
        }
    }
}