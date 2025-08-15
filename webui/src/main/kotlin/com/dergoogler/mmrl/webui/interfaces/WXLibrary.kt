package com.dergoogler.mmrl.webui.interfaces

import com.sun.jna.Library
import com.sun.jna.Native

interface WXLibrary : Library {
    fun <T : WXLibrary> load(
        name: String,
        interfaceClass: Class<T>,
        options: Map<String, String> = emptyMap(),
    ): T = Native.load(name, interfaceClass, options)

    fun <T : WXLibrary> unregister(
        interfaceClass: Class<T>,
    ) = Native.unregister(interfaceClass)

    companion object {
        fun Library.toWXLibrary(): WXLibrary = this as WXLibrary
        fun WXLibrary.toLibrary(): Library = this as Library
    }
}