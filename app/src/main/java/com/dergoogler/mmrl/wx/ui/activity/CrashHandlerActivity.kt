package com.dergoogler.mmrl.wx.ui.activity

import android.os.Build
import android.os.Bundle
import com.dergoogler.mmrl.wx.ui.screens.crash.CrashHandlerScreen
import com.dergoogler.mmrl.wx.util.BaseActivity
import com.dergoogler.mmrl.wx.util.HelpMessage
import com.dergoogler.mmrl.wx.util.setBaseContent
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CrashHandlerActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val message = intent.getStringExtra("message") ?: "Unknown Message"
        val stacktrace = intent.getStringExtra("stacktrace") ?: "Unknown Stacktrace"

        val help =
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getSerializableExtra<HelpMessage?>("help", HelpMessage::class.java)
                } else {
                    intent.getSerializableExtra("help") as HelpMessage
                }
            } catch (_: Exception) {
                null
            }

        setBaseContent {
            CrashHandlerScreen(
                message = message,
                stacktrace = stacktrace,
                help = help,
            )
        }
    }
}