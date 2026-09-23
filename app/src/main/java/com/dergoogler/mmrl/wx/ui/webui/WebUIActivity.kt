package com.dergoogler.mmrl.wx.ui.webui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import com.dergoogler.mmrl.ext.exception.BrickException
import com.dergoogler.mmrl.wx.ui.component.ModuleScope
import com.dergoogler.mmrl.wx.util.BaseActivity
import com.dergoogler.mmrl.wx.util.setBaseContent
import com.dergoogler.mmrl.wx.util.setMyCrashHandler
import dagger.hilt.android.AndroidEntryPoint
import dev.mmrlx.compose.webui.WebUIRecomposer

@AndroidEntryPoint
class WebUIActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        setMyCrashHandler()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val moduleId = intent.getStringExtra("MODULE_ID")
            ?: throw BrickException("moduleId cannot be null or empty")

        setBaseContent {
            ModuleScope(moduleId, toolbar = false) {
                WebUIRecomposer {
                    WebUIScreen()
                }
            }
        }
    }

    companion object {
        fun start(context: Context, moduleId: String) {
            try {
                val intent = Intent(
                    context,
                    WebUIActivity::class.java
                )
                    .apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                        putExtra("MODULE_ID", moduleId)
                    }

                context.startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, e.toString(), Toast.LENGTH_LONG).show()
            }
        }
    }
}
