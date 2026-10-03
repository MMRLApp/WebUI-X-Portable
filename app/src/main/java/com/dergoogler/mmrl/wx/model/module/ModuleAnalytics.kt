package com.dergoogler.mmrl.wx.model.module

import android.app.usage.StorageStatsManager
import android.content.Context
import android.os.Environment
import android.os.storage.StorageManager
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.io.IOException

data class ModuleAnalytics(
    private val context: Context,
    private val local: List<Module>,
) {
    val totalModules = local.size

    private fun getTotalByState(state: com.dergoogler.mmrl.platform.content.State) =
        local.filter { it.state == state }.size

    val totalEnabled = getTotalByState(com.dergoogler.mmrl.platform.content.State.ENABLE)
    val totalDisabled = getTotalByState(com.dergoogler.mmrl.platform.content.State.DISABLE)
    val totalUpdated = getTotalByState(com.dergoogler.mmrl.platform.content.State.UPDATE)

    val totalModulesUsageBytes get() = local.sumOf { it.size }

    val totalDeviceStorageBytes: Long
        get() {
            val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager

            return try {
                val storageStatsManager =
                    context.getSystemService(Context.STORAGE_STATS_SERVICE) as StorageStatsManager
                val uuid = storageManager.getUuidForPath(Environment.getDataDirectory())
                val totalBytes = storageStatsManager.getTotalBytes(uuid)
                totalBytes
            } catch (e: IOException) {
                Log.d("ModuleAnalytics", "totalDeviceStorageBytes: $e")
                0L
            }
        }

    val totalStorageUsage = totalModulesUsageBytes.toFloat() / totalDeviceStorageBytes.toFloat()

    companion object {
        @Composable
        fun remember(modules: List<Module>): State<ModuleAnalytics> {
            val context = LocalContext.current
            return remember(modules) {
                derivedStateOf {
                    ModuleAnalytics(
                        context = context,
                        local = modules,
                    )
                }
            }
        }
    }
}


