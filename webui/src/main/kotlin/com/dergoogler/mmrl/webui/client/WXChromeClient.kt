package com.dergoogler.mmrl.webui.client

import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.JsPromptResult
import android.webkit.JsResult
import android.webkit.PermissionRequest
import android.webkit.WebView
import dev.mmrlx.hybridwebui.HybridWebUI
import dev.mmrlx.hybridwebui.HybridWebUIChromeClient
import com.dergoogler.mmrl.ui.component.dialog.ConfirmData
import com.dergoogler.mmrl.ui.component.dialog.PromptData
import com.dergoogler.mmrl.ui.component.dialog.confirm
import com.dergoogler.mmrl.ui.component.dialog.prompt
import com.dergoogler.mmrl.webui.R
import com.dergoogler.mmrl.webui.util.WebUIOptions

open class WXChromeClient(
    view: HybridWebUI,
    private val options: WebUIOptions,
) : HybridWebUIChromeClient(view) {
    companion object {
        private const val TAG = "WXChromeClient"
    }

    override fun onJsAlert(
        view: WebView,
        url: String,
        message: String?,
        result: JsResult,
    ): Boolean = options {
        context.confirm(
            confirmData = ConfirmData(
                title = context.getString(R.string.says, modId.id),
                description = message ?: "(empty)",
                onConfirm = { result.confirm() },
                onClose = { result.cancel() }
            ),
            colorScheme = colorScheme
        )

        true
    }

    override fun onJsConfirm(
        view: WebView,
        url: String,
        message: String?,
        result: JsResult,
    ): Boolean = options {
        context.confirm(
            confirmData = ConfirmData(
                title = context.getString(R.string.says, modId.id),
                description = message ?: "(empty)",
                onConfirm = { result.confirm() },
                onClose = { result.cancel() }
            ),
            colorScheme = colorScheme
        )

        true
    }

    override fun onJsPrompt(
        view: WebView,
        url: String,
        message: String?,
        defaultValue: String?,
        result: JsPromptResult,
    ): Boolean = options {
        context.prompt(
            promptData = PromptData(
                title = message ?: context.getString(R.string.says, modId.id),
                value = defaultValue ?: "",
                onConfirm = { result.confirm(it) },
                onClose = { result.cancel() }
            ),
            colorScheme = colorScheme
        )

        true
    }

    override fun onPermissionRequest(request: PermissionRequest) = options {
        val perms = config.permissions
        val filteredPermissions = perms.mapNotNull {
            if (Regex("^android\\.webkit\\.resource\\.([A-Z_]+)$").matches(it)) {
                return@mapNotNull it
            } else null
        }.toTypedArray()

        if (filteredPermissions.isEmpty()) return@options

        val p = filteredPermissions.joinToString("") { "\n- $it" }

        context.confirm(
            confirmData = ConfirmData(
                title = context.getString(R.string.says, modId.id),
                description = "Requesting permissions: $p",
                onConfirm = { request.grant(filteredPermissions) },
                onClose = { request.deny() }
            ),
            colorScheme = colorScheme
        )
    }
}

