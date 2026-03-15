package com.dergoogler.mmrl.hybridwebui.interfaces

import android.content.Context
import android.util.Log
import com.dergoogler.mmrl.hybridwebui.HybridWebUI
import com.dergoogler.mmrl.hybridwebui.store.NetworkRequestStore
import com.dergoogler.mmrl.hybridwebui.store.WebConsoleStore

data class JavaScriptInterfaceImplementation<T : JavaScriptInterface>(
    val clazz: Class<T>,
    val initArgs: Array<Any>? = null,
    val parameterTypes: Array<Class<*>>? = null,
) {
    data class Instance(
        private val inst: JavaScriptInterface,
    ) {
        val name: String = inst.name
        val instance: JavaScriptInterface = inst
    }

    fun createNew(
        context: Context,
        view: HybridWebUI
    ): Instance? {
        return runCatching {
            val types = arrayOf(Context::class.java, HybridWebUI::class.java, *(parameterTypes ?: emptyArray()))
            val args = arrayOf(context, view, *(initArgs ?: emptyArray()))

            val constructor = clazz.getDeclaredConstructor(*types)
            val instance = constructor.newInstance(*args)

            Instance(instance as JavaScriptInterface)
        }.onFailure { e ->
            Log.e(TAG, "Failed to create JavaScriptInterface instance for ${clazz.simpleName}", e)
        }.getOrNull()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as JavaScriptInterfaceImplementation<*>

        if (clazz != other.clazz) return false
        if (!initArgs.contentEquals(other.initArgs)) return false
        if (!parameterTypes.contentEquals(other.parameterTypes)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = clazz.hashCode()
        result = 31 * result + initArgs.contentHashCode()
        result = 31 * result + parameterTypes.contentHashCode()
        return result
    }

    private companion object {
        const val TAG = "JavaScriptInterfaceImplementation"
    }
}