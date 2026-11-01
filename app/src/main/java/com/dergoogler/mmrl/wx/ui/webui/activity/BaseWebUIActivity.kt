package com.dergoogler.mmrl.wx.ui.webui.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.dergoogler.mmrl.ext.exception.BrickException
import com.dergoogler.mmrl.wx.datastore.model.WebUILaunchMode
import com.dergoogler.mmrl.wx.datastore.providable.LocalUserPreferences
import com.dergoogler.mmrl.wx.ui.component.LocalModule
import com.dergoogler.mmrl.wx.ui.providable.LocalDestinationsNavigator
import com.dergoogler.mmrl.wx.ui.webui.WebUIScreen
import com.dergoogler.mmrl.wx.util.BaseActivity
import com.dergoogler.mmrl.wx.util.setBaseContent
import com.dergoogler.mmrl.wx.util.setMyCrashHandler
import com.ramcosta.composedestinations.generated.destinations.WebUIScreenDestination
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
open class BaseWebUIActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        setMyCrashHandler()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val moduleId = intent.getStringExtra("MODULE_ID")
            ?: throw BrickException("moduleId cannot be null or empty")

        Log.d("BaseWebUIActivity", "moduleId: $moduleId")

        setBaseContent {
            WebUIScreen(moduleId)
        }
    }

    companion object {
        inline fun <reified T : BaseWebUIActivity> start(context: Context, moduleId: String) {
            try {
                val intent = Intent(
                    context,
                    T::class.java
                )
                    .apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                        putExtra("MODULE_ID", moduleId)
                    }

                context.startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, e.toString(), Toast.LENGTH_LONG).show()
            }
        }

        @Composable
        fun launcher(): () -> Unit {
            val prefs = LocalUserPreferences.current
            val context = LocalContext.current
            val module = LocalModule.current
            val navigator = LocalDestinationsNavigator.current

            return remember(prefs.webuiLaunchMode, module) {
                {
                    when (prefs.webuiLaunchMode) {
                        WebUILaunchMode.NO_ACTIVITY -> {
                            navigator.navigate(WebUIScreenDestination(module.id, true))
                        }

                        WebUILaunchMode.SEPARATE_PROCESS -> {
                            start<NewProcessWebUIActivity>(context, module.id)
                        }

                        WebUILaunchMode.SAME_APP_PROCESS -> {
                            start<ExistingProcessWebUIActivity>(context, module.id)
                        }
                    }
                }
            }
        }
    }
}
