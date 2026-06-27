package com.dergoogler.mmrl.wx.ui.webui

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.dergoogler.mmrl.ext.exception.BrickException
import com.dergoogler.mmrl.wx.ui.component.ModuleScope
import com.dergoogler.mmrl.wx.util.BaseActivity
import com.dergoogler.mmrl.wx.util.setBaseContent
import com.dergoogler.mmrl.wx.util.setMyCrashHandler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

@AndroidEntryPoint
class WebUIActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        setMyCrashHandler()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val moduleId = intent.getStringExtra("MODULE_ID")
            ?: throw BrickException("moduleId cannot be null or empty")

        setBaseContent {
            var localKey by remember { mutableIntStateOf(0) }

            LaunchedEffect(Unit) {
                recomposeFlow.collect {
                    localKey++
                }
            }

            ModuleScope(moduleId, toolbar = false) {
                key(localKey) {
                    WebUIScreen()
                }
            }
        }
    }

    companion object {
        private val _recomposeFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        private val recomposeFlow = _recomposeFlow.asSharedFlow()

        fun recompose() {
            _recomposeFlow.tryEmit(Unit)
        }
    }
}
