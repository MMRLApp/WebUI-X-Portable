package com.dergoogler.mmrl.wx.ui.webui.interfaces.legacy

import android.app.Activity
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
import dev.mmrlx.webui.WebUI
import dev.mmrlx.webui.interfaces.ExportMethod
import dev.mmrlx.webui.interfaces.ExportVariable
import dev.mmrlx.webui.interfaces.JavaScriptInterface
import org.json.JSONObject

@JsonClass(generateAdapter = true)
internal data class Manager(
    val name: String,
    val versionName: String,
    val versionCode: Int,
)

class ModuleInterface(
    webui: WebUI,
) : JavaScriptInterface(webui) {

    override val prototypeClass = "ModuleInterface"
    override val propertyName = "$${module.sanitizedId}"

    private fun getWindowInsetsController(activity: Activity): WindowInsetsControllerCompat =
        WindowCompat.getInsetsController(
            activity.window,
            webview
        )

    init {
        with(activity) {
            getWindowInsetsController(this).systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    @ExportMethod
    fun getManager(): JSONObject {
        deprecated("$propertyName.getManager()", "webui.getCurrentRootManager()")

        return jsonObject {
            "name" to settings.workingMode.toString
            "versionName" to "-1"
            "versionCode" to -1
        }
    }

    @ExportMethod
    fun getMmrl(): JSONObject {
        deprecated("$propertyName.getMmrl()", "webui.getCurrentApplication()")

        val packageInfo = kontext.packageManager.getPackageInfo(kontext.packageName, 0)
        val versionCode = PackageInfoCompat.getLongVersionCode(packageInfo)
        val versionName = packageInfo.versionName ?: "unknown"

        return jsonObject {
            "name" to packageInfo.packageName
            "versionName" to versionName
            "versionCode" to versionCode
        }
    }

    @Deprecated("Use window.getComputedStyle(document.body).getPropertyValue('--window-inset-top') instead")
    @ExportMethod
    fun getWindowTopInset(): Int {
        deprecated(
            "$propertyName.getWindowTopInset()",
            "window.getComputedStyle(document.body).getPropertyValue('--window-inset-top')"
        )
        return 0
    }

    @Deprecated("Use window.getComputedStyle(document.body).getPropertyValue('--window-inset-bottom') instead")
    @ExportMethod
    fun getWindowBottomInset(): Int {
        deprecated(
            "$propertyName.getWindowBottomInset()",
            "window.getComputedStyle(document.body).getPropertyValue('--window-inset-bottom')"
        )
        return 0
    }

    @Deprecated("Use window.getComputedStyle(document.body).getPropertyValue('--window-inset-left') instead")
    @ExportMethod
    fun getWindowLeftInset(): Int {
        deprecated(
            "$propertyName.getWindowLeftInset()",
            "window.getComputedStyle(document.body).getPropertyValue('--window-inset-left')"
        )
        return 0
    }

    @Deprecated("Use window.getComputedStyle(document.body).getPropertyValue('--window-inset-right') instead")
    @ExportMethod
    fun getWindowRightInset(): Int {
        deprecated(
            "$propertyName.getWindowRightInset()",
            "window.getComputedStyle(document.body).getPropertyValue('--window-inset-right')"
        )
        return 0
    }

    @ExportVariable
    val isLightNavigationBars: Boolean
        get() = with(activity) {
            getWindowInsetsController(this).isAppearanceLightNavigationBars
        }

    @ExportVariable
    val isDarkMode: Boolean
        get() = settings.darkMode

    @ExportMethod
    fun setLightNavigationBars(isLight: Boolean) = webview.post {
        with(activity) {
            getWindowInsetsController(this).isAppearanceLightNavigationBars = isLight
        }
    }

    @ExportVariable
    val isLightStatusBars: Boolean
        get() = with(activity) {
            getWindowInsetsController(this).isAppearanceLightStatusBars
        }

    @ExportMethod
    fun setLightStatusBars(isLight: Boolean) = webview.post {
        with(activity) {
            getWindowInsetsController(this).isAppearanceLightStatusBars = isLight
        }
    }

    @ExportMethod
    fun getSdk(): Int = Build.VERSION.SDK_INT

    @ExportMethod
    fun shareText(text: String) {
        ShareCompat.IntentBuilder(kontext)
            .setType("text/plain")
            .setText(text)
            .startChooser()
    }

    @ExportMethod
    fun shareText(text: String, type: String) {
        ShareCompat.IntentBuilder(kontext)
            .setType(type)
            .setText(text)
            .startChooser()
    }
}