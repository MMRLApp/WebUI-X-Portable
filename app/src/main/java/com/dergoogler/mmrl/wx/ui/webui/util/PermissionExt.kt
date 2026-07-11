package com.dergoogler.mmrl.wx.ui.webui.util

import com.dergoogler.mmrl.wx.model.module.permissions
import com.dergoogler.mmrl.wx.ui.webui.alerts.Md3Confirm
import com.dergoogler.mmrl.wx.ui.webui.mdColorScheme
import com.dergoogler.mmrl.wx.ui.webui.module
import dev.mmrlx.compose.layout.addOverlayView
import dev.mmrlx.webui.PureJavaScriptInterface

private val requestedPermissions = mutableSetOf<String>()


fun <T> PureJavaScriptInterface.requirePermission(
    name: Permissions.Group,
    method: String? = null,
    block: () -> T,
): T? = requirePermission(
    name,
    method,
    null,
    block,
)

fun <T> PureJavaScriptInterface.requirePermission(
    name: Permissions.Name,
    method: String? = null,
    block: () -> T,
): T? = requirePermission(
    Permissions.Group(setOf(name)),
    method,
    null,
    block,
)

fun <T> PureJavaScriptInterface.requirePermission(
    group: Permissions.Group,
    method: String? = null,
    default: T,
    block: () -> T,
): T {
    val permissions = module.webrootConfig.permissions.toMutableList()

    // Already granted?
    group.permissions.firstOrNull {
        permissions.contains(it.permissionName)
    }?.let {
        return block()
    }

    // Prevent showing the same permission request twice.
    val requestKey = group.permissions
        .map { it.permissionName }
        .sorted()
        .joinToString("|")

    if (!requestedPermissions.add(requestKey)) {
        return default
    }

    val permissionNames = group.permissions.joinToString(" or ") {
        "\"${it.permissionName}\""
    }

    val message = buildString {
        append("${module.name} requires the $permissionNames permission to access this feature. ")
        append("If you deny this request, some WebUI functionality may not work as expected. ")
        append("After confirming, the WebUI will be refreshed.")

        if (method != null) {
            append("\n\nThis request was called by $id.$method(...)")
        }
    }

    mainThread {
        activity.addOverlayView {
            Md3Confirm(
                title = "Missing permission",
                description = message,
                onConfirm = {
                    // Grant every permission in the group.
                    group.permissions.forEach {
                        if (it.permissionName !in permissions) {
                            permissions += it.permissionName
                        }
                    }

                    module.webrootConfig.set(
                        "permissions",
                        permissions
                    )

                    reload()
                },
                onClose = {},
                colorScheme = mdColorScheme,
                confirmText = "Allow",
                cancelText = "Reject",
            )
        }
    }

    return default
}