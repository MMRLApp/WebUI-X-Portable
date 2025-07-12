package com.dergoogler.mmrl.webui.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageInfo
import android.net.Uri
import android.util.Log
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.pm.PackageInfoCompat
import androidx.core.net.toUri
import com.dergoogler.mmrl.platform.Platform
import com.dergoogler.mmrl.platform.PlatformManager
import com.dergoogler.mmrl.platform.file.SuFile
import com.dergoogler.mmrl.platform.model.ModId
import com.dergoogler.mmrl.platform.model.ModId.Companion.moduleDir
import com.dergoogler.mmrl.platform.model.ModId.Companion.webrootDir
import com.dergoogler.mmrl.ui.theme.Colors.Companion.getColorScheme
import com.dergoogler.mmrl.webui.activity.WXActivity
import com.dergoogler.mmrl.webui.client.WXClient
import com.dergoogler.mmrl.webui.model.Insets
import com.dergoogler.mmrl.webui.model.RequireNewVersion
import com.dergoogler.mmrl.webui.model.WebUIConfig.Companion.asWebUIConfig
import java.net.URI


/**
 * Configuration options for the WebUI.
 *
 * This data class holds various settings and derived properties that define the behavior and environment of the WebUI.
 * It encapsulates information such as module identification, application version, domain settings, debugging flags,
 * and file paths related to the WebUI.
 *
 * @property modId The unique identifier of the module associated with this WebUI.
 * @property context The Android [Context] used for accessing application-specific resources and classes.
 * @property appVersionCode The version code of the application. **Deprecated: Not used anymore.**
 * @property domain The base domain used for the WebUI (e.g., "https://mui.kernelsu.org").
 * @property domainSafeRegex A regular expression used to validate if a domain is safe for production use.
 * @property debugDomainSafeRegex A regular expression used to validate if a debug domain is safe, typically used for local network checks (e.g., localhost, local IP addresses).
 * @property debug Indicates whether debug mode is enabled. This might enable additional logging or development features.
 * @property remoteDebug Indicates whether remote debugging is enabled. This is often used with tools like Chrome DevTools.
 * @property enableEruda Indicates whether the Eruda console is enabled for debugging in the WebView.
 * @property debugDomain The domain used for debugging, often a local network address (e.g., "https://127.0.0.1:8080").
 * @property onUnsafeDomainRequest An optional callback function that is invoked when an attempt is made to load content from an unsafe domain.
 * @property isDarkMode Indicates whether the dark mode is enabled in the UI. This can be used to signal the WebView to use a dark theme.
 * @property userAgentString The custom user agent string to be used by the WebView.
 * @property client An optional [WXClient] that can be used to customize the behavior of the WebView.
 * @property colorScheme The [ColorScheme] to be applied to the WebUI, derived from the current theme and dark mode status.
 * @property cls An optional [Class] object, which can be used for context-specific operations or logging.
 */
