package com.dergoogler.mmrl.webui.interfaces

import com.sun.jna.Library

interface WXLibrary : Library {
    companion object {
        fun Library.toWXLibrary(): WXLibrary = this as WXLibrary
        fun WXLibrary.toLibrary(): Library = this as Library
    }
}