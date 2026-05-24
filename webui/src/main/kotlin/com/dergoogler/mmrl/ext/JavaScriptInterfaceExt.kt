package com.dergoogler.mmrl.ext

import android.util.Log
import com.dergoogler.mmrl.hybridwebui.interfaces.JavaScriptInterface
import com.dergoogler.mmrl.hybridwebui.interfaces.JavaScriptInterfaceImplementation
import com.dergoogler.mmrl.webui.interfaces.WXInterface
import com.dergoogler.mmrl.webui.interfaces.WXOptions

private const val TAG = "JavaScriptInterfaceImplementation.createNewWX(...)"

fun <T : JavaScriptInterface> JavaScriptInterfaceImplementation<T>.createNewWX(wxOptions: WXOptions): JavaScriptInterfaceImplementation.Instance? {
    return runCatching {
        val types = arrayOf(WXOptions::class.java, *(parameterTypes ?: emptyArray()))
        val args = arrayOf(wxOptions, *(initArgs ?: emptyArray()))

        val constructor = clazz.getDeclaredConstructor(*types)
        val instance = constructor.newInstance(*args)

        JavaScriptInterfaceImplementation.Instance(instance as WXInterface)
    }.onFailure { e ->
        Log.e(TAG, "Failed to create WX instance for ${clazz.simpleName}", e)
    }.getOrNull()
}