@Immutable
data class WebUIOptions(
    val modId: ModId = ModId.EMPTY,
    val context: Context,
    @Deprecated("Not used anymore")
    val appVersionCode: Int = -1,
    val domain: Uri = "https://mui.kernelsu.org".toUri(),
    val domainSafeRegex: Regex = Regex("^https?://mui\\.kernelsu\\.org(/.*)?$"),
    val debugDomainSafeRegex: Regex = Regex(
        "^(https?://)?(localhost|127\\.0\\.0\\.1|::1|10(?:\\.\\d{1,3}){3}|172\\.(?:1[6-9]|2\\d|3[01])(?:\\.\\d{1,3}){2}|192\\.168(?:\\.\\d{1,3}){2})(?::([0-9]{1,5}))?$"
    ),
    val debug: Boolean = false,
    val remoteDebug: Boolean = false,
    val enableEruda: Boolean = false,
    val autoOpenEruda: Boolean = false,
    val debugDomain: String = "https://127.0.0.1:8080",
    val onUnsafeDomainRequest: (() -> Unit)? = null,
    val isDarkMode: Boolean = false,
    val userAgentString: String = "DON'T TRACK ME DOWN MOTHERFUCKER!",
    val colorScheme: ColorScheme = context.getColorScheme(id = 0, darkMode = isDarkMode),
    val client: ((WebUIOptions, Insets) -> WXClient)? = null,
    val cls: Class<out WXActivity>? = null,
) : ContextWrapper(context) {
    private val packageManager
        get() = PlatformManager.get(null) {
            packageManager
        }
    private val myUserId
        get() = PlatformManager.get(null) {
            userManager.myUserId
        }

    val isProviderAlive get() = PlatformManager.isAlive

    val versionName: String
        get() = PlatformManager.get("") {
            with(moduleManager) { version }
        }

    val versionCode: Int
        get() = PlatformManager.get(-1) {
            with(moduleManager) { versionCode }
        }

    val platform: Platform
        get() = PlatformManager.get(Platform.Unknown) {
            platform
        }

    val webRoot = modId.webrootDir

    fun isDomainSafe(domain: String): Boolean {
        if (debug) {
            return debugDomain.isLocalWifiUrl()
        }

        return domainSafeRegex.matches(domain)
    }

    val config = modId.asWebUIConfig

    val indexFile: String
        get() {
            val files = webRoot.listFiles { dir -> dir.isFile() }

            if (files == null) return "index.html"

            val indexFiles = files.map { it.name }

            val foundFile = indexFiles
                .sortedWith(compareByDescending {
                    when {
                        it.matches(Regex(".*\\.(wx|webuix)\\.(html|htm)$")) -> 3
                        it.matches(Regex(".*\\.mmrl\\.(html|htm)$")) -> 2
                        it.matches(Regex("index\\.(html|htm)$")) -> 1
                        else -> 0
                    }
                }).firstOrNull()

            if (foundFile != null) {
                return foundFile
            }

            return "index.html"
        }

    private val currentPackageInfo: PackageInfo?
        get() {
            if (packageManager == null) {
                Log.d(TAG, "packageManager is null")
                return null
            }

            if (myUserId == null) {
                Log.d(TAG, "myUserId is null")
                return null
            }

            try {
                return packageManager!!.getPackageInfo(context.packageName, 0, myUserId!!)
            } catch (e: Exception) {
                Log.e(TAG, "Error getting package version code: ${e.message}")
                return null
            }
        }

    val requireNewAppVersion: RequireNewVersion?
        get() {
            if (currentPackageInfo == null) {
                Log.d(TAG, "currentPackageInfo is null")
                return null
            }

            val packageName = currentPackageInfo!!.packageName
            val versionCode = PackageInfoCompat.getLongVersionCode(currentPackageInfo!!)

            val findPkgFromCfg = config.require.packages.find { pkg ->
                packageName in pkg.packageNames
            }

            if (findPkgFromCfg == null) {
                Log.d(TAG, "Package $packageName not found in config")
                return null
            }

            return RequireNewVersion(
                required = versionCode < findPkgFromCfg.code,
                requiredCode = findPkgFromCfg.code,
                supportLink = findPkgFromCfg.supportLink,
                supportText = findPkgFromCfg.supportText,
                packageInfo = currentPackageInfo!!
            )
        }

    val domainUrl
        get(): String {
            if (debug && remoteDebug) {
                return debugDomain
            }

            return "$domain/$indexFile"
        }

    val isErudaEnabled = debug && enableEruda
    val isAutoOpenErudaEnabled = isErudaEnabled && autoOpenEruda

    var recomposeCount by mutableIntStateOf(0)

    private fun String.isLocalWifiUrl(): Boolean {
        return try {
            val uri = URI(this)
            val host = uri.host ?: return false
            val port = uri.port

            host.matches(debugDomainSafeRegex) && (port == -1 || port in 1..65535)
        } catch (e: Exception) {
            false
        }
    }

    private companion object Default {
        const val TAG = "WebUIOptions"
    }

    @Suppress("RedundantIf")
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is WebUIOptions) return false

        if (context != other.context) return false
        if (modId != other.modId) return false
        if (appVersionCode != other.appVersionCode) return false
        if (domain != other.domain) return false
        if (domainSafeRegex != other.domainSafeRegex) return false
        if (remoteDebug != other.remoteDebug) return false
        if (debugDomainSafeRegex != other.debugDomainSafeRegex) return false
        if (debug != other.debug) return false
        if (enableEruda != other.enableEruda) return false
        if (debugDomain != other.debugDomain) return false
        if (isDarkMode != other.isDarkMode) return false
        if (userAgentString != other.userAgentString) return false
        if (cls != other.cls) return false
        if (colorScheme != other.colorScheme) return false
        if (onUnsafeDomainRequest != other.onUnsafeDomainRequest) return false

        return true
    }

    fun copy(
        modId: ModId = this.modId,
        domain: Uri = this.domain,
        domainSafeRegex: Regex = this.domainSafeRegex,
        remoteDebug: Boolean = this.remoteDebug,
        debugDomainSafeRegex: Regex = this.debugDomainSafeRegex,
        debug: Boolean = this.debug,
        enableEruda: Boolean = this.enableEruda,
        debugDomain: String = this.debugDomain,
        isDarkMode: Boolean = this.isDarkMode,
        userAgentString: String = this.userAgentString,
        colorScheme: ColorScheme = this.colorScheme,
        onUnsafeDomainRequest: (() -> Unit)? = this.onUnsafeDomainRequest,
        cls: Class<out WXActivity>? = this.cls,
    ): WebUIOptions = WebUIOptions(
        context = context,
        modId = modId,
        appVersionCode = appVersionCode,
        domain = domain,
        domainSafeRegex = domainSafeRegex,
        remoteDebug = remoteDebug,
        debugDomainSafeRegex = debugDomainSafeRegex,
        debug = debug,
        enableEruda = enableEruda,
        debugDomain = debugDomain,
        isDarkMode = isDarkMode,
        colorScheme = colorScheme,
        onUnsafeDomainRequest = onUnsafeDomainRequest,
        userAgentString = userAgentString,
        cls = cls,
    )

    override fun hashCode(): Int {
        var result = context.hashCode()
        result = 31 * result + modId.hashCode()
        result = 31 * result + appVersionCode
        result = 31 * result + domain.hashCode()
        result = 31 * result + domainSafeRegex.hashCode()
        result = 31 * result + remoteDebug.hashCode()
        result = 31 * result + debugDomainSafeRegex.hashCode()
        result = 31 * result + debug.hashCode()
        result = 31 * result + enableEruda.hashCode()
        result = 31 * result + debugDomain.hashCode()
        result = 31 * result + isDarkMode.hashCode()
        result = 31 * result + userAgentString.hashCode()
        result = 31 * result + (cls?.hashCode() ?: 0)
        result = 31 * result + colorScheme.hashCode()
        return result
    }
}