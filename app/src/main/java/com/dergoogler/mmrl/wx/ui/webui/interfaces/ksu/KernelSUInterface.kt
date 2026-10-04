package com.dergoogler.mmrl.wx.ui.webui.interfaces.ksu

import android.content.pm.ApplicationInfo
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.view.Window
import android.widget.Toast
import androidx.core.content.pm.PackageInfoCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.dergoogler.mmrl.wx.model.module.killShellWhenBackground
import com.dergoogler.mmrl.wx.ui.webui.isRootMode
import com.dergoogler.mmrl.wx.ui.webui.module
import com.dergoogler.mmrl.wx.ui.webui.util.Permissions
import com.dergoogler.mmrl.wx.ui.webui.util.dangerousFunction
import com.dergoogler.mmrl.wx.ui.webui.util.packages
import com.topjohnwu.superuser.CallbackList
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.ShellUtils
import com.topjohnwu.superuser.internal.WaitRunnable
import dev.mmrlx.webui.JavaScriptFunctionScope
import dev.mmrlx.webui.JavaScriptRegistry
import dev.mmrlx.webui.WebUI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.CompletableFuture

/** Native state of the `ksu` interface (shell handling + lifecycle). */
private class KernelSUShell(private val webui: WebUI) : WebUI by webui {
    private val config get() = module.webrootConfig
    private val commands = if (!settings.isRootMode) arrayOf("sh") else arrayOf("su")

    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    var shell: Shell = Shell.getShell()

    init {
        onStop { if (config.killShellWhenBackground) shell.close() }
        onResume { if (config.killShellWhenBackground) shell = createRootShell(true) }
        onDestroy {
            shell.close()
            scope.cancel()
        }
    }

    fun createRootShell(globalMnt: Boolean = false): Shell {
        Shell.enableVerboseLogging = settings.debug

        val builder = Shell.Builder.create()
        if (globalMnt) builder.setFlags(Shell.FLAG_MOUNT_MASTER)

        shell = builder.build(*commands)
        return shell
    }

    inline fun <T> withNewRootShell(globalMnt: Boolean = false, block: Shell.() -> T): T =
        createRootShell(globalMnt).use(block)

    /** Ensures it really runs on the ui thread. */
    fun runAndWait(r: Runnable) {
        if (ShellUtils.onMainThread()) {
            r.run()
        } else {
            val wr = WaitRunnable(r)
            Handler(Looper.getMainLooper()).post(wr)
            wr.waitUntilDone()
        }
    }
}

private fun processOptions(sb: StringBuilder, opts: JSONObject?) {
    val cwd = opts?.optString("cwd")
    if (!TextUtils.isEmpty(cwd)) sb.append("cd ${cwd};")

    opts?.optJSONObject("env")?.let { env ->
        env.keys().forEach { key -> sb.append("export ${key}=${env.getString(key)};") }
    }
}

