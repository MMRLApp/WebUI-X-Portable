package com.dergoogler.mmrl.webui.util.lua

import android.util.Log
import androidx.core.net.toUri
import com.dergoogler.mmrl.ext.directBuffer
import com.dergoogler.mmrl.hybridwebui.HybridWebUI
import com.dergoogler.mmrl.platform.file.SuFile
import com.dergoogler.mmrl.platform.model.ModId.Companion.webrootDir
import com.dergoogler.mmrl.webui.util.WebUIOptions
import com.dergoogler.mmrl.webui.view.WXView
import party.iroiro.luajava.lua51.Lua51

class LuaEngine(
    private val options: WebUIOptions,
    private val wx: WXView,
) {
    fun interface LuaRegisterPathHandler {
        fun invoke(path: String, handler: HybridWebUI.PathHandler, authority: String?)
    }

    fun interface LuaRegisterEventListener {
        fun invoke(objectName: String, event: HybridWebUI.EventListener)
    }

    private val webroot get() = options.modId.webrootDir

    private val engine = Lua51().also { lua ->
        lua.openLibraries()

        lua.setExternalLoader(LuaSuFileLoader(options));

        lua.pushJavaClass(String::class.java)
        lua.setGlobal("String")

        lua.pushJavaClass(Int::class.java)
        lua.setGlobal("Int")

        lua.pushJavaClass(Long::class.java)
        lua.setGlobal("Long")

        lua.pushJavaClass(Float::class.java)
        lua.setGlobal("Float")

        lua.pushJavaClass(Double::class.java)
        lua.setGlobal("Double")

        lua.pushJavaClass(Boolean::class.java)
        lua.setGlobal("Boolean")

        lua.pushJavaClass(StringBuilder::class.java)
        lua.setGlobal("StringBuilder")

        lua.pushJavaClass(HashMap::class.java)
        lua.setGlobal("HashMap")

        lua.pushJavaClass(ArrayList::class.java)
        lua.setGlobal("ArrayList")

        lua.pushJavaObject(object : LuaRegisterPathHandler {
            override fun invoke(
                path: String,
                handler: HybridWebUI.PathHandler,
                authority: String?,
            ) {
                try {
                    val uriAuthority = authority?.toUri() ?: wx.uri
                    wx.addPathHandler(path, handler, uriAuthority)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to register path handler", e)
                }
            }
        }).also { lua.setGlobal("_registerPathHandler") }

        lua.pushJavaObject(object : LuaRegisterEventListener {
            override fun invoke(objectName: String, event: HybridWebUI.EventListener) {
                try {
                    wx.addEventListener(objectName, event)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to register event listener", e)
                }
            }
        }).also { lua.setGlobal("_registerEventListener") }
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