package com.dergoogler.mmrl.wx.ui.activity

import android.os.Bundle
import com.dergoogler.mmrl.wx.ui.screens.crash.CrashHandlerScreen
import com.dergoogler.mmrl.wx.util.BaseActivity
import com.dergoogler.mmrl.wx.util.setBaseContent
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CrashHandlerActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val message = intent.getStringExtra("message") ?: "Unknown Message"
        val stacktrace = intent.getStringExtra("stacktrace") ?: "Unknown Stacktrace"
        val helpMessage = intent.getStringExtra("helpMessage")

        setBaseContent {
            CrashHandlerScreen(
                message = message,
                stacktrace = stacktrace,
                helpMessage = helpMessage,
            )
        }
    }
}