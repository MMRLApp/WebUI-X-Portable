package com.dergoogler.mmrl.webui.util.errorPages

import com.dergoogler.mmrl.webui.R
import com.dergoogler.mmrl.webui.util.WebUIOptions
import kotlinx.html.b
import kotlinx.html.button
import kotlinx.html.div
import kotlinx.html.i

fun WebUIOptions.requireNewVersionErrorPage(): String {
    val rq = requireNewAppVersion
    val supportText = rq?.supportText
    val supportLink = rq?.supportLink
    val requiredCode = rq?.requiredCode
    val appName = rq?.packageInfo?.applicationInfo?.loadLabel(packageManager).toString()

    return baseErrorPage(
        title = getString(R.string.requireNewVersion_cannot_load_webui),
        errorSvgIcon = {
            masksTheater()
        },
        description = {
            div {
                b { +modId.id }
                +" "
                +getString(R.string.requireNewVersion_require_text, appName)
                +" "
                i { +requiredCode.toString() }
            }
        },
        tryFollowing = listOf(
            getString(R.string.requireNewVersion_try_the_following_one, appName),
            getString(R.string.requireNewVersion_try_the_following_two)
        ),
        errorCode = "ERR_NEW_WX_VERSION_REQUIRED",
        extraButtons = {
            if (supportLink != null && supportText != null) {
                button(classes = "more") {
                    attributes["onclick"] = "window.open('$supportLink');"
                    +supportText
                }
            }
        }
    )
}