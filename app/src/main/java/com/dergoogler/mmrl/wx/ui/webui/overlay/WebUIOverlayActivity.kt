@file:Suppress("RedundantSuspendModifier")

package com.dergoogler.mmrl.wx.ui.webui.overlay

import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import com.dergoogler.mmrl.wx.datastore.UserPreferencesRepository
import com.dergoogler.mmrl.wx.datastore.model.UserPreferences
import com.dergoogler.mmrl.wx.model.module.AdbPath
import com.dergoogler.mmrl.wx.model.module.Module
import com.dergoogler.mmrl.wx.model.module.Module.Companion.readProps
import com.dergoogler.mmrl.wx.ui.webui.base
import com.dergoogler.mmrl.wx.ui.webui.pathHandlers.webrootPathHandler
import dagger.hilt.android.AndroidEntryPoint
import dev.mmrlx.nio.Path
import dev.mmrlx.nio.SuFile
import dev.mmrlx.nio.inputStream
import dev.mmrlx.webui.WebUI
import dev.mmrlx.webui.overlay.OverlayActivity
import dev.mmrlx.webui.overlay.OverlayKontext
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@AndroidEntryPoint
class WebUIOverlayActivity: OverlayActivity() {
    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    override val baseUri: Uri = BASE_URI

    override suspend fun onCreate(
        webui: WebUI,
        kontext: OverlayKontext,
    ): WebUI {
        val intent = requireNotNull(kontext.intent) {
            "Overlay intent is missing"
        }

        SuFile.AutoInit(
            kontext.coroutineScope,
            kontext,
        ).await()

        val prefs = userPreferencesRepository.data.first()
        val module = loadModule(intent, prefs, kontext)

        val directory =
            requireNotNull(intent.getStringExtra(EXTRA_DIRECTORY)?.takeIf(String::isNotBlank)) {
                "Missing directory"
            }

        return webui
            .base(
                module = module,
                colorScheme = prefs.colorScheme(kontext),
                prefs = prefs,
            )
            .settings {
                extra["windowManager"] = kontext.windowManager
            }
            .registerRoutes {
                webrootPathHandler(Path.resolve(module.path.webrootDir, directory))
            }
    }

    private suspend fun loadModule(
        intent: Intent,
        prefs: UserPreferences,
        kontext: OverlayKontext,
    ): Module {
        val moduleId = intent.getStringExtra(EXTRA_MODULE_ID)
            ?.takeIf(String::isNotBlank)
            ?: throw IllegalArgumentException("Missing module ID")


        val adbPath = AdbPath(prefs.getAdbPath(kontext))

        val moduleDir = SuFile(adbPath.modulesDir, moduleId)

        require(moduleDir.exists()) {
            "Module directory does not exist: ${moduleDir.path}"
        }

        val propsFile = SuFile(moduleDir, MODULE_PROP)

        require(propsFile.exists()) {
            "Module properties file does not exist: ${propsFile.path}"
        }

        val props = propsFile.inputStream().use { stream ->
            readProps(stream)
        }

        return Module(
            adbPath,
            props,
            kontext,
            prefs.webuiEngine,
        )
    }

    companion object {
        private val BASE_URI = "https://mui.kernelsu.org".toUri()

        const val EXTRA_MODULE_ID = "moduleId"
        const val EXTRA_DIRECTORY = "directory"
        private const val MODULE_PROP = "module.prop"
    }
}