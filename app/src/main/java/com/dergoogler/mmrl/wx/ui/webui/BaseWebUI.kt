package com.dergoogler.mmrl.wx.ui.webui

import android.system.OsConstants.O_RDONLY
import androidx.compose.material3.ColorScheme
import com.dergoogler.mmrl.wx.datastore.model.UserPreferences
import com.dergoogler.mmrl.wx.datastore.model.WebUILaunchMode
import com.dergoogler.mmrl.wx.datastore.model.WorkingMode.Companion.isRoot
import com.dergoogler.mmrl.wx.model.module.Module
import com.dergoogler.mmrl.wx.ui.webui.interfaces.appInterface
import com.dergoogler.mmrl.wx.ui.webui.interfaces.fileSystemInterface
import com.dergoogler.mmrl.wx.ui.webui.interfaces.ksu.kernelSUInterface
import com.dergoogler.mmrl.wx.ui.webui.interfaces.legacy.legacyFileInputInterface
import com.dergoogler.mmrl.wx.ui.webui.interfaces.legacy.legacyFileOutputInterface
import com.dergoogler.mmrl.wx.ui.webui.interfaces.legacy.legacyModuleInterface
import com.dergoogler.mmrl.wx.ui.webui.interfaces.moduleInterface
import com.dergoogler.mmrl.wx.ui.webui.pathHandlers.internalPathHandler
import com.dergoogler.mmrl.wx.ui.webui.pathHandlers.ksu.iconPathHandler
import com.dergoogler.mmrl.wx.ui.webui.pathHandlers.suRoute
import dev.mmrlx.nio.SuFile
import dev.mmrlx.nio.SuFileInputStream
import dev.mmrlx.nio.SuFileOutputStream
import dev.mmrlx.nio.SuRandomAccessFile
import dev.mmrlx.webui.WebUI
import okhttp3.internal.userAgent

fun WebUI.base(
    module: Module,
    colorScheme: ColorScheme,
    prefs: UserPreferences,
    isDebug: Boolean = false,
) = this.factories {
    inputStreamFactory { paths ->
        val path = paths.first
        val mode: Int = paths[1, O_RDONLY]
        SuFileInputStream(SuFile(path), mode, 0)
    }

    outputStreamFactory { paths ->
        val path = paths.first
        val append = paths[1, false]
        SuFileOutputStream(path, append)
    }

    randomAccessFileFactory { paths ->
        val path = paths.first
        val mode = paths[1, "r"]
        SuRandomAccessFile(path, mode)
    }

    fileFactory { paths ->
        SuFile(*paths)
    }
}
    .settings {
        schemeWhitelist += "ksu"
        useDefaultApplicationInterface = false
        useDefaultFileSystem = false
        debug = isDebug
        forceKillProcess =
            prefs.forceKillWebUIProcess && prefs.webuiLaunchMode == WebUILaunchMode.SEPARATE_PROCESS
        userAgentString = userAgent
        darkMode = prefs.isDarkMode()
        showEventPayloadInConsole = prefs.logEventPayload

        extra["module"] = module
        extra["disableGlobalExitConfirm"] = prefs.disableGlobalExitConfirm
        extra["isRootMode"] = prefs.workingMode.isRoot
        extra["workingMode"] = prefs.workingMode
        extra["mdColorScheme"] = colorScheme
        extra["preferences"] = prefs
    }
    .registerJavaScriptInterfaces {
        moduleInterface()
        kernelSUInterface()
        appInterface()
        fileSystemInterface()
        // legacy
        legacyModuleInterface()
        legacyFileInputInterface()
        legacyFileOutputInterface()
    }
    .backHandlers()
    .client {}
    .chromeClient { }
    .registerRoutes {
        // ksu://icon/
        iconPathHandler()
    }
    .registerRoutes {
        internalPathHandler()
        suRoute(
            "/.${module.id}/",
            module.path.moduleDir
        )

        suRoute(
            "/.adb/",
            module.adbPath.baseDir
        )
        suRoute(
            "/.config/",
            module.adbPath.configDir
        )
        suRoute(
            "/.local/",
            module.adbPath.localDir
        )
    }