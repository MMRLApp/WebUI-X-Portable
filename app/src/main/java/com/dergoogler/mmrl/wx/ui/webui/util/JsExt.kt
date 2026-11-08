package com.dergoogler.mmrl.wx.ui.webui.util

import dev.mmrlx.webui.JavaScriptClassScope
import dev.mmrlx.webui.JavaScriptScope

fun JavaScriptScope.cls(name: String, block: JavaScriptClassScope.() -> Unit) =
    clazz("cls.$name", block)