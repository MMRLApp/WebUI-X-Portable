package com.dergoogler.mmrl.wx.ui.webui.interfaces.legacy

import android.os.Build
import androidx.core.app.ShareCompat
import androidx.core.content.pm.PackageInfoCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.dergoogler.mmrl.wx.ui.webui.deprecated
import com.dergoogler.mmrl.wx.ui.webui.module
import com.dergoogler.mmrl.wx.ui.webui.sanitizedId
import com.dergoogler.mmrl.wx.ui.webui.workingMode
import com.squareup.moshi.JsonClass
import dev.mmrlx.utilities.json.jsonObject
import dev.mmrlx.webui.JavaScriptRegistry

@JsonClass(generateAdapter = true)
internal data class Manager(
    val name: String,
    val versionName: String,
    val versionCode: Int,
)

/** Legacy `window.$<moduleId>` interface. */
fun JavaScriptRegistry.legacyModuleInterface() {
    val name = "$${module.sanitizedId}"

    namespace(name) {
        fun controller() = WindowCompat.getInsetsController(activity.window, webview)

        controller().systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        function("getManager") {
            deprecated("$name.getManager()", "webui.getCurrentRootManager()")
            jsonObject {
                "name" to settings.workingMode.toString
                "versionName" to "-1"
                "versionCode" to -1
            }
        }

        function("getMmrl") {
            deprecated("$name.getMmrl()", "webui.getCurrentApplication()")
            val packageInfo = kontext.packageManager.getPackageInfo(kontext.packageName, 0)
            jsonObject {
                "name" to packageInfo.packageName
                "versionName" to (packageInfo.versionName ?: "unknown")
                "versionCode" to PackageInfoCompat.getLongVersionCode(packageInfo)
            }
        }

        listOf("Top", "Bottom", "Left", "Right").forEach { side ->
            function("getWindow${side}Inset") {
                deprecated(
                    "$name.getWindow${side}Inset()",
                    "window.getComputedStyle(document.body).getPropertyValue('--window-inset-${side.lowercase()}')"
                )
                0
            }
        }

        function("createShortcut") {
            deprecated("$name.createShortcut()", "webui.createShortcut()")
            module.createShortcut(true)
        }

        function("hasShortcut") {
            deprecated("$name.hasShortcut()", "webui.hasShortcut")
            module.hasShortcut()
        }

        function("isLightNavigationBars") { controller().isAppearanceLightNavigationBars }
        function("isLightStatusBars") { controller().isAppearanceLightStatusBars }
        function("isDarkMode") { settings.darkMode }
        function("getSdk") { Build.VERSION.SDK_INT }

        function("setLightNavigationBars") {
            val light = checkBoolean(0)
            webview.post { controller().isAppearanceLightNavigationBars = light }
        }

        function("setLightStatusBars") {
            val light = checkBoolean(0)
            webview.post { controller().isAppearanceLightStatusBars = light }
        }

        function("shareText") {
            ShareCompat.IntentBuilder(kontext)
                .setType(optString(1, "text/plain"))
                .setText(checkString(0))
                .startChooser()
        }
    }
}