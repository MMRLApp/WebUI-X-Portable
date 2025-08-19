package com.dergoogler.mmrl.webui.activity

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.os.Process
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.annotation.CallSuper
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.lifecycleScope
import com.dergoogler.mmrl.compat.BuildCompat
import com.dergoogler.mmrl.ext.nullply
import com.dergoogler.mmrl.platform.model.ModId
import com.dergoogler.mmrl.platform.model.ModId.Companion.getModId
import com.dergoogler.mmrl.platform.model.ModId.Companion.putBaseDir
import com.dergoogler.mmrl.platform.model.ModId.Companion.putModId
import com.dergoogler.mmrl.ui.component.dialog.ConfirmData
import com.dergoogler.mmrl.ui.component.dialog.confirm
import com.dergoogler.mmrl.webui.R
import com.dergoogler.mmrl.webui.model.WXEvent
import com.dergoogler.mmrl.webui.model.WXEventHandler
import com.dergoogler.mmrl.webui.model.WXKeyboardEventData
import com.dergoogler.mmrl.webui.model.WebUIConfig
import com.dergoogler.mmrl.webui.model.WebUIConfig.Companion.asWebUIConfig
import com.dergoogler.mmrl.webui.util.WebUIOptions
import com.dergoogler.mmrl.webui.view.WXView
import com.dergoogler.mmrl.webui.view.WebUIXView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.system.exitProcess

/**
 * Base activity class for displaying web content using [WXView].
 *
 * This activity handles the basic lifecycle of a web view, including:
 * - Edge-to-edge display.
 * - Swipe-to-refresh functionality.
 * - Keyboard visibility adjustments.
 * - Back press handling with customizable behavior (native, JavaScript, or custom).
 * - Loading indicators.
 * - Configuration through [WebUIOptions] and [WebUIConfig].
 *
 * Subclasses should typically override [onRender] to set up their specific UI
 * and potentially provide custom loading renderers or back press handling.
 *
 * The activity expects a [ModId] to be passed via intent extras with the key "MOD_ID" or "id".
 * This [ModId] is used to load module-specific configurations.
 *
 * @property view The [WXView] instance used to display web content. Must be initialized.
 * @property options The [WebUIOptions] used to configure the web UI. Must be initialized.
 * @property modId Lazily initialized [ModId] from intent extras.
 */
@Suppress("OVERRIDE_DEPRECATION")
open class WXActivity : ComponentActivity() {
    private var isKeyboardShowing by mutableStateOf(false)
    private lateinit var rootView: View
    var view: WebUIXView? = null

    /**
     * Lazily initializes the [ModId] from the intent extras.
     * It attempts to find a string extra with the key "MOD_ID" or "id" and convert it to a [ModId].
     *
     * @return The [ModId] if found, otherwise `null`.
     */
    var modId: ModId? = null
        private set

    /**
     * Executes a block of code with the ModId if it exists in the intent.
     *
     * This function attempts to extract a "MOD_ID" or "id" string extra from the activity's intent.
     * If found, it converts it to a [ModId] and executes the provided [block] with the [ModId] as its receiver.
     *
     * @param R The return type of the [block].
     * @param block A lambda function that takes a [ModId] as its receiver and returns a value of type [R].
     * @return The result of executing the [block] if a [ModId] is found, otherwise `null`.
     */
    fun <R> modId(block: ModId.() -> R): R? {
        val id = modId ?: return null
        return block(id)
    }

    /**
     * Executes the given [block] with a [WebUIConfig] instance derived from the [modId].
     *
     * This function provides a convenient way to access and configure web UI settings
     * specific to the current module identified by [modId].
     *
     * @param R The return type of the [block].
     * @param block A lambda function that receives a [WebUIConfig] and returns a value of type [R].
     * @return The result of executing the [block], or `null` if [modId] is `null`.
     */
    fun <R> config(block: WebUIConfig.() -> R): R? = modId {
        return@modId block(asWebUIConfig)
    }

