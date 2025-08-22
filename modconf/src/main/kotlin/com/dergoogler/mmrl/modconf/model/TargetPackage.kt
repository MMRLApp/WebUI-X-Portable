package com.dergoogler.mmrl.modconf.model

import com.dergoogler.mmrl.modconf.Kontext
import com.dergoogler.mmrl.platform.PlatformManager

data class TargetPackage(
    val name: Regex,
    val minVersion: Long,
) {
    fun getInfo(kontext: Kontext, userId: Int = 0) =
        PlatformManager.packageManager.getPackageInfo(kontext.packageName, 0, userId)
}