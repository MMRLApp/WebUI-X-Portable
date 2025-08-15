package com.dergoogler.mmrl.webui.interfaces

import com.sun.jna.Library
import com.sun.jna.Native

interface WXLibrary : Library {
    companion object {
        fun Library.toWXLibrary(): WXLibrary = this as WXLibrary
        fun WXLibrary.toLibrary(): Library = this as Library

        inline fun <reified T : WXLibrary> load(
            name: String,
            options: Map<String, String> = emptyMap(),
        ): T = Native.load(name, T::class.java, options)

        inline fun <reified T : WXLibrary> unregister() = Native.unregister(T::class.java)
    }
}