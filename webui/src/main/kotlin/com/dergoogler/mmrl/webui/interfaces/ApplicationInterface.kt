package com.dergoogler.mmrl.webui.interfaces

import android.app.Activity
import android.content.Intent
import android.webkit.JavascriptInterface
import androidx.annotation.Keep
import androidx.core.content.pm.PackageInfoCompat
import com.dergoogler.mmrl.compat.MediaStoreCompat.getPathForUri
import com.dergoogler.mmrl.platform.PlatformManager
import com.dergoogler.mmrl.webui.activity.WXActivity.Companion.exit
import com.dergoogler.mmrl.webui.model.App
import com.dergoogler.mmrl.webui.model.WebUIConfigAdditionalConfig.Companion.toValueMap
import com.dergoogler.mmrl.webui.moshi
import com.squareup.moshi.JsonClass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Keep
class ApplicationInterface(
    wxOptions: WXOptions,
) : WXInterface(wxOptions) {
    override var name: String = "webui"
    override var tag: String = "ApplicationInterface"

    @JavascriptInterface
    fun exit() {
        withActivity {
            exit(options)
        }
    }

    @JavascriptInterface
    fun setRefreshing(state: Boolean) {
        if (!config.pullToRefresh) {
            console.error(
                Exception("Pull-To-Refresh needs to be enable in order to use $name.setRefreshing(boolean)")
            )
            return
        }

        if (!config.useJavaScriptRefreshInterceptor) {
            console.error(
                Exception("$name.setRefreshing(boolean) is not supported with native refresh interceptor")
            )
            return
        }

        val swipeLayout = webView.mSwipeView

        if (swipeLayout == null) {
            console.error(Exception("WXSwipeRefresh not found"))
            return
        }

        post {
            swipeLayout.isRefreshing = state
        }
    }

    @get:JavascriptInterface
    val currentRootManager: App
        get() = App(
            packageName = PlatformManager.platform.name,
            versionName = PlatformManager.moduleManager.version,
            versionCode = PlatformManager.moduleManager.versionCode.toLong()
        )

    @get:JavascriptInterface
    val currentApplication: App
        get() = getApplication(context.packageName)

    @JavascriptInterface
    fun getApplication(packageName: String): App {
        val packageInfo = context.packageManager.getPackageInfo(packageName, 0)
        val versionCode = PackageInfoCompat.getLongVersionCode(packageInfo)
        val versionName = packageInfo.versionName ?: "unknown"

        return App(
            packageName = packageInfo.packageName,
            versionName = versionName,
            versionCode = versionCode
        )
    }

    @get:JavascriptInterface
    val additionalConfig: String
        get() {
            try {
                val adapter = moshi.adapter(Map::class.java)
                val valueMap = config.additionalConfig.toValueMap()
                val json: String? = adapter.toJson(valueMap)
                return json ?: "{}"
            } catch (e: Exception) {
                console.error(e)
                return "{}"
            }
        }

    @JavascriptInterface
    fun openFile(i: IntentData?) {
        if (i == null) {
            console.error(Exception("Intent is null"))
            return
        }

        scope.launch(Dispatchers.IO) {
            val chooser = Intent.createChooser(i.intent, "Select File")
            withActivity {
                startActivityForResult(chooser, PICK_FILE_REQUEST)
            }
        }
    }

    @JavascriptInterface
    fun startActivity(i: IntentData) {
        scope.launch(Dispatchers.IO) {
            withActivity {
                startActivity(i.intent)
            }
        }
    }

    @JsonClass(generateAdapter = true)
    data class OnResultData(
        val path: String?,
    )

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
    ) {
        if (requestCode != PICK_FILE_REQUEST) return

        if (resultCode == Activity.RESULT_OK) {
            val uri = data?.data
            val path = context.getPathForUri(uri ?: return)

            webView.postWXEvent(PICK_FILE_EVENT_NAME, OnResultData(path))

            return
        }

        if (resultCode == Activity.RESULT_CANCELED) {
            webView.postWXEvent(PICK_FILE_EVENT_NAME, null)

            return
        }

        webView.postWXEvent(PICK_FILE_EVENT_NAME, null)
    }

    private companion object {
        const val PICK_FILE_REQUEST: Int = 1
        const val PICK_FILE_EVENT_NAME: String = "filepicked"
        const val TAG: String = "ApplicationInterface"
    }
}