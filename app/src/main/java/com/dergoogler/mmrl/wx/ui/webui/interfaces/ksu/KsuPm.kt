package com.dergoogler.mmrl.wx.ui.webui.interfaces.ksu

import android.content.pm.ApplicationInfo
import androidx.core.content.pm.PackageInfoCompat
import com.dergoogler.mmrl.wx.ui.webui.util.packages
import dev.mmrlx.webui.JavaScriptFunctionScope
import dev.mmrlx.webui.JavaScriptScope
import org.json.JSONArray
import org.json.JSONObject

internal fun JavaScriptFunctionScope.getPackagesInfo(): String {
    // accepts an array, or the old JSON string
    val packageNames = optArray(0) ?: JSONArray(checkString(0))
    val appMap = packages.associateBy { it.packageName }
    val jsonArray = JSONArray()

    for (i in 0 until packageNames.length()) {
        val pkgName = packageNames.getString(i)
        val appInfo = appMap[pkgName]
        val obj = JSONObject()

        if (appInfo != null) {
            val app = appInfo.applicationInfo
            obj.put("packageName", appInfo.packageName)
            obj.put("versionName", appInfo.versionName ?: "")
            obj.put("versionCode", PackageInfoCompat.getLongVersionCode(appInfo))
            obj.put(
                "appLabel",
                kontext.packageManager.getApplicationLabel(appInfo.applicationInfo!!)
            )
            obj.put("category", app?.category ?: JSONObject.NULL)
            obj.put("flags", app?.flags ?: JSONObject.NULL)
            obj.put(
                "isSystem",
                if (app != null) (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0 else JSONObject.NULL
            )
            obj.put(
                "isGame",
                if (app != null) app.category == ApplicationInfo.CATEGORY_GAME ||
                        (app.flags and ApplicationInfo.FLAG_IS_GAME) != 0 else JSONObject.NULL
            )
            obj.put("uid", app?.uid ?: JSONObject.NULL)
        } else {
            obj.put("packageName", pkgName)
            obj.put("error", "Package not found or inaccessible")
        }

        jsonArray.put(obj)
    }

    return jsonArray.toString()
}

internal fun JavaScriptFunctionScope.listPackages(): String {
    val type = checkString(0).lowercase()

    return JSONArray(
        packages
            .filter { appInfo ->
                val flags = appInfo.applicationInfo?.flags ?: 0
                val category = appInfo.applicationInfo?.category ?: 0

                when (type) {
                    "system" -> (flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    "user" -> (flags and ApplicationInfo.FLAG_SYSTEM) == 0
                    "game" -> category == ApplicationInfo.CATEGORY_GAME ||
                            (flags and ApplicationInfo.FLAG_IS_GAME) != 0

                    else -> true
                }
            }
            .map { it.packageName }
            .sorted()
    ).toString()
}

fun JavaScriptScope.ksuPm() {
    function("version") { 1 }
    function("listPackages") { listPackages() }
    function("getPackagesInfo") { getPackagesInfo() }
}
