package com.dergoogler.mmrl.webui.interfaces

import android.app.Activity
import android.os.Build
import android.webkit.JavascriptInterface
import androidx.annotation.Keep
import androidx.core.app.ShareCompat
import androidx.core.content.pm.PackageInfoCompat
import androidx.core.view.ViewCompat.getWindowInsetsController
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.dergoogler.mmrl.ext.findActivity
import com.dergoogler.mmrl.platform.Platform
import com.dergoogler.mmrl.platform.PlatformManager
import com.dergoogler.mmrl.webui.moshi
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class Manager(
    val name: String,
    val versionName: String,
    val versionCode: Int,
)

@Keep
class ModuleInterface(
    wxOptions: WXOptions,
) : WXInterface(wxOptions) {
    override var name: String = "$${modId.sanitizedId}"
    override var tag: String = "ModuleInterface"

/*    private fun getWindowInsetsController(activity: Activity): WindowInsetsControllerCompat =
        WindowCompat.getInsetsController(
            activity.window,
            webView
        )*/

    init {
      /*  with(activity) {
            getWindowInsetsController(this).systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }*/
    }

    private var managerAdapter: JsonAdapter<Manager> = moshi.adapter(Manager::class.java)

    @get:JavascriptInterface
    val manager: String
        get() {
            deprecated("$name.getManager()", "webui.getCurrentRootManager()")

            return managerAdapter.toJson(
                Manager(
                    name = PlatformManager.platform.name,
                    versionName = PlatformManager.moduleManager.version,
                    versionCode = PlatformManager.moduleManager.versionCode
                )
            )
        }

    @get:JavascriptInterface
    val mmrl: String
        get() {
            deprecated("$name.getMmrl()", "webui.getCurrentApplication()")

            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val versionCode = PackageInfoCompat.getLongVersionCode(packageInfo)
            val versionName = packageInfo.versionName ?: "unknown"

            return managerAdapter.toJson(
                Manager(
                    name = packageInfo.packageName,
                    versionName = versionName,
                    versionCode = versionCode.toInt()
                )
            )
        }

    @get:JavascriptInterface
    val hasAccessToFileSystem: Boolean
        get() {
            deprecated("$name.getHasAccessToFileSystem()")
            return true
        }

    @get:JavascriptInterface
    val hasAccessToAdvancedKernelSuAPI: Boolean
        get() {
            deprecated("$name.getHasAccessToAdvancedKernelSuAPI()")
            return true
        }

    @Deprecated("Use window.getComputedStyle(document.body).getPropertyValue('--window-inset-top') instead")
    @get:JavascriptInterface
    val windowTopInset: Int
        get() {
            deprecated(
                "$name.getWindowTopInset()",
                "window.getComputedStyle(document.body).getPropertyValue('--window-inset-top')"
            )
            return -1
        }

    @Deprecated("Use window.getComputedStyle(document.body).getPropertyValue('--window-inset-bottom') instead")
    @get:JavascriptInterface
    val windowBottomInset: Int
        get() {
            deprecated(
                "$name.getWindowBottomInset()",
                "window.getComputedStyle(document.body).getPropertyValue('--window-inset-bottom')"
            )
            return -1
        }

    @Deprecated("Use window.getComputedStyle(document.body).getPropertyValue('--window-inset-left') instead")
    @get:JavascriptInterface
    val windowLeftInset: Int
        get() {
            deprecated(
                "$name.getWindowLeftInset()",
                "window.getComputedStyle(document.body).getPropertyValue('--window-inset-left')"
            )
            return -1
        }

    @Deprecated("Use window.getComputedStyle(document.body).getPropertyValue('--window-inset-right') instead")
    @get:JavascriptInterface
    val windowRightInset: Int
        get() {
            deprecated(
                "$name.getWindowRightInset()",
                "window.getComputedStyle(document.body).getPropertyValue('--window-inset-right')"
            )
            return -1
        }

    @get:JavascriptInterface
    val isLightNavigationBars: Boolean?
        get() = false /*withActivity<Boolean> {
            getWindowInsetsController(this).isAppearanceLightNavigationBars
        }*/

    @get:JavascriptInterface
    val isDarkMode: Boolean
        get() = options.isDarkMode

    @JavascriptInterface
    fun setLightNavigationBars(isLight: Boolean) = post {
      /*  withActivity {
            getWindowInsetsController(this).isAppearanceLightNavigationBars = isLight
        }*/
    }

    @get:JavascriptInterface
    val isLightStatusBars: Boolean?
        get() =false /* withActivity {
            getWindowInsetsController(this).isAppearanceLightStatusBars
        }*/

    @JavascriptInterface
    fun setLightStatusBars(isLight: Boolean) = post {
       /* withActivity {
            getWindowInsetsController(this).isAppearanceLightStatusBars = isLight
        }*/
    }

    @get:JavascriptInterface
    val sdk: Int get() = Build.VERSION.SDK_INT

    @JavascriptInterface
    fun shareText(text: String) {
        ShareCompat.IntentBuilder(context)
            .setType("text/plain")
            .setText(text)
            .startChooser()
    }

    @JavascriptInterface
    fun shareText(text: String, type: String) {
        ShareCompat.IntentBuilder(context)
            .setType(type)
            .setText(text)
            .startChooser()
    }

    @get:JavascriptInterface
    val recomposeCount get() = options.recomposeCount

    @JavascriptInterface
    fun recompose() = options.recomposeCount++

    @JavascriptInterface
    fun requestAdvancedKernelSUAPI() {
        deprecated("$name.requestAdvancedKernelSUAPI()")
    }

    @JavascriptInterface
    fun requestFileSystemAPI() {
        deprecated("$name.requestFileSystemAPI()")
    }

    @JavascriptInterface
    fun createShortcut(title: String?, icon: String?) {
        deprecated("$name.createShortcut($title, $icon)", "Use $name.createShortcut() instead")
    }

    @JavascriptInterface
    fun createShortcut() {
        if (options.cls == null) {
            console.error(Exception("No class were defined for shortcuts"))
            return
        }

        config.createShortcut(context, options.cls)
    }


    @JavascriptInterface
    fun hasShortcut(): Boolean = config.hasWebUIShortcut(context)
}