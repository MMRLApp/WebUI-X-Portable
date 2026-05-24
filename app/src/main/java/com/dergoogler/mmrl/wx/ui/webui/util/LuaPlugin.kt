package com.dergoogler.mmrl.wx.ui.webui.util

import com.dergoogler.mmrl.wx.ui.webui.module
import dev.mmrlx.webui.WebUI
import dev.mmrlx.webui.registerLuaPlugin

// TODO: expose new module paths data class with WebUISettings.extra<T>()
fun WebUI.luaPlugin(): WebUI {
    // Only register the Lua plugin when the `index.lua` file exists
    val indexLuaFile = file(module.path.webrootLuaIndex)

    if (!indexLuaFile.exists()) return this

    return registerLuaPlugin {
        plugin(indexLuaFile.path)
        global["mod"] = module.toLuaTable()
    }
}