package com.dergoogler.mmrl.wx.ui.webui.interfaces

import com.dergoogler.mmrl.wx.ui.webui.module
import com.dergoogler.mmrl.wx.ui.webui.util.Permissions
import com.dergoogler.mmrl.wx.ui.webui.util.Permissions.or
import com.dergoogler.mmrl.wx.ui.webui.util.requirePermission
import dev.mmrlx.webui.JavaScriptInterface
import dev.mmrlx.webui.WebUI
import dev.mmrlx.webui.javascript.annotation.ExportVariable

class ModuleInterface(
    webui: WebUI,
) : JavaScriptInterface(webui) {
    override val prototypeClass = "Module"
    override val propertyName = "mod"

    @ExportVariable
    val adbPath = requirePermission(Permissions.MX.MODINFO or Permissions.KSU.MODINFO, "adbPath") {
        module.adbPath.toJSONObject()
    }

    @ExportVariable
    val path = requirePermission(
        Permissions.MX.MODINFO or Permissions.KSU.MODINFO,
        "path"
    ) { module.path.toJSONObject() }

    @ExportVariable
    override val id = requirePermission(
        Permissions.MX.MODINFO or Permissions.KSU.MODINFO,
        "adbPath",
        "",
    ) { module.id }

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