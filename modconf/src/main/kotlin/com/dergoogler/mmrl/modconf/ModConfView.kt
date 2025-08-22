@file:SuppressLint("ViewConstructor")

package com.dergoogler.mmrl.modconf

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ModConfView(kontext: Kontext) {
    val config = kontext.config
    val loader = kontext.dexLoader ?: run {
        ErrorUI("No dexLoader available")
        return
    }

    val instance = remember(loader, config.className) {
        try {
            val rawClass = loader.loadClass(config.className)

            if (!ModConfModule::class.java.isAssignableFrom(rawClass)) {
                Log.e(TAG, "Loaded class ${config.className} does not extend ModConfModule")
                null
            } else {
                @Suppress("UNCHECKED_CAST")
                val clazz = rawClass as Class<out ModConfModule>
                val modconfClass = ModConfClass(clazz)
                modconfClass.createNew(kontext).also {
                    Log.i(TAG, "Successfully loaded ModConfModule: ${clazz.name}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load class ${config.className}", e)
            null
        }
    }

    if (instance == null) {
        ErrorUI("Failed to load module: ${config.className}")
        return
    }

    instance.Content()
}

@Composable
private fun ErrorUI(message: String) {
    Text(
        text = message,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.padding(16.dp)
    )
}

private const val TAG = "ModConfView"



