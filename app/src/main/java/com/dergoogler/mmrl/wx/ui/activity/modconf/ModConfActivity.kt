package com.dergoogler.mmrl.wx.ui.activity.modconf

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import androidx.lifecycle.lifecycleScope
import com.dergoogler.mmrl.ext.exception.BrickException
import com.dergoogler.mmrl.modconf.Kontext
import com.dergoogler.mmrl.modconf.ModConfView
import com.dergoogler.mmrl.platform.model.ModId.Companion.getModId
import com.dergoogler.mmrl.ui.component.dialog.ConfirmData
import com.dergoogler.mmrl.ui.component.dialog.confirm
import com.dergoogler.mmrl.webui.activity.WXActivity.Companion.createLoadingRenderer
import com.dergoogler.mmrl.wx.util.BaseActivity
import com.dergoogler.mmrl.wx.util.initPlatform
import com.dergoogler.mmrl.wx.util.setBaseContent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@SuppressLint("SetJavaScriptEnabled")
@AndroidEntryPoint
class ModConfActivity : BaseActivity() {
    val modId get() = intent.getModId() ?: throw BrickException("Invalid Module ID")
    val userPrefs get() = runBlocking { userPreferencesRepository.data.first() }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Enable edge to edge
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        super.onCreate(savedInstanceState)

        val colorScheme = userPrefs.colorScheme(this)
        val loading = createLoadingRenderer(colorScheme)
        setContentView(loading)

        lifecycleScope.launch {
            val ready = initPlatform(
                context = this@ModConfActivity,
                platform = userPrefs.workingMode.toPlatform(),
                scope = this
            )

            if (ready.await()) {
                setBaseContent {
                    val kontext = remember { Kontext(baseContext, modId) }
                    ModConfView(kontext)
                }
                return@launch
            }

            confirm(
                ConfirmData(
                    title = "Failed!",
                    description = "Failed to initialize platform. Please try again.",
                    confirmText = "Close",
                    onConfirm = {
                        finish()
                    },
                ),
                colorScheme = colorScheme
            )
        }

    }
}