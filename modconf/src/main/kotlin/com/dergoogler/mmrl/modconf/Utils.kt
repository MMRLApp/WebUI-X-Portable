package com.dergoogler.mmrl.modconf

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Custom ModConf context that allows loading custom resources
 */
val LocalKontext = staticCompositionLocalOf<Kontext> {
    error("CompositionLocal LocalKontext not present")
}

/**
 * Stores the original app context. {@link LocalContext} will be overriden by {@link Kontext}
 */
val LocalAppContext = staticCompositionLocalOf<Context> {
    error("CompositionLocal LocalKontext not present")
}