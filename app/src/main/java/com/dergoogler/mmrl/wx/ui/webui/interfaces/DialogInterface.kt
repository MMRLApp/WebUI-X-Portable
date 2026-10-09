package com.dergoogler.mmrl.wx.ui.webui.interfaces

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_WEAK
import android.hardware.biometrics.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.annotation.UiThread
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.dergoogler.mmrl.wx.ui.webui.alerts.Confirm
import com.dergoogler.mmrl.wx.ui.webui.alerts.Prompt
import com.dergoogler.mmrl.wx.ui.webui.alerts.fromString
import dev.mmrlx.compose.layout.addOverlayView
import dev.mmrlx.utilities.json.getAs
import dev.mmrlx.utilities.json.getByPathOrDefault
import dev.mmrlx.webui.JavaScriptScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.coroutines.resume

fun JavaScriptScope.dialogs() {
    asyncFunction("prompt") {
        val deferred = CompletableDeferred<String?>()
        val options = checkObject(0)

        val theme = options.getAs<String?>("theme", null)
        val title = options.getAs<String>("title", "Confirm")
        val launchKeyboard = options.getAs<Boolean>("launchKeyboard", true)
        val confirmText = options.getByPathOrDefault<String>("buttons.confirmText", "Confirm")
        val cancelText = options.getByPathOrDefault<String>("buttons.cancelText", "Cancel")
        val defaultValue = options.getAs<String>("defaultValue", "")
        val supportingText = options.getAs<String?>("supportingText", null)
        val message = options.getAs<String?>("message", null)
            ?: throw IllegalArgumentException("Message must not null")

        val keyboardType = options.getAs<String>("keyboardType", "done").let {
            KeyboardType.fromString(it)
        }
        val imeAction = options.getAs<String>("imeAction", "text").let {
            ImeAction.fromString(it)
        }

        withContext(Dispatchers.Main) {
            activity.addOverlayView {
                this@dialogs.Prompt(
                    title = title,
                    description = message,
                    value = defaultValue,
                    onConfirm = {
                        deferred.complete(it)
                    },
                    onClose = {
                        deferred.complete(null)
                    },
                    confirmText = confirmText,
                    cancelText = cancelText,
                    launchKeyboard = launchKeyboard,
                    keyboardType = keyboardType,
                    imeAction = imeAction,
                    theme = theme,
                    supportingText = supportingText
                )
            }
        }

        return@asyncFunction deferred.await()
    }

    asyncFunction("confirm") {
        val deferred = CompletableDeferred<Boolean>()
        val options = checkObject(0)

        val theme = options.getAs<String?>("theme", null)
        val title = options.getAs<String>("title", "Confirm")
        val confirmText = options.getByPathOrDefault("buttons.confirmText", "Confirm")
        val cancelText = options.getByPathOrDefault("buttons.cancelText", "Cancel")
        val message = options.getAs<String?>("message", null)
            ?: throw IllegalArgumentException("Message must not null")

        withContext(Dispatchers.Main) {
            activity.addOverlayView {
                this@dialogs.Confirm(
                    title = title,
                    description = message,
                    onConfirm = {
                        deferred.complete(true)
                    },
                    onClose = {
                        deferred.complete(false)
                    },
                    confirmText = confirmText,
                    cancelText = cancelText,
                    theme = theme
                )
            }
        }

        return@asyncFunction deferred.await()
    }

    asyncFunction("protectedConfirm") {
        val options = checkObject(0)

        val title = options.getAs<String>("title", "Confirm")
        val description = options.getAs<String?>("description", null)
            ?: throw IllegalArgumentException("Description must not null")

        return@asyncFunction withContext(Dispatchers.Main) {
            deviceConfirm(
                activity = activity,
                title = title,
                message = description
            )
        }
    }
}

@UiThread
suspend fun deviceConfirm(
    activity: ComponentActivity,
    title: String,
    message: String,
): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        deviceConfirmApi30(activity, title, message)
    } else {
        deviceConfirmLegacy(activity, title, message)
    }

// API 30+: fingerprint / face / PIN / pattern / password
@RequiresApi(Build.VERSION_CODES.R)
private suspend fun deviceConfirmApi30(
    activity: ComponentActivity,
    title: String,
    description: String,
): Boolean = suspendCancellableCoroutine { cont ->
    val authenticators = BIOMETRIC_WEAK or DEVICE_CREDENTIAL

    val manager = activity.getSystemService(BiometricManager::class.java)
    if (manager?.canAuthenticate(authenticators) != BiometricManager.BIOMETRIC_SUCCESS) {
        cont.resume(false)
        return@suspendCancellableCoroutine
    }

    val signal = CancellationSignal()
    cont.invokeOnCancellation { signal.cancel() }

    val prompt = BiometricPrompt.Builder(activity)
        .setTitle(title)
        .setDescription(description)
        .setAllowedAuthenticators(authenticators)
        .build()

    prompt.authenticate(
        signal,
        activity.mainExecutor,
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                if (cont.isActive) cont.resume(true)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                if (cont.isActive) cont.resume(false)
            }
        }
    )
}

// API < 30: PIN / pattern / password via the system credential screen
@Suppress("DEPRECATION")
private suspend fun deviceConfirmLegacy(
    activity: ComponentActivity,
    title: String,
    description: String,
): Boolean = suspendCancellableCoroutine { cont ->
    val km = activity.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
    val intent: Intent? =
        if (km.isDeviceSecure) km.createConfirmDeviceCredentialIntent(title, description) else null

    if (intent == null) {
        cont.resume(false)
        return@suspendCancellableCoroutine
    }

    lateinit var launcher: ActivityResultLauncher<Intent>
    launcher = activity.activityResultRegistry.register(
        "device_confirm_${UUID.randomUUID()}",
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        launcher.unregister()
        if (cont.isActive) cont.resume(result.resultCode == Activity.RESULT_OK)
    }

    cont.invokeOnCancellation { launcher.unregister() }
    launcher.launch(intent)
}