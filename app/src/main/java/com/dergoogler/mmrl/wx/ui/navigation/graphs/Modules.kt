package com.dergoogler.mmrl.wx.ui.navigation.graphs

import android.util.Log
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.dergoogler.mmrl.ext.panicArguments
import com.dergoogler.mmrl.ext.panicString
import com.dergoogler.mmrl.platform.PlatformManager
import com.dergoogler.mmrl.platform.model.ModId.Companion.toModId
import com.dergoogler.mmrl.ui.providable.LocalNavController
import com.dergoogler.mmrl.wx.ui.navigation.MainScreen
import com.dergoogler.mmrl.wx.ui.screens.modules.ModulesScreen
import com.dergoogler.mmrl.wx.ui.screens.modules.screens.ConfigEditorScreen
import com.dergoogler.mmrl.wx.ui.screens.modules.screens.PluginsScreen
import com.dergoogler.mmrl.wx.util.getBaseDir

enum class ModulesScreen(val route: String) {
    Home("Modules"),
    Config("Config/{id}"),
    Plugins("Plugins/{id}"),
}

fun NavGraphBuilder.modulesScreen() = navigation(
    startDestination = ModulesScreen.Home.route,
    route = MainScreen.Modules.route
) {
    composable(
        route = ModulesScreen.Home.route,
        enterTransition = { fadeIn() },
        exitTransition = { fadeOut() }
    ) {
        ModulesScreen()
    }

    composable(
        route = ModulesScreen.Config.route,
        enterTransition = { fadeIn() },
        exitTransition = { fadeOut() }
    ) {
        val context = LocalContext.current
        val args = it.panicArguments
        val id = args.panicString("id")

        val baseDir = context.getBaseDir()
        val module = PlatformManager.moduleManager.getModuleById(id.toModId(baseDir.path))

        if (module == null) {
            return@composable
        }

        ConfigEditorScreen(module)
    }

    composable(
        route = ModulesScreen.Plugins.route,
        enterTransition = { fadeIn() },
        exitTransition = { fadeOut() }
    ) {
        val context = LocalContext.current
        val args = it.panicArguments
        val id = args.panicString("id")

        val baseDir = context.getBaseDir()
        val module = PlatformManager.moduleManager.getModuleById(id.toModId(baseDir.path))

        if (module == null) {
            LocalNavController.current.popBackStack()
            return@composable
        }

        PluginsScreen(module)
    }
}