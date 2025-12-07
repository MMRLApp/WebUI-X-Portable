@file:OptIn(ExperimentalSerializationApi::class)

package com.dergoogler.mmrl.wx.datastore.model

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import com.dergoogler.mmrl.datastore.model.DarkMode
import com.dergoogler.mmrl.datastore.model.ModulesMenu
import com.dergoogler.mmrl.datastore.model.WorkingMode
import com.dergoogler.mmrl.ui.theme.Colors
import com.dergoogler.mmrl.ui.theme.Colors.Companion.getColorScheme
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.protobuf.ProtoNumber
import java.io.InputStream
import java.io.OutputStream
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@Serializable
data class UserPreferences(
    @ProtoNumber(1) val workingMode: WorkingMode = WorkingMode.FIRST_SETUP,
    @ProtoNumber(2) val darkMode: DarkMode = DarkMode.FollowSystem,
    @ProtoNumber(3) val themeColor: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Colors.Dynamic.id else Colors.MMRLBase.id,
    @ProtoNumber(8) val modulesMenu: ModulesMenu = ModulesMenu(),
    @ProtoNumber(13) val datePattern: String = "d MMMM yyyy",
    @ProtoNumber(21) val webUiDevUrl: String = "https://127.0.0.1:8080",
    @ProtoNumber(22) val developerMode: Boolean = false,
    @ProtoNumber(23) val useWebUiDevUrl: Boolean = false,
    @ProtoNumber(35) val enableErudaConsole: Boolean = false,
    @ProtoNumber(37) val webuiEngine: WebUIEngine = WebUIEngine.PREFER_MODULE,
    @ProtoNumber(38) val enableAutoOpenEruda: Boolean = false,
    @ProtoNumber(39) val forceKillWebUIProcess: Boolean = false,
    @ProtoNumber(40) val disableGlobalExitConfirm: Boolean = false,
) {
    fun isDarkMode() = when (darkMode) {
        DarkMode.AlwaysOff -> false
        DarkMode.AlwaysOn -> true
        DarkMode.FollowSystem -> isSystemInDarkTheme()
    }

    fun colorScheme(context: Context) = context.getColorScheme(themeColor, isDarkMode())

    private fun isSystemInDarkTheme(): Boolean {
        val uiMode = Resources.getSystem().configuration.uiMode
        return (uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun encodeTo(output: OutputStream) = output.write(
        ProtoBuf.encodeToByteArray(this)
    )

    @OptIn(ExperimentalContracts::class)
    fun developerMode(
        also: UserPreferences.() -> Boolean,
    ): Boolean {
        contract {
            callsInPlace(also, InvocationKind.AT_MOST_ONCE)
        }

        return developerMode && also()
    }

    companion object {
        @OptIn(ExperimentalSerializationApi::class)
        fun decodeFrom(input: InputStream): UserPreferences =
            ProtoBuf.decodeFromByteArray(input.readBytes())
    }
}
