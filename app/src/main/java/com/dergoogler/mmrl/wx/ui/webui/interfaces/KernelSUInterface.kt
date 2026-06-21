package com.dergoogler.mmrl.wx.ui.webui.interfaces

import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.view.Window
import android.webkit.JavascriptInterface
import android.widget.Toast
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.dergoogler.mmrl.platform.PlatformManager
import com.dergoogler.mmrl.wx.model.module.killShellWhenBackground
import com.dergoogler.mmrl.wx.ui.webui.isRootMode
import com.dergoogler.mmrl.wx.ui.webui.module
import com.topjohnwu.superuser.CallbackList
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.ShellUtils
import com.topjohnwu.superuser.internal.WaitRunnable
import dev.mmrlx.webui.PureJavaScriptInterface
import dev.mmrlx.webui.WebUI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.CompletableFuture

class KernelSUInterface(webui: WebUI) : PureJavaScriptInterface(webui) {
    override var id: String = "ksu"

    private val config get() = module.webrootConfig

    private val commands = if (!settings.isRootMode) arrayOf("sh") else arrayOf("su")

    private var shell: Shell = Shell.getShell()

    private inline fun <T> withNewRootShell(
        globalMnt: Boolean = false,
        block: Shell.() -> T,
    ): T {
        return createRootShell(globalMnt).use(block)
    }

    private fun createRootShell(
        globalMnt: Boolean = false,
    ): Shell {
        Shell.enableVerboseLogging = settings.debug
        val builder = Shell.Builder.create()
        if (globalMnt) {
            builder.setFlags(Shell.FLAG_MOUNT_MASTER)
        }
        shell = builder.build(*commands)
        return shell
    }

    @JavascriptInterface
    fun mmrl(): Boolean {
        return true
    }

    @JavascriptInterface
    fun toast(msg: String) {
        webview.post {
            Toast.makeText(kontext, msg, Toast.LENGTH_SHORT).show()
        }
    }

    @JavascriptInterface
    fun fullScreen(enable: Boolean) {
        mainThread {
            if (enable) {
                hideSystemUI(activity.window)
            } else {
                showSystemUI(activity.window)
            }
        }
    }

    @JavascriptInterface
    fun exec(cmd: String): String {
        return withNewRootShell { ShellUtils.fastCmd(this, cmd) }
    }

    @JavascriptInterface
    fun execBool(cmd: String): Boolean {
        return withNewRootShell { ShellUtils.fastCmdResult(this, cmd) }
    }

    @JavascriptInterface
    fun exec(cmd: String, callbackFunc: String) {
        exec(cmd, null, callbackFunc)
    }

    private fun processOptions(sb: StringBuilder, options: String?) {
        val opts = if (options == null) JSONObject() else {
            JSONObject(options)
        }

        val cwd = opts.optString("cwd")
        if (!TextUtils.isEmpty(cwd)) {
            sb.append("cd ${cwd};")
        }

        opts.optJSONObject("env")?.let { env ->
            env.keys().forEach { key ->
                sb.append("export ${key}=${env.getString(key)};")
            }
        }
    }

    @JavascriptInterface
    fun exec(
        cmd: String,
        options: String?,
        callbackFunc: String,
    ) {
        val finalCommand = StringBuilder()
        processOptions(finalCommand, options)
        finalCommand.append(cmd)

        supervisorScope.launch(Dispatchers.IO) {
            val result = withNewRootShell(
                globalMnt = true,
            ) {
                newJob().add(finalCommand.toString()).to(ArrayList(), ArrayList()).exec()
            }

            val stdout = result.out.joinToString(separator = "\n")
            val stderr = result.err.joinToString(separator = "\n")

            val jsCode =
                "(function() { try { ${callbackFunc}(${result.code}, ${
                    JSONObject.quote(
                        stdout
                    )
                }, ${JSONObject.quote(stderr)}); } catch(e) { console.error(e); } })();"

            runJs(jsCode)
        }
    }

    // ensure it really runs on the ui thread
    private fun runAndWait(r: Runnable) {
        if (ShellUtils.onMainThread()) {
            r.run()
        } else {
            val wr = WaitRunnable(r)
            Handler(Looper.getMainLooper()).post(wr)
            wr.waitUntilDone()
        }
    }

    @JavascriptInterface
    fun spawn(command: String, args: String, options: String?, callbackFunc: String) {
        val finalCommand = StringBuilder()

        processOptions(finalCommand, options)

        if (!TextUtils.isEmpty(args)) {
            finalCommand.append(command).append(" ")
            JSONArray(args).let { argsArray ->
                for (i in 0 until argsArray.length()) {
                    finalCommand.append(argsArray.getString(i))
                    finalCommand.append(" ")
                }
            }
        } else {
            finalCommand.append(command)
        }

        val shell = createRootShell(
            globalMnt = true,
        )

        val emitData = fun(name: String, data: String) {
            val jsCode =
                "(function() { try { ${callbackFunc}.${name}.emit('data', ${
                    JSONObject.quote(
                        data
                    )
                }); } catch(e) { console.error('emitData', e); } })();"

            runJs(jsCode)
        }

        val stdout = object : CallbackList<String>(::runAndWait) {
            override fun onAddElement(s: String) {
                emitData("stdout", s)
            }
        }

        val stderr = object : CallbackList<String>(::runAndWait) {
            override fun onAddElement(s: String) {
                emitData("stderr", s)
            }
        }

        supervisorScope.launch(Dispatchers.IO) {
            val future = shell.newJob().add(finalCommand.toString()).to(stdout, stderr).enqueue()
            val completableFuture = CompletableFuture.supplyAsync {
                future.get()
            }

            completableFuture.thenAccept { result ->
                val emitExitCode =
                    "(function() { try { ${callbackFunc}.emit('exit', ${result.code}); } catch(e) { console.error(`emitExit error: \${e}`); } })();"
                runJs(emitExitCode)


                if (result.code != 0) {
                    val emitErrCode =
                        "(function() { try { var err = new Error(); err.exitCode = ${result.code}; err.message = ${
                            JSONObject.quote(
                                result.err.joinToString(
                                    "\n"
                                )
                            )
                        };${callbackFunc}.emit('error', err); } catch(e) { console.error('emitErr', e); } })();"
                    runJs(emitErrCode)
                }
            }.whenComplete { _, _ ->
                runJsCatching { shell.close() }
            }
        }
    }

    @JavascriptInterface
    fun moduleInfo(): String {
        val moduleInfos = JSONArray(PlatformManager.moduleManager.modules)
        val currentModuleInfo = JSONObject()
        currentModuleInfo.put("moduleDir", module.path.moduleDir)
        for (i in 0 until moduleInfos.length()) {
            val currentInfo = moduleInfos.getJSONObject(i)

            if (currentInfo.getString("id") != module.id) {
                continue
            }

            val keys = currentInfo.keys()
            for (key in keys) {
                currentModuleInfo.put(key, currentInfo[key])
            }
            break
        }
        return currentModuleInfo.toString()
    }

    override fun onStop() {
        super.onStop()

        if (config.killShellWhenBackground) {
            shell.close()
        }
    }

    override fun onDestroy() {
        shell.close()
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()

        if (config.killShellWhenBackground) {
            shell = createRootShell(true)
        }
    }

    private fun hideSystemUI(window: Window) =
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

    private fun showSystemUI(window: Window) =
        WindowInsetsControllerCompat(
            window,
            window.decorView
        ).show(WindowInsetsCompat.Type.systemBars())
}

