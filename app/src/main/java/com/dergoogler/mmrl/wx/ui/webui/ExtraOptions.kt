package com.dergoogler.mmrl.wx.ui.webui

import android.os.IBinder
import android.view.View
import android.view.ViewGroup.LayoutParams
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalView
import com.dergoogler.mmrl.wx.datastore.model.UserPreferences
import com.dergoogler.mmrl.wx.datastore.model.WorkingMode
import com.dergoogler.mmrl.wx.model.module.Module
import dev.mmrlx.compose.webui.drawCompose
import dev.mmrlx.nio.SuFile
import dev.mmrlx.nio.SuRandomAccessFile
import dev.mmrlx.webui.JavaScriptFunctionScope
import dev.mmrlx.webui.JavaScriptScope
import dev.mmrlx.webui.WebUI
import dev.mmrlx.webui.WebUIFactories
import dev.mmrlx.webui.WebUISettings
import dev.mmrlx.webui.extra
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

val WebUI.preferences: UserPreferences
    get() = settings.preferences

val WebUISettings.preferences: UserPreferences
    get() = extra<UserPreferences>("preferences")
        ?: throw IllegalStateException("Preferences not set")

val WebUI.module: Module
    get() = settings.extra<Module>("module") ?: throw IllegalStateException("Module not set")

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

val WebUI.mdColorScheme: ColorScheme
    get() = settings.extra<ColorScheme>("mdColorScheme")
        ?: throw IllegalStateException("MD Color Scheme not set")

private val printedDeprecations = mutableSetOf<String>()

fun JavaScriptFunctionScope.deprecated(
    method: String,
    replaceWith: String? = null,
    once: Boolean = false,
) {
    if (once && !printedDeprecations.add(method)) return

    console.warn(
        "[DEPRECATED] The `$method` method will be removed in future versions.${if (replaceWith != null) " Use `$replaceWith` instead." else ""}",
    )
}

fun JavaScriptScope.deprecatedFunction(
    name: String,
    replaceWith: String? = null,
    once: Boolean = true,
    block: JavaScriptFunctionScope.() -> Any?,
) = function(name) {
    deprecated(name, replaceWith, once)
    block()
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

fun <R> JavaScriptFunctionScope.runTry(
    message: String = "Unknown Error",
    default: R,
    block: () -> R,
): R = try {
    block()
} catch (e: Throwable) {
    console.error(message, e)
    default
}

fun <R> JavaScriptFunctionScope.runTry(
    message: String = "Unknown Error",
    block: () -> R,
): R? = runTry(message, null, block)

fun <R, T> JavaScriptFunctionScope.runTryJsWith(
    with: T,
    message: String = "Unknown Error",
    block: T.() -> R,
): R? = runTryJsWith(with, message, null, block)

fun <R, T> JavaScriptFunctionScope.runTryJsWith(
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

inline operator fun <reified T> Array<out Any>.get(
    index: Int,
    default: T,
): T {
    if (index !in indices) {
        return default
    }

    val value = this[index]

    return if (value is T) {
        value
    } else {
        default
    }
}

fun WebUIFactories.randomAccessFileFactory(factory: WebUIFactories.Factory<SuRandomAccessFile>) {
    addFactory("randomAccessFile", factory)
}

fun WebUI.randomAccessFile(path: String, mode: String): SuRandomAccessFile {
    return getFactory<SuRandomAccessFile>("randomAccessFile")?.create(path, mode)
        ?: throw IllegalStateException("Random access file factory not set")
}

suspend fun WebUI.asyncOverlayLayout(
    layoutParams: LayoutParams = LayoutParams(
        LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT
    ),
    content: @Composable () -> Unit,
) = withContext(Dispatchers.Main) {
    drawCompose(layoutParams = layoutParams, content = content)
}

fun WebUI.syncOverlayLayout(
    layoutParams: LayoutParams = LayoutParams(
        LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT
    ),
    content: @Composable () -> Unit,
) = webview.post {
    drawCompose(layoutParams = layoutParams, content = content)
}

@Composable
fun rememberWindowToken(): IBinder? {
    val view = LocalView.current
    var windowToken by remember(view) { mutableStateOf(view.windowToken) }

    DisposableEffect(view) {
        if (view.isAttachedToWindow) {
            windowToken = view.windowToken
        }

        val listener = object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {
                windowToken = v.windowToken
            }

            override fun onViewDetachedFromWindow(v: View) {
                windowToken = null
            }
        }

        view.addOnAttachStateChangeListener(listener)
        onDispose {
            view.removeOnAttachStateChangeListener(listener)
        }
    }

    return windowToken
}