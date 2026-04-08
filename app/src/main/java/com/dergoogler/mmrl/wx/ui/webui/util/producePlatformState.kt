package com.dergoogler.mmrl.wx.ui.webui.util

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import com.dergoogler.mmrl.platform.PlatformManager
import com.dergoogler.mmrl.platform.TIMEOUT_MILLIS
import com.dergoogler.mmrl.wx.datastore.providable.LocalUserPreferences
import com.dergoogler.mmrl.wx.util.initPlatform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.cancellation.CancellationException

@Composable
fun isPlatformAlive(): State<Boolean> {
    val context = LocalContext.current
    val prefs = LocalUserPreferences.current
    return produceState(false) {
        value = initPlatform(context, prefs.workingMode.toPlatform())
    }
}

/**
 * Only collects, no init
 */
@Composable
fun <T> producePlatformState(
    fallback: T,
    vararg keys: Any?,
    timeoutMillis: Long = TIMEOUT_MILLIS,
    block: suspend CoroutineScope.() -> T,
): State<T> {
    return produceState(initialValue = fallback, keys = arrayOf(*keys, PlatformManager)) {
        PlatformManager.isAliveFlow.collectLatest { isAlive ->
            if (isAlive) {
                Log.d(PlatformManager.TAG, "Platform is alive, executing block.")
                try {
                    val result = withTimeoutOrNull(timeoutMillis) {
                        block()
                    }

                    if (result != null) {
                        value = result
                    } else {
                        Log.w(PlatformManager.TAG, "Block execution timed out.")
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(PlatformManager.TAG, "Error executing block.", e)
                }
            } else {
                Log.d(PlatformManager.TAG, "Platform not alive; using fallback.")
                value = fallback
            }
        }
    }
}