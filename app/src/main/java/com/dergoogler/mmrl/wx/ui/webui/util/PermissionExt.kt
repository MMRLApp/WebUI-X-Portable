package com.dergoogler.mmrl.wx.ui.webui.util

import com.dergoogler.mmrl.wx.model.module.permissions
import com.dergoogler.mmrl.wx.ui.webui.alerts.Md3Confirm
import com.dergoogler.mmrl.wx.ui.webui.mdColorScheme
import com.dergoogler.mmrl.wx.ui.webui.module
import dev.mmrlx.compose.layout.addOverlayView
import dev.mmrlx.webui.JavaScriptFunctionScope
import dev.mmrlx.webui.JavaScriptScope

private val requestedPermissions = mutableSetOf<String>()

fun JavaScriptScope.dangerousFunction(
    name: String,
    permission: Permissions.Name,
    block: JavaScriptFunctionScope.() -> Any?,
) = function(name) {
    requirePermission(permission, name) {
        block()
    }
}

fun JavaScriptScope.dangerousFunction(
    name: String,
    permission: Permissions.Group,
    block: JavaScriptFunctionScope.() -> Any?,
) = function(name) {
    requirePermission(permission, name) {
        block()
    }
}

fun JavaScriptScope.dangerousAsyncFunction(
    name: String,
    permission: Permissions.Name,
    block: JavaScriptFunctionScope.() -> Any?,
) = asyncFunction(name) {
    requirePermission(permission, name) {
        block()
    }
}

fun JavaScriptScope.dangerousAsyncFunction(
    name: String,
    permission: Permissions.Group,
    block: JavaScriptFunctionScope.() -> Any?,
) = asyncFunction(name) {
    requirePermission(permission, name) {
        block()
    }
}

fun <T> JavaScriptScope.requirePermission(
    name: Permissions.Group,
    method: String? = null,
    block: () -> T,
): T? = requirePermission(
    name,
    method,
    null,
    block,
)

fun <T> JavaScriptScope.requirePermission(
    name: Permissions.Name,
    method: String? = null,
    block: () -> T,
): T? = requirePermission(
    Permissions.Group(setOf(name)),
    method,
    null,
    block,
)

fun <T> JavaScriptScope.requirePermission(
    group: Permissions.Group,
    method: String? = null,
    default: T,
    block: () -> T,
): T = requirePermission(
    interfaceId = namespaceName,
    group = group,
    method = method,
    default = default,
    block = block,
)

private fun <T> JavaScriptScope.requirePermission(
    interfaceId: String,
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
            append("\n\nThis request was called by $interfaceId.$method")
        }
    }

    activity.runOnUiThread {
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