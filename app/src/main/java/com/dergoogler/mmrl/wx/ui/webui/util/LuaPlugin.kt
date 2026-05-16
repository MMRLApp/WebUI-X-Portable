package com.dergoogler.mmrl.wx.ui.webui.util

import com.dergoogler.mmrl.wx.ui.webui.modId
import dev.mmrlx.webui.WebUI
import dev.mmrlx.webui.registerLuaPlugin

// TODO: expose new module paths data class with WebUISettings.extra<T>()
fun WebUI.luaPlugin(): WebUI {
    // Only register the Lua plugin when the `index.lua` file exists
    val indexLuaFile = file("/data/adb/modules/${modId}/webroot/index.lua")

    if (!indexLuaFile.exists()) return this

    return registerLuaPlugin {
        plugin(indexLuaFile.path)
    }
}