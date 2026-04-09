@file:Suppress("unused")

package com.dergoogler.mmrl.wx.ui.webui.interfaces

import dev.mmrlx.webui.WebUI
import dev.mmrlx.webui.interfaces.JavaScriptInterface

// TODO: Not yet implemented, requires file system re-work.
class FileSystemInterface(webui: WebUI) : JavaScriptInterface(webui) {

    init {
        throw UnsupportedOperationException("Not yet implemented, requires file system rework.")
    }
//
//
//    private enum class AccessMode(val mode: Int) {
//        F_OK(0),
//        R_OK(1),
//        W_OK(2),
//        X_OK(4);
//
//        companion object {
//            fun from(mode: Int): AccessMode? {
//                return entries.find { it.mode == mode }
//            }
//        }
//    }
//
//    // TODO: add support for bitwise modes
//    // https://github.com/MMRLApp/MMRL/blob/master/platform/src/main/kotlin/com/dergoogler/mmrl/platform/file/FileManager.kt#L182-L183
//    private enum class Mode(val mode: Int)
//
//
//    @ExportMethod
//    suspend fun readFile(
//        path: String,
//        options: JSONObject,
//    ): Promise<String> {
//        val charset = options.getAs<String, Charset>("encoding", Charsets.UTF_8) {
//            Charset.forName(it)
//        }
//
//        return Promise {
//            try {
//                val text = SuFile(path).readNText(charset)
//                resolve(text)
//            } catch (e: Exception) {
//                reject(e)
//            }
//        }
//    }
//
//    @ExportMethod
//    fun readFileSync(
//        path: String,
//        options: JSONObject,
//    ): String? {
//        val charset = options.getAs<String, Charset>("encoding", Charsets.UTF_8) {
//            Charset.forName(it)
//        }
//
//        try {
//            return SuFile(path).readNText(charset)
//        } catch (e: Exception) {
//            console.error(e)
//            return null
//        }
//    }
//
//    @ExportMethod
//    suspend fun writeFile(
//        path: String,
//        data: String,
//        options: JSONObject,
//    ): Promise<Unit> {
//        val charset = options.getAs<String, Charset>("encoding", Charsets.UTF_8) {
//            Charset.forName(it)
//        }
//
//        return Promise {
//            try {
//                SuFile(path).writeNText(data, charset)
//                resolve(Unit)
//            } catch (e: Exception) {
//                reject(e)
//            }
//        }
//    }
//
//
//    @ExportMethod
//    suspend fun access(
//        path: String,
//        mode: Int,
//    ): Promise<Boolean> {
//        return Promise {
//            val accessMode = AccessMode.from(mode)
//
//            if (accessMode == null) {
//                reject(Error("Invalid access mode"))
//                return@Promise
//            }
//
//            try {
//                val success = when (accessMode) {
//                    AccessMode.F_OK -> {
//                        SuFile(path).exists()
//                    }
//
//                    AccessMode.R_OK -> {
//                        SuFile(path).canRead()
//                    }
//
//                    AccessMode.W_OK -> {
//                        SuFile(path).canWrite()
//                    }
//
//                    AccessMode.X_OK -> {
//                        SuFile(path).canExecute()
//                    }
//                }
//
//                resolve(success)
//            } catch (e: Exception) {
//                reject(e)
//            }
//        }
//    }
//
//    private inline fun <reified R, reified T> JSONObject.getAs(
//        name: String,
//        default: T,
//        converter: (R) -> T,
//    ): T {
//        if (opt(name) == null || isNull(name)) return default
//
//        val rawValue: Any? = when (R::class) {
//            String::class -> optString(name)
//            Int::class -> optInt(name)
//            Long::class -> optLong(name)
//            Double::class -> optDouble(name)
//            Boolean::class -> optBoolean(name)
//            JSONObject::class -> optJSONObject(name)
//            JSONArray::class -> optJSONArray(name)
//            else -> opt(name)
//        }
//
//        return try {
//            (rawValue as? R)?.let { converter(it) } ?: default
//        } catch (e: Exception) {
//            default
//        }
//    }
}