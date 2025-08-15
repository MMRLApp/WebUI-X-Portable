package com.dergoogler.mmrl.webui.model

import com.dergoogler.mmrl.webui.interfaces.WXLibrary
import com.dergoogler.mmrl.webui.interfaces.WXOptions
import com.sun.jna.Native

data class SharedObject<T : WXLibrary>(
    val clazz: Class<T>,
    val initargs: Array<Any>? = null,
    val parameterTypes: Array<Class<*>>? = null,
) {
    data class Instance(
        private val inst: WXLibrary,
    ) {
        val name: String = inst.javaClass.simpleName
        val instance: WXLibrary = inst
    }

    fun createNew(wxOptions: WXOptions): Instance {
        val constructor = if (parameterTypes != null) {
            clazz.getDeclaredConstructor(WXOptions::class.java, *parameterTypes)
        } else {
            clazz.getDeclaredConstructor(WXOptions::class.java)
        }

        val newInstance = if (initargs != null) {
            constructor.newInstance(wxOptions, *initargs)
        } else {
            constructor.newInstance(wxOptions)
        }

        return Instance((newInstance as WXLibrary))
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SharedObject<*>

        if (clazz != other.clazz) return false
        if (!initargs.contentEquals(other.initargs)) return false
        if (!parameterTypes.contentEquals(other.parameterTypes)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = clazz.hashCode()
        result = 31 * result + initargs.contentHashCode()
        result = 31 * result + parameterTypes.contentHashCode()
        return result
    }

    fun load(): T? = Native.load(clazz)
}