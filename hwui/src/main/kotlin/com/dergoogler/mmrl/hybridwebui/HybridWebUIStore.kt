package com.dergoogler.mmrl.hybridwebui

import androidx.lifecycle.ViewModel
import com.dergoogler.mmrl.hybridwebui.store.JavaScriptInterfaceStore
import com.dergoogler.mmrl.hybridwebui.store.NetworkRequestStore
import com.dergoogler.mmrl.hybridwebui.store.PathMatchersStore
import com.dergoogler.mmrl.hybridwebui.store.WebConsoleStore

class HybridWebUIStore : ViewModel() {
    internal val jsInterfaceStore = JavaScriptInterfaceStore()
    val networkStore = NetworkRequestStore(maxHistory = 200)
    val consoleStore = WebConsoleStore(maxHistory = 200)
    val pathMatchers = PathMatchersStore()

    fun clear() {
        networkStore.clear()
        consoleStore.clear()
        pathMatchers.clear()
    }

    override fun onCleared() {
        clear()
        super.onCleared()
    }
}
