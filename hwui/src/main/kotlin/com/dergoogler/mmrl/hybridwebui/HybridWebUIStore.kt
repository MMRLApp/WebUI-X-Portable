package com.dergoogler.mmrl.hybridwebui

import android.content.Intent
import android.net.Uri
import android.webkit.ValueCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.ViewModel
import com.dergoogler.mmrl.hybridwebui.HybridWebUI.OnFileSaveRequest
import com.dergoogler.mmrl.hybridwebui.store.JavaScriptInterfaceStore
import com.dergoogler.mmrl.hybridwebui.store.NetworkRequestStore
import com.dergoogler.mmrl.hybridwebui.store.PathMatchersStore
import com.dergoogler.mmrl.hybridwebui.store.WebConsoleStore

class HybridWebUIStore : ViewModel() {
    internal val jsInterfaceStore = JavaScriptInterfaceStore()
    val networkStore = NetworkRequestStore(maxHistory = 200)
    val consoleStore = WebConsoleStore(maxHistory = 200)
    val pathMatchers = PathMatchersStore()

    var pendingSaveData: ByteArray? = null

    internal var onSaveFileRequest: OnFileSaveRequest? = null

    fun onSaveFileRequest(req: OnFileSaveRequest) {
        onSaveFileRequest = req
    }

    internal var fileChooserLauncher: ActivityResultLauncher<Intent>? = null
    var saveFileLauncher: ActivityResultLauncher<Intent>? = null
    internal var filePathCallback: ValueCallback<Array<Uri>>? = null

    fun clear() {
        networkStore.clear()
        consoleStore.clear()
        pathMatchers.clear()
        pendingSaveData = null
        onSaveFileRequest = null
        fileChooserLauncher = null
        saveFileLauncher = null
        filePathCallback = null
    }

    override fun onCleared() {
        clear()
        super.onCleared()
    }
}
