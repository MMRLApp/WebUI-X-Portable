package com.dergoogler.mmrl.webui.interfaces

import com.sun.jna.Callback

interface WXCallback : Callback {
    companion object {
        fun Callback.toWXCallback(): WXCallback = this as WXCallback
        fun WXCallback.toCallback(): Callback = this as Callback
    }
}