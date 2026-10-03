package com.dergoogler.mmrl.wx.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.net.toUri

class BrowserUriHandler(
    private val context: Context,
    private val color: Color,
) {
    private companion object {
        const val TAG = "BrowserUriHandler"
    }

    fun open(url: String) = open(url.toUri())
    fun open(uri: Uri) {
        try {
            val colorSchemeParams =
                CustomTabColorSchemeParams
                    .Builder()
                    .setToolbarColor(color.toArgb())
                    .build()

            val customTabsIntent =
                CustomTabsIntent
                    .Builder()
                    .setDefaultColorSchemeParams(colorSchemeParams)
                    .build()

            customTabsIntent.apply {
                launchUrl(context, uri)
            }
        } catch (e: ActivityNotFoundException) {
            Log.e(TAG, "Unable to launch custom tab", e)
            context.startActivity(
                Intent(Intent.ACTION_VIEW, uri)
            )
        } catch (e: Exception) {
            throw BrickException(
                message = "Unable to open browser",
                cause = e,
                help = HelpMessage("Make sure that you have a proper **Browser (Chrome ect)** or **WebView implementation** installed")
            )
        }
    }
}
