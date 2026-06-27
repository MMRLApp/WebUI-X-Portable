package com.dergoogler.mmrl.wx.ui.webui.interfaces

import com.dergoogler.mmrl.wx.ui.webui.module
import dev.mmrlx.webui.JavaScriptInterface
import dev.mmrlx.webui.WebUI
import dev.mmrlx.webui.javascript.annotation.ExportVariable

class ModuleInterface(
    webui: WebUI,
) : JavaScriptInterface(webui) {
    override val prototypeClass = "Module"
    override val propertyName = "mod"

    @ExportVariable
    val adbPath = module.adbPath.toJSONObject()

    @ExportVariable
    val path = module.path.toJSONObject()

    @ExportVariable
    override val id = module.id

    @ExportVariable
    val name = module.name

    @ExportVariable
    val author = module.author

    @ExportVariable
    val version = module.version

    @ExportVariable
    val versionCode = module.versionCode

    @ExportVariable
    val description = module.description
}