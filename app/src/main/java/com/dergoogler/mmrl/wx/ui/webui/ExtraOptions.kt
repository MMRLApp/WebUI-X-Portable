package com.dergoogler.mmrl.wx.ui.webui

import com.dergoogler.mmrl.wx.datastore.model.WorkingMode
import com.dergoogler.mmrl.wx.model.module.Module
import dev.mmrlx.nio.SuFile
import dev.mmrlx.webui.JavaScriptInterface
import dev.mmrlx.webui.WebUI
import dev.mmrlx.webui.WebUISettings
import dev.mmrlx.webui.extra

val WebUI.module: Module
    get() = settings.extra<Module>("module") ?: throw IllegalStateException("Module not set")

val WebUISettings.enableErudaConsole: Boolean
    get() = debug && extra<Boolean>("enableEruda", false)

val WebUISettings.autoOpenEruda: Boolean
    get() = enableErudaConsole && extra<Boolean>("autoOpenEruda", false)

val WebUISettings.disableGlobalExitConfirm: Boolean
    get() = extra<Boolean>("disableGlobalExitConfirm", false)

val WebUISettings.forceKillWebUIProcess: Boolean
    get() = extra<Boolean>("forceKillWebUIProcess", true)

val WebUISettings.isRootMode: Boolean
    get() = extra<Boolean>("isRootMode", false)

val WebUISettings.workingMode: WorkingMode
    get() = extra<WorkingMode>("workingMode", WorkingMode.MODE_NON_ROOT)

fun WebUI.sufile(vararg paths: Any): SuFile {
    val f = file(*paths)

    if (f !is SuFile) {
        throw IllegalArgumentException("Provided file factory is not a `dev.mmrlx.nio.SuFile`")
    }

    return f
}

fun JavaScriptInterface.deprecated(method: String, replaceWith: String? = null) {
    console.warn(
        "[DEPRECATED] The `$method` method will be removed in future versions.${if (replaceWith != null) " Use `$replaceWith` instead." else ""}",
    )
}

val Module.sanitizedId: String
    get() {
        return id.replace(Regex("[^a-zA-Z0-9_]"), "_")
    }

val Module.sanitizedIdWithFile
    get(): String {
        return "$${
            when {
                sanitizedId.length >= 2 -> sanitizedId[0].uppercase() + sanitizedId[1]
                sanitizedId.isNotEmpty() -> sanitizedId[0].uppercase()
                else -> ""
            }
        }File"
    }

val Module.sanitizedIdWithFileInputStream get(): String = "${sanitizedIdWithFile}InputStream"
val Module.sanitizedIdWithFileOutputStream get(): String = "${sanitizedIdWithFile}OutputStream"

fun <R> JavaScriptInterface.runTry(
    message: String = "Unknown Error",
    default: R,
    block: () -> R,
): R = try {
    block()
} catch (e: Throwable) {
    console.error(message, e)
    default
}

fun <R> JavaScriptInterface.runTry(
    message: String = "Unknown Error",
    block: () -> R,
): R? = runTry(message, null, block)

fun <R, T> JavaScriptInterface.runTryJsWith(
    with: T,
    message: String = "Unknown Error",
    block: T.() -> R,
): R? = runTryJsWith(with, message, null, block)

fun <R, T> JavaScriptInterface.runTryJsWith(
    with: T,
    message: String = "Unknown Error",
    default: R,
    block: T.() -> R,
): R {
    return try {
        with(with, block)
    } catch (e: Throwable) {
        console.error(message, e)
        return default
    }
}

val Array<out Any>.append: Boolean
    get() {
        val p = this[1]

        if (p is Boolean) {
            return p
        }

        return false
    }