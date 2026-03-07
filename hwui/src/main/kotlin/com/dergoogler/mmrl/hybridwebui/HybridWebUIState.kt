package com.dergoogler.mmrl.hybridwebui

import android.content.Intent
import android.net.Uri
import android.util.LruCache
import android.webkit.ValueCallback
import androidx.activity.result.ActivityResultLauncher
import com.dergoogler.mmrl.hybridwebui.HybridWebUI.OnFileSaveRequest
import com.dergoogler.mmrl.hybridwebui.HybridWebUI.OnInsetsEvent
import com.dergoogler.mmrl.hybridwebui.HybridWebUI.PathMatcher

object HybridWebUIState {
    val pathMatchers: MutableList<PathMatcher> = mutableListOf()
    var onInsetsEvent: OnInsetsEvent? = null
    val insetsCache = object : LruCache<String, HybridWebUIInsets>(16) {}

    var pendingSaveData: ByteArray? = null

    internal var onSaveFileRequest: OnFileSaveRequest? = null

    fun onSaveFileRequest(req: OnFileSaveRequest) {
        onSaveFileRequest = req
    }

    internal var fileChooserLauncher: ActivityResultLauncher<Intent>? = null
    var saveFileLauncher: ActivityResultLauncher<Intent>? = null
    internal var filePathCallback: ValueCallback<Array<Uri>>? = null

}