/** Options may arrive as an object or as a JSON string (old `JSON.stringify(options)` callers). */
private fun JavaScriptFunctionScope.optOptions(index: Int): JSONObject? =
    when (val v = if (isNull(index)) null else arguments.get(index)) {
        null -> null
        is JSONObject -> v
        is String -> JSONObject(v)
        else -> throw IllegalArgumentException("bad argument #${index + 1} (object expected)")
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

/** Legacy `window.ksu` interface. */
fun JavaScriptRegistry.kernelSUInterface() = namespace("ksu") {
    val io = KsuIO(this@kernelSUInterface)

    val ksu = KernelSUShell(this)
    val pm = kontext.packageManager

    // Legacy
    function("mmrl") { true }

    function("toast") {
        val msg = checkString(0)
        webview.post { Toast.makeText(kontext, msg, Toast.LENGTH_SHORT).show() }
    }

    function("fullScreen") {
        val enable = checkBoolean(0)
        webview.post {
            if (enable) hideSystemUI(activity.window) else showSystemUI(activity.window)
        }
    }

    dangerousFunction("execBool", Permissions.KSU.SHELL) {
        val cmd = checkString(0)
        ksu.withNewRootShell { ShellUtils.fastCmdResult(this, cmd) }
    }

    // exec(cmd) -> string | exec(cmd, callback) | exec(cmd, options, callback)
    dangerousFunction("exec", Permissions.KSU.SHELL) {
        val cmd = checkString(0)

        if (size == 1) {
            return@dangerousFunction ksu.withNewRootShell { ShellUtils.fastCmd(this, cmd) }

        }

        val options = if (size >= 3) optOptions(1) else null
        val callbackFunc = checkString(size - 1)

        val finalCommand = StringBuilder()
        processOptions(finalCommand, options)
        finalCommand.append(cmd)

        ksu.scope.launch {
            val result = ksu.withNewRootShell(globalMnt = true) {
                newJob().add(finalCommand.toString()).to(ArrayList(), ArrayList()).exec()
            }

            val stdout = result.out.joinToString(separator = "\n")
            val stderr = result.err.joinToString(separator = "\n")

            runJs(
                "(function() { try { ${callbackFunc}(${result.code}, ${JSONObject.quote(stdout)}, ${
                    JSONObject.quote(
                        stderr
                    )
                }); } catch(e) { console.error(e); } })();"
            )
        }
        null
    }

    dangerousFunction("spawn", Permissions.KSU.SHELL) {
        val command = checkString(0)
        val args = optString(1, "")
        val options = optOptions(2)
        val callbackFunc = checkString(3)

        val finalCommand = StringBuilder()
        processOptions(finalCommand, options)

        if (!TextUtils.isEmpty(args)) {
            finalCommand.append(command).append(" ")
            // args may be sent as a JSON string or as an array
            val argsArray = JSONArray(args)
            for (i in 0 until argsArray.length()) {
                finalCommand.append(argsArray.getString(i)).append(" ")
            }
        } else {
            finalCommand.append(command)
        }

        val shell = ksu.createRootShell(globalMnt = true)

        val emitData = fun(name: String, data: String) {
            runJs(
                "(function() { try { ${callbackFunc}.${name}.emit('data', ${
                    JSONObject.quote(
                        data
                    )
                }); } catch(e) { console.error('emitData', e); } })();"
            )
        }

        val stdout = object : CallbackList<String>(ksu::runAndWait) {
            override fun onAddElement(s: String) = emitData("stdout", s)
        }
        val stderr = object : CallbackList<String>(ksu::runAndWait) {
            override fun onAddElement(s: String) = emitData("stderr", s)
        }

        ksu.scope.launch {
            val future =
                shell.newJob().add(finalCommand.toString()).to(stdout, stderr).enqueue()

            CompletableFuture.supplyAsync { future.get() }
                .thenAccept { result ->
                    runJs(
                        "(function() { try { ${callbackFunc}.emit('exit', ${result.code}); } catch(e) { console.error(`emitExit error: \${e}`); } })();"
                    )

                    if (result.code != 0) {
                        runJs(
                            "(function() { try { var err = new Error(); err.exitCode = ${result.code}; err.message = ${
                                JSONObject.quote(
                                    result.err.joinToString("\n")
                                )
                            };${callbackFunc}.emit('error', err); } catch(e) { console.error('emitErr', e); } })();"
                        )
                    }
                }
                .whenComplete { _, _ -> runCatching { shell.close() } }
        }
        null

    }

    function("moduleInfo") {
        val currentModuleInfo = JSONObject()
        currentModuleInfo.put("moduleDir", module.path.moduleDir)
        currentModuleInfo.put("id", module.id)
        currentModuleInfo.toString()
    }

    function("listPackages") {
        val type = checkString(0).lowercase()

        JSONArray(
            packages
                .filter { appInfo ->
                    val flags = appInfo.applicationInfo?.flags ?: 0
                    val category = appInfo.applicationInfo?.category ?: 0

                    when (type) {
                        "system" -> (flags and ApplicationInfo.FLAG_SYSTEM) != 0
                        "user" -> (flags and ApplicationInfo.FLAG_SYSTEM) == 0
                        "game" -> category == ApplicationInfo.CATEGORY_GAME ||
                                (flags and ApplicationInfo.FLAG_IS_GAME) != 0

                        else -> true
                    }
                }
                .map { it.packageName }
                .sorted()
        ).toString()
    }

    function("getPackagesInfo") {
        // accepts an array, or the old JSON string
        val packageNames = optArray(0) ?: JSONArray(checkString(0))
        val appMap = packages.associateBy { it.packageName }
        val jsonArray = JSONArray()

        for (i in 0 until packageNames.length()) {
            val pkgName = packageNames.getString(i)
            val appInfo = appMap[pkgName]
            val obj = JSONObject()

            if (appInfo != null) {
                val app = appInfo.applicationInfo
                obj.put("packageName", appInfo.packageName)
                obj.put("versionName", appInfo.versionName ?: "")
                obj.put("versionCode", PackageInfoCompat.getLongVersionCode(appInfo))
                obj.put("appLabel", pm.getApplicationLabel(appInfo.applicationInfo!!))
                obj.put("category", app?.category ?: JSONObject.NULL)
                obj.put("flags", app?.flags ?: JSONObject.NULL)
                obj.put(
                    "isSystem",
                    if (app != null) (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0 else JSONObject.NULL
                )
                obj.put(
                    "isGame",
                    if (app != null) app.category == ApplicationInfo.CATEGORY_GAME ||
                            (app.flags and ApplicationInfo.FLAG_IS_GAME) != 0 else JSONObject.NULL
                )
                obj.put("uid", app?.uid ?: JSONObject.NULL)
            } else {
                obj.put("packageName", pkgName)
                obj.put("error", "Package not found or inaccessible")
            }

            jsonArray.put(obj)
        }

        jsonArray.toString()
    }

    dangerousFunction("io", Permissions.KSU.IO) {
        newObject { ksuIO(io) }
    }
}