    /**
     * Called to render the initial UI of the activity.
     *
     * This function is invoked within a [CoroutineScope] during the `onCreate` lifecycle method.
     * Subclasses should override this method to set up their specific UI elements,
     * such as initializing and adding the [WXView] to the layout.
     *
     * The base implementation initializes `rootView` by finding the content view of the activity.
     *
     * @param scope The [CoroutineScope] in which this function is executed, typically the activity's `lifecycleScope`.
     */
    open suspend fun onRender(scope: CoroutineScope) {
        rootView = findViewById(android.R.id.content)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        modId = intent.getModId()

        lifecycleScope.launch {
            onRender(this)
            registerBackEvents()

            config {
                if (windowResize) {
                    rootView.viewTreeObserver.addOnGlobalLayoutListener {
                        val r = Rect()
                        rootView.getWindowVisibleDisplayFrame(r)

                        val screenHeight = rootView.rootView.height
                        val keypadHeight = screenHeight - r.bottom
                        val keyboardVisibleNow = keypadHeight > screenHeight * 0.15

                        if (keyboardVisibleNow != isKeyboardShowing) {
                            isKeyboardShowing = keyboardVisibleNow

                            view?.wx?.postWXEvent(
                                WXEventHandler(
                                    WXEvent.WX_ON_KEYBOARD,
                                    WXKeyboardEventData(visible = keyboardVisibleNow)
                                )
                            )

                            if (keyboardVisibleNow) {
                                adjustWebViewHeight(keypadHeight)
                            } else {
                                resetWebViewHeight()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun adjustWebViewHeight(keypadHeight: Int) {
        val params = view?.layoutParams
        params?.height = rootView.height - keypadHeight
        view?.layoutParams = params
    }

    private fun resetWebViewHeight() {
        val params = view?.layoutParams
        params?.height = LinearLayout.LayoutParams.MATCH_PARENT
        view?.layoutParams = params
    }

    private fun registerBackEvents() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val options = view?.options ?: return
                val wx = view?.wx ?: return

                val backHandler = options.config.backHandler
                val interceptor = options.config.backInterceptor

                if (backHandler != true) {
                    handleNativeBack()
                    return
                }

                when (interceptor) {
                    "native" -> handleNativeBack()
                    "javascript" -> wx.postWXEvent(
                        WXEventHandler(WXEvent.WX_ON_BACK, null)
                    )

                    true -> handleNativeBack()
                    null -> handleNativeBack()
                    false -> exit(options)
                    else -> exit(options)
                }
            }
        })
    }

    private fun handleNativeBack() {
        val options = view?.options ?: return
        val wx = view?.wx ?: return

        if (wx.canGoBack()) {
            wx.goBack()
            return
        }

        if (options.config.exitConfirm) {
            confirm(
                confirmData = ConfirmData(
                    title = getString(R.string.exit),
                    description = getString(R.string.exit_desc),
                    onConfirm = { exit(options) },
                    onClose = {}
                ),
                colorScheme = options.colorScheme
            )
            return
        }

        exit(options)
    }

    @CallSuper
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        view?.wx?.onActivityResult(requestCode, resultCode, data)
    }

    @CallSuper
    override fun onDestroy() {
        view.nullply {
            wx.onActivityDestroyInterfaces()
        }

        super.onDestroy()
    }

    @CallSuper
    override fun onResume() {
        view.nullply {
            wx.onResume()
            wx.resumeTimers()
            wx.postWXEvent(WXEvent.WX_ON_RESUME)
            wx.onActivityResumeInterfaces()
        }

        super.onResume()
    }

    @CallSuper
    override fun onPause() {
        view.nullply {
            wx.onPause()
            wx.pauseTimers()
            wx.postWXEvent(WXEvent.WX_ON_PAUSE)
            wx.onActivityPauseInterfaces()
        }

        super.onPause()
    }

    @CallSuper
    override fun onStop() {
        view.nullply {
            wx.onActivityStopInterfaces()
        }

        super.onStop()
    }

    /**
     * Sets the title of the activity in the recent apps list.
     *
     * This function updates the task description for the activity, which affects
     * how it's displayed in the Android recents screen.
     *
     * On Android T (API 33) and above, it uses the modern `ActivityManager.TaskDescription.Builder`
     * to set the label.
     * For older versions, it uses the deprecated `ActivityManager.TaskDescription` constructor.
     *
     * @param title The string to be set as the title.
     */
    protected fun setActivityTitle(title: String) {
        if (BuildCompat.atLeastT) {
            val taskDescription =
                ActivityManager.TaskDescription.Builder()
                    .setLabel(title)
                    .build()
            setTaskDescription(taskDescription)
        } else {
            @Suppress("DEPRECATION")
            setTaskDescription(ActivityManager.TaskDescription(title))
        }
    }

    companion object {
        private const val TAG = "WXActivity"

        /**
         * Launches a [WXActivity] of the specified type [T].
         *
         * This extension function simplifies the process of starting a [WXActivity].
         * It creates an [Intent] for the given [WXActivity] subclass [T],
         * adds the `FLAG_ACTIVITY_NEW_DOCUMENT` and `FLAG_ACTIVITY_MULTIPLE_TASK` flags,
         * puts the provided [modId] and [baseDir] into the intent extras, and optionally allows
         * further configuration of the [Intent] via the [intentConfig] lambda.
         *
         * @param T The specific subclass of [WXActivity] to launch.
         * @param modId The [ModId] to be passed to the activity.
         * @param baseDir The base directory for the module, defaults to [ModId.ADB_DIR].
         * @param intentConfig An optional lambda to configure the [Intent] before starting the activity.
         */
        inline fun <reified T : WXActivity> Context.launchWebUIX(
            modId: ModId,
            baseDir: String = ModId.ADB_DIR,
            noinline intentConfig: (Intent.() -> Unit)? = null,
        ) {
            val intent = Intent(this, T::class.java)
                .apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                    putModId(modId)
                    putBaseDir(baseDir)
                    intentConfig.nullply { this() }
                }

            this.startActivity(intent)
        }

        internal fun Activity.exit(options: WebUIOptions) {
            if (!options.forceKillWebUIProcess) {
                finish()
                return
            }

            finish()
            Process.killProcess(Process.myPid())
            exitProcess(0)
        }

        /**
         * Launches a new instance of the specified [ComponentActivity] (`T`) as a Web UI.
         *
         * This function creates an [Intent] to start the activity `T`. It applies the following configurations
         * to the intent:
         * - Adds `Intent.FLAG_ACTIVITY_NEW_DOCUMENT` and `Intent.FLAG_ACTIVITY_MULTIPLE_TASK` flags.
         * - Puts the provided `id` (as a [ModId]) into the intent's extras.
         * - Allows for additional intent configuration via the optional `intentConfig` lambda.
         *
         * After configuring the intent, it starts the activity.
         *
         * @param T The type of the [ComponentActivity] to launch. Must be a subclass of [ComponentActivity].
         * @param id The identifier string for the module, which will be converted to a [ModId].
         * @param intentConfig An optional lambda function to further configure the [Intent] before launching the activity.
         *                     This lambda will be executed with the [Intent] as its receiver.
         */
        inline fun <reified T : ComponentActivity> Context.launchWebUI(
            id: String,
            noinline intentConfig: (Intent.() -> Unit)? = null,
        ) {
            val intent = Intent(this, T::class.java)
                .apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                    putModId(id)
                    intentConfig.nullply { this() }
                }

            this.startActivity(intent)
        }

        /**
         * Creates and returns a [View] to be used as a loading indicator.
         *
         * This function constructs a [FrameLayout] that fills its parent.
         * It sets the background color based on the `options.colorScheme.background`.
         * Inside the [FrameLayout], a [ProgressBar] is added and centered.
         * The indeterminate drawable of the [ProgressBar] is tinted with the `options.colorScheme.primary` color.
         *
         * @return A [View] instance representing the loading indicator.
         */
        fun ComponentActivity.createLoadingRenderer(colorScheme: ColorScheme): View =
            FrameLayout(baseContext).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setBackgroundColor(colorScheme.background.toArgb())
                addView(ProgressBar(context).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        Gravity.CENTER
                    )
                    indeterminateDrawable.setTint(colorScheme.primary.toArgb())
                })
            }
    }
}
