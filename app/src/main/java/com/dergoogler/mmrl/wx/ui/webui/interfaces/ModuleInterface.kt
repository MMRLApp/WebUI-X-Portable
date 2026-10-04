package com.dergoogler.mmrl.wx.ui.webui.interfaces

import com.dergoogler.mmrl.wx.model.module.toJSONObject
import com.dergoogler.mmrl.wx.ui.webui.module
import dev.mmrlx.webui.JavaScriptRegistry

fun JavaScriptRegistry.moduleInterface() = namespace("mod") {
    property("adbPath", module.adbPath.toJSONObject())
    property("path", module.path.toJSONObject())
    property("id", module.id)
    property("author", module.author)
    property("version", module.version)
    property("versionCode", module.versionCode)
    property("description", module.description)
    property("config", module.webrootConfig.toJSONObject())

}