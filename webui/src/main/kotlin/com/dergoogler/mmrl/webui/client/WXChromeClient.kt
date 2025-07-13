package com.dergoogler.mmrl.webui.client

import android.webkit.JsPromptResult
import android.webkit.JsResult
import android.webkit.WebChromeClient
import android.webkit.WebView
import com.dergoogler.mmrl.ui.component.dialog.ConfirmData
import com.dergoogler.mmrl.ui.component.dialog.PromptData
import com.dergoogler.mmrl.ui.component.dialog.confirm
import com.dergoogler.mmrl.ui.component.dialog.prompt
import com.dergoogler.mmrl.webui.R
import com.dergoogler.mmrl.webui.util.WebUIOptions
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

open class WXChromeClient(
    private val options: WebUIOptions,
) : WebChromeClient() {
    private companion object {
        const val TAG = "WXChromeClient"
    }

    @OptIn(ExperimentalContracts::class)
    private inline fun options(
        result: JsResult,
        options: WebUIOptions,
        block: WebUIOptions.() -> Unit,
    ): Boolean {
        contract {
            callsInPlace(block, InvocationKind.AT_MOST_ONCE)
        }

        block(options)
        return true
    }

    override fun onJsAlert(
        view: WebView?,
        url: String,
        message: String,
        result: JsResult,
    ): Boolean = options(result, options) {
        context.confirm(
            confirmData = ConfirmData(
                title = context.getString(R.string.says, modId.id),
                description = message,
                onConfirm = { result.confirm() },
                onClose = { result.cancel() }
            ),
            colorScheme = colorScheme
        )
    }

    override fun onJsConfirm(
        view: WebView,
        url: String,
        message: String,
        result: JsResult,
    ): Boolean = options(result, options) {
        context.confirm(
            confirmData = ConfirmData(
                title = context.getString(R.string.says, modId.id),
                description = message,
                onConfirm = { result.confirm() },
                onClose = { result.cancel() }
            ),
            colorScheme = colorScheme
        )
    }

    override fun onJsPrompt(
        view: WebView,
        url: String?,
        message: String?,
        defaultValue: String?,
        result: JsPromptResult,
    ): Boolean = options(result, options) {
        context.prompt(
            promptData = PromptData(
                title = message ?: context.getString(R.string.says, modId.id),
                value = defaultValue ?: "",
                onConfirm = { result.confirm(it) },
                onClose = { result.cancel() }
            ),
            colorScheme = colorScheme
        )
    }
}

