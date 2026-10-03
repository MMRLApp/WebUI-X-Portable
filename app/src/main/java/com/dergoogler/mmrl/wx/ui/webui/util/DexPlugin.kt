package com.dergoogler.mmrl.wx.ui.webui.util

import android.util.Log
import com.dergoogler.mmrl.wx.model.module.dex
import com.dergoogler.mmrl.wx.ui.webui.module
import com.dergoogler.mmrl.wx.ui.webui.sufile
import dev.mmrlx.webui.WebUI
import dev.mmrlx.webui.dex.registerDexPlugin

fun WebUI.dexPlugin(): WebUI {
    val dexFiles = module.webrootConfig.dex

    dexFiles.forEach {
        val path = it.path ?: return@forEach
        val className = it.className ?: return@forEach

        val filePath = sufile(module.path.webrootDir, path)

        Log.d("", "exists: ${filePath.exists()}")

        registerDexPlugin {
            plugin(filePath.path)
            className(className)
        }
    }

    return this
}