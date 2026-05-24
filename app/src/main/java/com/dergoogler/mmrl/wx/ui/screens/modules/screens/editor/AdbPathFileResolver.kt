package com.dergoogler.mmrl.wx.ui.screens.modules.screens.editor

import android.util.Log
import com.dergoogler.mmrl.wx.model.module.AdbPath
import dev.mmrlx.nio.SuFile
import dev.mmrlx.nio.inputStream
import io.github.rosemoe.sora.langs.textmate.registry.provider.FileResolver
import java.io.InputStream

class AdbPathFileResolver(
    private val debug: Boolean,
    private val adbPath: AdbPath,
) : FileResolver {
    override fun resolveStreamByPath(path: String): InputStream? {
        val file = SuFile(adbPath.localDir, "webuix", path)

        if (debug) {
            Log.d("AdbPathFileResolver", file.toString())
        }

        return if (file.exists()) {
            file.inputStream()
        } else {
            null
        }
    }
}