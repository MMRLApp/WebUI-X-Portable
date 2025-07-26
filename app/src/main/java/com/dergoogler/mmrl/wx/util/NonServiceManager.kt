package com.dergoogler.mmrl.wx.util

import android.content.Context
import com.dergoogler.mmrl.ext.exception.BrickException
import com.dergoogler.mmrl.platform.Platform
import com.dergoogler.mmrl.platform.service.ServiceManager
import com.dergoogler.mmrl.platform.stub.IModuleManager

class NonServiceManager(
    context: Context,
    platform: Platform,
) : ServiceManager(platform) {
    private val xModuleManager by lazy {
        when (platform) {
            Platform.NonRoot -> {
                NonRootModuleManager(context, platform)
            }

            else -> throw BrickException("Unsupported Platform $platform")
        }
    }

    override fun getModuleManager(): IModuleManager = xModuleManager
}
