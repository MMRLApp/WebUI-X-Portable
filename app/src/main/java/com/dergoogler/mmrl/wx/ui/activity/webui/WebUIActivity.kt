package com.dergoogler.mmrl.wx.ui.activity.webui

import android.os.Bundle
import android.os.Build
import androidx.lifecycle.lifecycleScope
import com.dergoogler.mmrl.wx.datastore.UserPreferencesRepository
import com.dergoogler.mmrl.ext.managerVersion
import com.dergoogler.mmrl.wx.ui.activity.webui.interfaces.KernelSUInterface
import com.dergoogler.mmrl.ext.exception.BrickException
import com.dergoogler.mmrl.platform.PlatformManager
import com.dergoogler.mmrl.ui.component.dialog.ConfirmData
import com.dergoogler.mmrl.ui.component.dialog.confirm
import com.dergoogler.mmrl.webui.activity.WXActivity
import com.dergoogler.mmrl.webui.util.WebUIOptions
import com.dergoogler.mmrl.webui.view.WebUIXView
import com.dergoogler.mmrl.wx.BuildConfig
import com.dergoogler.mmrl.wx.util.initPlatform
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class WebUIActivity : WXActivity() {
    @Inject
    internal lateinit var userPreferencesRepository: UserPreferencesRepository

    private val userPrefs get() = runBlocking { userPreferencesRepository.data.first() }

    private val userAgent
        get(): String {
            val mmrlVersion = this.managerVersion.second

            val platform = PlatformManager.platform.name

            val platformVersion = PlatformManager.get(-1) {
                moduleManager.versionCode
            }

            val osVersion = Build.VERSION.RELEASE
            val deviceModel = Build.MODEL

            return "WebUI X/$mmrlVersion (Linux; Android $osVersion; $deviceModel; $platform/$platformVersion)"
        }

    override fun onRender(savedInstanceState: Bundle?) {
        initPlatform(userPrefs)
        super.onRender(savedInstanceState)

        val modId = this.modId ?: throw BrickException("modId cannot be null or empty")

        val options = WebUIOptions(
            modId = modId,
            context = this,
            debug = userPrefs.developerMode,
            appVersionCode = BuildConfig.VERSION_CODE,
            remoteDebug = userPrefs.useWebUiDevUrl,
            enableEruda = userPrefs.enableErudaConsole,
            debugDomain = userPrefs.webUiDevUrl,
            userAgentString = userAgent,
            isDarkMode = userPrefs.isDarkMode(),
            colorScheme = userPrefs.colorScheme(this),
            cls = WebUIActivity::class.java
        )

        val view = WebUIXView(options).apply {
            wx.addJavascriptInterface<KernelSUInterface>()
            wx.loadDomain()
        }

        this.options = options
        this.view = view

        // Activity Title
        config {
            if (title != null) {
                setActivityTitle("WebUI X - $title")
            }
        }

        val loading = createLoadingRenderer()
        setContentView(loading)

        lifecycleScope.launch {
            val active = initPlatform(this, this@WebUIActivity, userPrefs.workingMode.toPlatform())

            if (!active.await()) {
                confirm(
                    ConfirmData(
                        title = "Failed!",
                        description = "Failed to initialize platform. Please try again.",
                        confirmText = "Close",
                        onConfirm = {
                            finish()
                        },
                    ),
                    colorScheme = options.colorScheme
                )

                return@launch
            }

            setContentView(view)
        }
    }
}