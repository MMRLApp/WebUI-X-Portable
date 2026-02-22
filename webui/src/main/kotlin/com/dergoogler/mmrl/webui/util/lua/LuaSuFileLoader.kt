package com.dergoogler.mmrl.webui.util.lua

import android.util.Log
import com.dergoogler.mmrl.ext.directBuffer
import com.dergoogler.mmrl.platform.file.Path
import com.dergoogler.mmrl.platform.file.SuFile
import com.dergoogler.mmrl.platform.model.ModId.Companion.webrootDir
import com.dergoogler.mmrl.webui.util.WebUIOptions
import party.iroiro.luajava.ClassPathLoader
import party.iroiro.luajava.Lua
import java.nio.Buffer

class LuaSuFileLoader(
    private val options: WebUIOptions,
) : ClassPathLoader() {
    private val context get() = options.context
    private val webroot get() = options.modId.webrootDir

    override fun load(module: String, ignored: Lua): Buffer? {
        try {
            val path = getPath(module)

            if (path.startsWith("wx:")) {
                val assetPath = path.substring("wx:".length)
                return context.assets.open(Path.resolve("lua", assetPath)).directBuffer
            }

            val file = SuFile(webroot, "lua", path)
            return file.directBuffer
        } catch (e: Exception) {
            Log.e(TAG, "Error loading Lua file: ${e.message}")
            return null
        }
    }

    private companion object {
        const val TAG = "LuaSuFileLoader"
    }
}