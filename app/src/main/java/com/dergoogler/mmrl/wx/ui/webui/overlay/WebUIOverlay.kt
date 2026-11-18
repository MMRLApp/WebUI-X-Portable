package com.dergoogler.mmrl.wx.ui.webui.overlay

import dev.mmrlx.webui.overlay.OverlayActivity
import dev.mmrlx.webui.overlay.OverlayService

class WebUIOverlay : OverlayService() {
    override val activity: Class<out OverlayActivity> = WebUIOverlayActivity::class.java
}
