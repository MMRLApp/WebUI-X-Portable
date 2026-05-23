package com.dergoogler.mmrl.wx.ui.component

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import com.dergoogler.mmrl.ext.none
import com.dergoogler.mmrl.wx.model.module.Module
import com.dergoogler.mmrl.wx.model.module.ModuleUIState
import com.dergoogler.mmrl.wx.ui.providable.LocalDestinationsNavigator
import dev.mmrlx.compose.ui.scaffold.Scaffold
import dev.mmrlx.compose.ui.scaffold.ScaffoldScope

val LocalModule = compositionLocalOf { Module.Empty }

@Composable
fun ModuleScope(moduleId: String, content: @Composable () -> Unit) {
    val state by Module.rememberCreate(moduleId)

    when (state) {
        ModuleUIState.Loading -> {
            ContentWrapper("Loading...") {
                LoadingContent()
            }
        }

        is ModuleUIState.Error -> {
            ContentWrapper("ERROR") {
                val msg = (state as ModuleUIState.Error).message
                ErrorContent(msg)
            }
        }

        is ModuleUIState.Ready -> {
            val module = (state as ModuleUIState.Ready).module

            CompositionLocalProvider(LocalModule provides module) {
                content()
            }
        }
    }
}

@Composable
private fun ContentWrapper(
    title: String = "Error",
    content: @Composable ScaffoldScope.() -> Unit,
) {
    val navigator = LocalDestinationsNavigator.current
    Scaffold(
        toolbar = {
            NavigateUpToolbar(
                title = title,
                onBack = { navigator.popBackStack() },
            )
        },
        contentWindowInsets = WindowInsets.none,
        content = content
    )
}