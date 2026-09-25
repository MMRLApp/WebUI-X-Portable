package com.dergoogler.mmrl.wx.ui.providable

import androidx.compose.runtime.staticCompositionLocalOf
import com.dergoogler.mmrl.wx.util.BrowserUriHandler

val LocalBrowser = staticCompositionLocalOf<BrowserUriHandler> {
    error("CompositionLocal BrowserUriHandler not present")
}