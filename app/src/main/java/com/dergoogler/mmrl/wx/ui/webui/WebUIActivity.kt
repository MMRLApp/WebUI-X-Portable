package com.dergoogler.mmrl.wx.ui.webui

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.ExperimentalComposeApi
import com.dergoogler.mmrl.ext.exception.BrickException
import com.dergoogler.mmrl.wx.ui.component.ModuleScope
import com.dergoogler.mmrl.wx.util.BaseActivity
import com.dergoogler.mmrl.wx.util.setBaseContent
import com.dergoogler.mmrl.wx.util.setMyCrashHandler
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WebUIActivity : BaseActivity() {
    @OptIn(ExperimentalComposeApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        setMyCrashHandler()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val moduleId = intent.getStringExtra("MODULE_ID")
            ?: throw BrickException("moduleId cannot be null or empty")

        setBaseContent {
            ModuleScope(moduleId, toolbar = false) {
                WebUIScreen()
            }
        }
    }
}