package com.dergoogler.mmrl.webui.util.lua

import android.util.Log
import com.dergoogler.mmrl.ext.directBuffer
import com.dergoogler.mmrl.platform.file.SuFile
import com.dergoogler.mmrl.platform.model.ModId.Companion.webrootDir
import com.dergoogler.mmrl.webui.util.WebUIOptions
import party.iroiro.luajava.Lua
import party.iroiro.luajava.lua51.Lua51

class LuaEngine(
    private val options: WebUIOptions,
) {
    private val webroot get() = options.modId.webrootDir

    private val engine = Lua51().also { lua ->
        lua.openLibraries()

        lua.pushJavaObject(options)
        lua.setGlobal("_WX_OPTIONS");

        lua.setExternalLoader(LuaSuFileLoader(options));
    }

    fun evalPrimitive(script: String): Any? {
        val results = engine.eval(script)
        if (results.isEmpty()) return null
        val value = results[0]
        return when(value.type()) {
            Lua.LuaType.NIL -> null
            Lua.LuaType.BOOLEAN -> value.toBoolean()
            Lua.LuaType.NUMBER -> value.toInteger()
            Lua.LuaType.STRING -> value.toString()
            else -> null
        }
    }

    fun run() {
        try {
            val mainScript = SuFile(webroot, "lua", "main.lua")
            if (!mainScript.exists()) {
                Log.e(TAG, "main.lua not found")
                return
            }

            engine.run(mainScript.directBuffer, "main")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to eval Lua scripts", e)
        }
    }

    private companion object {
        const val TAG = "LuaEngine"
    }
}