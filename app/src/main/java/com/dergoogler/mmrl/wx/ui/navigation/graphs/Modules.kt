package com.dergoogler.mmrl.wx.ui.navigation.graphs

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import androidx.navigation.toRoute
import com.dergoogler.mmrl.platform.PlatformManager
import com.dergoogler.mmrl.platform.model.ModId.Companion.toModId
import com.dergoogler.mmrl.wx.ui.navigation.MainRoute
import com.dergoogler.mmrl.wx.ui.screens.modules.ModulesScreen
import com.dergoogler.mmrl.wx.ui.screens.modules.screens.ConfigEditorScreen
import com.dergoogler.mmrl.wx.ui.screens.modules.screens.PluginsScreen
import com.dergoogler.mmrl.wx.util.getBaseDir
import kotlinx.serialization.Serializable

sealed interface ModulesRoute {
    @Serializable
    data object Home : ModulesRoute

    @Serializable
    data class Config(val id: String) : ModulesRoute

    @Serializable
    data class Plugins(val id: String) : ModulesRoute
}

fun NavGraphBuilder.modulesRoute() = navigation<MainRoute.Modules>(
    startDestination = ModulesRoute.Home,
) {
    composable<ModulesRoute.Home>(
        enterTransition = { fadeIn() },
        exitTransition = { fadeOut() }
    ) {
        ModulesScreen()
    }

    composable<ModulesRoute.Config>(
        enterTransition = { fadeIn() },
        exitTransition = { fadeOut() }
    ) {
        val context = LocalContext.current
        val route = it.toRoute<ModulesRoute.Config>()

        val baseDir = context.getBaseDir()
        val module = PlatformManager.moduleManager.getModuleById(route.id.toModId(baseDir.path))

        if (module == null) {
            return@composable
        }

        ConfigEditorScreen(module)
    }

    composable<ModulesRoute.Plugins>(
        enterTransition = { fadeIn() },
        exitTransition = { fadeOut() }
    ) {
        val context = LocalContext.current
        val route = it.toRoute<ModulesRoute.Config>()

        val baseDir = context.getBaseDir()
        val module = PlatformManager.moduleManager.getModuleById(route.id.toModId(baseDir.path))

        if (module == null) {
            return@composable
        }

        PluginsScreen(module)
    }
}