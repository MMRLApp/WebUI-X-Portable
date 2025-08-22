package com.dergoogler.mmrl.modconf

import androidx.compose.runtime.staticCompositionLocalOf

val LocalKontext = staticCompositionLocalOf<Kontext> {
    error("CompositionLocal LocalKontext not present")
}