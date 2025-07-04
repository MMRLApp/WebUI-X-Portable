package com.dergoogler.mmrl.webui.activity

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toArgb
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
    private var mView: WebUIXView? = null
    private var mOptions: WebUIOptions? = null

    var view
        get() = checkNotNull(mView) {
            "mView cannot be null"
        }
        set(value) {
            mView = value
        }

    var options
        get() = checkNotNull(mOptions) {
            "mOptions cannot be null"
        }
        set(value) {
            mOptions = value
        }

    /**
     * Lazily initializes the [ModId] from the intent extras.
     * It attempts to find a string extra with the key "MOD_ID" or "id" and convert it to a [ModId].
     *
     * @return The [ModId] if found, otherwise `null`.
     */
    val modId: ModId? = intent.getModId()

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
     * Called when the activity is ready to render its content.
     *
     * This method is responsible for initializing the `rootView` by finding it in the layout.
     * Subclasses can override this method to perform additional setup before the UI is displayed.
     *
     * @param savedInstanceState If the activity is being re-initialized after
     *     previously being shut down then this Bundle contains the data it most
     *     recently supplied in [onSaveInstanceState].  **Note: Otherwise it is null.**
     */
    open fun onRender(savedInstanceState: Bundle?) {
        rootView = findViewById(android.R.id.content)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        onRender(savedInstanceState)
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

                        view.wx.postWXEvent(
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

    private fun adjustWebViewHeight(keypadHeight: Int) {
        val params = view.layoutParams
        params.height = rootView.height - keypadHeight
        view.layoutParams = params
    }

    private fun resetWebViewHeight() {
        val params = view.layoutParams
        params.height = LinearLayout.LayoutParams.MATCH_PARENT
        view.layoutParams = params
    }

    private fun registerBackEvents() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val backHandler = options.config.backHandler
                val interceptor = options.config.backInterceptor

                if (backHandler != true) {
                    finish()
                    return
                }

                when (interceptor) {
                    "native" -> handleNativeBack()
                    "javascript" -> view.wx.postWXEvent(
                        WXEventHandler(WXEvent.WX_ON_BACK, null)
                    )

                    true -> handleNativeBack()
                    null -> handleNativeBack()
                    false -> finish()
                    else -> finish()
                }
            }
        })
    }

    private fun handleNativeBack() {
        if (view.wx.canGoBack()) {
            view.wx.goBack()
            return
        }

        if (options.config.exitConfirm) {
            confirm(
                confirmData = ConfirmData(
                    title = getString(R.string.exit),
                    description = getString(R.string.exit_desc),
                    onConfirm = { finish() },
                    onClose = {}
                ),
                colorScheme = options.colorScheme
            )
            return
        }

        finish()
    }

   override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        view.wx.onActivityResult(requestCode, resultCode, data)
    }

    override fun onDestroy() {
        super.onDestroy()
        with(view.wx) {
            onActivityDestroyInterfaces()
            destroy()
        }
    }

    override fun onResume() {
        with(view.wx) {
            this.onResume()
            resumeTimers()
            postWXEvent(WXEvent.WX_ON_RESUME)
            onActivityResumeInterfaces()
        }

        super.onResume()
    }

    override fun onPause() {
        with(view.wx) {
            this.onPause()
            pauseTimers()
            postWXEvent(WXEvent.WX_ON_PAUSE)
            onActivityPauseInterfaces()
        }

        super.onPause()
    }

    override fun onStop() {
        with(view.wx) {
            onActivityStopInterfaces()
        }

        super.onStop()

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
    protected fun createLoadingRenderer(): View =
        FrameLayout(baseContext).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(options.colorScheme.background.toArgb())
            addView(ProgressBar(context).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER
                )
                indeterminateDrawable.setTint(options.colorScheme.primary.toArgb())
            })
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
    }
}
