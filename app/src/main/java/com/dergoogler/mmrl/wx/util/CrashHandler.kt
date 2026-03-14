package com.dergoogler.mmrl.wx.util

import android.content.Intent
import androidx.activity.ComponentActivity
import com.dergoogler.mmrl.ext.exception.BrickException
import com.dergoogler.mmrl.wx.R
import com.dergoogler.mmrl.wx.ui.activity.CrashHandlerActivity
import kotlin.system.exitProcess

fun ComponentActivity.setMyCrashHandler() {
    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
        throwable.printStackTrace()
        startCrashActivity(thread, throwable)
    }
}

private fun ComponentActivity.startCrashActivity(
    thread: Thread,
    throwable: Throwable,
) {
    val intent =
        Intent(this, CrashHandlerActivity::class.java).apply {
            putExtra("message", throwable.message)
            if (throwable is BrickException) {
                putExtra("helpMessage", throwable.helpMessage)
            }
            putExtra("stacktrace", formatStackTrace(throwable))
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
    startActivity(intent)
    finish()

    exitProcess(0)
}

private fun ComponentActivity.formatStackTrace(
    throwable: Throwable,
    numberOfLines: Int = 88,
): String {
    val stackTrace = throwable.stackTrace
    val stackTraceElements = stackTrace.joinToString("\n") { it.toString() }

    return if (stackTrace.size > numberOfLines) {
        val trimmedStackTrace =
            stackTraceElements.lines().take(numberOfLines).joinToString("\n")
        val moreCount = stackTrace.size - numberOfLines

        getString(R.string.stack_trace_truncated, trimmedStackTrace, moreCount)
    } else {
        stackTraceElements
    }
}