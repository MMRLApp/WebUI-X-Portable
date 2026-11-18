package com.dergoogler.mmrl.wx.ui.webui.util

import com.dergoogler.mmrl.wx.model.module.permissions
import com.dergoogler.mmrl.wx.ui.webui.alerts.Md3Confirm
import com.dergoogler.mmrl.wx.ui.webui.mdColorScheme
import com.dergoogler.mmrl.wx.ui.webui.module
import com.dergoogler.mmrl.wx.ui.webui.syncOverlayLayout
import dev.mmrlx.webui.JavaScriptFunctionScope
import dev.mmrlx.webui.JavaScriptScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

private val requestedPermissions = mutableSetOf<String>()

// Dangerous functions

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

// Async versions support suspend functions.

private suspend fun <T> JavaScriptScope.requirePermissionAsync(
    permission: Permissions.Name,
    method: String? = null,
    default: T? = null,
    block: suspend () -> T,
): T? = requirePermissionAsync(
    group = Permissions.Group(setOf(permission)),
    method = method,
    default = default,
    block = block,
)

fun JavaScriptScope.dangerousAsyncFunction(
    name: String,
    permission: Permissions.Group,
    block: suspend JavaScriptFunctionScope.() -> Any?,
) = asyncFunction(name) {
    requirePermissionAsync(permission, name) {
        block()
    }
}

fun JavaScriptScope.dangerousAsyncFunction(
    name: String,
    permission: Permissions.Name,
    block: suspend JavaScriptFunctionScope.() -> Any?,
) = asyncFunction(name) {
    requirePermissionAsync(permission, name) {
        block()
    }
}

// Public permission API

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

// Synchronous permission check

private fun <T> JavaScriptScope.requirePermission(
    interfaceId: String,
    group: Permissions.Group,
    method: String? = null,
    default: T,
    block: () -> T,
): T {
    val permissions = module.webrootConfig.permissions.toMutableList()

    if (group.permissions.any {
            it.permissionName in permissions
        }) {
        return block()
    }

    val requestKey = group.requestKey()

    if (!requestedPermissions.add(requestKey)) {
        return default
    }

    showPermissionDialog(
        interfaceId = interfaceId,
        group = group,
        method = method,
    ) { }

    return default
}

// Asynchronous permission check

private suspend fun <T> JavaScriptScope.requirePermissionAsync(
    group: Permissions.Group,
    method: String? = null,
    default: T? = null,
    block: suspend () -> T,
): T? {
    val permissions = module.webrootConfig.permissions

    if (group.permissions.any {
            it.permissionName in permissions
        }) {
        return block()
    }

    val requestKey = group.requestKey()

    if (!requestedPermissions.add(requestKey)) {
        return default
    }

    val granted = suspendCancellableCoroutine { continuation ->
        continuation.invokeOnCancellation {
            requestedPermissions.remove(requestKey)
        }

        showPermissionDialog(
            interfaceId = namespaceName,
            group = group,
            method = method,
        ) { result ->
            if (continuation.isActive) {
                continuation.resume(result)
            }
        }
    }

    if (!granted) {
        return default
    }

    // Permission was granted. Execute the original function.
    return block()
}

// Permission dialog

private fun JavaScriptScope.showPermissionDialog(
    interfaceId: String,
    group: Permissions.Group,
    method: String?,
    onResult: (Boolean) -> Unit,
) {
    val requestKey = group.requestKey()

    val permissions = module.webrootConfig.permissions.toMutableList()

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

    var completed = false

    fun finish(granted: Boolean) {
        if (completed) return
        completed = true

        requestedPermissions.remove(requestKey)
        onResult(granted)
    }

    activity {
        syncOverlayLayout {
            Md3Confirm(
                title = "Missing permission",
                description = message,
                onConfirm = {
                    group.permissions.forEach {
                        if (it.permissionName !in permissions) {
                            permissions += it.permissionName
                        }
                    }

                    module.webrootConfig.set(
                        "permissions",
                        permissions
                    )

                    finish(true)
                    reload()
                },
                onClose = {
                    finish(false)
                },
                colorScheme = mdColorScheme,
                confirmText = "Allow",
                cancelText = "Reject",
            )
        }
    }
}

// Helpers

private fun Permissions.Group.requestKey(): String =
    permissions
        .map { it.permissionName }
        .sorted()
        .joinToString("|")
