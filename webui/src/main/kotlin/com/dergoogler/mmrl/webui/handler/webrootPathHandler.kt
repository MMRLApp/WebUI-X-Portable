package com.dergoogler.mmrl.webui.handler

import android.util.Log
import android.webkit.WebResourceResponse
import com.dergoogler.mmrl.ext.isNull
import com.dergoogler.mmrl.platform.file.SuFile
import com.dergoogler.mmrl.platform.file.SuFile.Companion.toSuFile
import com.dergoogler.mmrl.platform.model.ModId.Companion.moduleConfigDir
import com.dergoogler.mmrl.webui.InjectionType
import com.dergoogler.mmrl.webui.PathHandler
import com.dergoogler.mmrl.webui.addInjection
import com.dergoogler.mmrl.webui.asResponse
import com.dergoogler.mmrl.webui.model.Insets
import com.dergoogler.mmrl.webui.notFoundResponse
import com.dergoogler.mmrl.webui.util.WebUIOptions
import java.io.ByteArrayOutputStream
import java.io.IOException

fun webrootPathHandler(
    options: WebUIOptions,
    insets: Insets,
): PathHandler {

    val configBase = options.modId.moduleConfigDir
    val configStyleBase = SuFile(configBase, "style")
    val configJsBase = SuFile(configBase, "js")

    val customCssFile = SuFile(configStyleBase, "custom.css")

    val customJsHead = SuFile(configJsBase, "head")
    val customJsBody = SuFile(configJsBase, "body")
    val customJsFile = SuFile(customJsBody, "custom.js")

    val directory = SuFile(options.webRoot).getCanonicalDirPath().toSuFile()

    SuFile.createDirectories(customJsHead, customJsBody, configStyleBase)

    val reversedPaths =
        listOf(
            "mmrl/",
            "internal/",
            ".adb/",
            ".local/",
            ".config/",
            ".${options.modId.id}/",
            "__root__/"
        )

    return handler@{ path ->
        reversedPaths.forEach {
            if (path.endsWith(it)) return@handler null
        }

        if (path.endsWith("favicon.ico") || path.startsWith("favicon.ico")) return@handler notFoundResponse

        try {
            val file = directory.getCanonicalFileIfChild(path) ?: run {
                Log.e(
                    "webrootPathHandler",
                    "The requested file: %s is outside the mounted directory: %s".format(
                        path,
                        options.webRoot
                    ),
                )
                return@handler notFoundResponse
            }

            if (!file.exists() && options.config.historyFallback) {
                val historyFallbackFile =
                    SuFile(
                        options.webRoot,
                        options.config.historyFallbackFile
                    )

                return@handler historyFallbackFile.asResponse()
            }

            data class Editor(
                val mode: String,
                val name: String,
                val file: SuFile,
            )

            val injections = buildList {
                if (options.isErudaEnabled) {
                    addInjection {
                        appendLine("<script data-internal src=\"https://mui.kernelsu.org/internal/assets/eruda/eruda-editor.js\"></script>")
                        appendLine("<script data-internal type=\"module\">")
                        appendLine("\timport eruda from \"https://mui.kernelsu.org/internal/assets/eruda/eruda.mjs\";")
                        appendLine("\teruda.init();")

                        val editors = listOf(
                            Editor("css", "style", customCssFile),
                            Editor("javascript", "script", customJsFile),
                        )

                        for (editor in editors) {
                            appendLine("\tconst ${editor.name} = erudaEditor({")
                            appendLine("\t\t\tmodId: \"${options.modId.id}\",")
                            appendLine("\t\t\tfile: ${options.modId.sanitizedIdWithFile},")
                            appendLine("\t\t\tfileToEdit: \"${editor.file.path}\",")
                            appendLine("\t\t\tlang: \"${editor.mode}\",")
                            appendLine("\t\t\tname: \"${editor.name}\",")
                            appendLine("\t})")
                            appendLine("eruda.add(${editor.name})")
                        }

                        appendLine("\tconst sheet = new CSSStyleSheet();")
                        appendLine("\tsheet.replaceSync(\".eruda-dev-tools { padding-bottom: ${insets.bottom}px }\");")
                        appendLine("\twindow.eruda.shadowRoot.adoptedStyleSheets.push(sheet)")
                        appendLine("</script>")
                    }
                }

                if (options.config.autoStatusBarsStyle) {
                    addInjection {
                        appendLine("<script data-internal-configurable type=\"module\">")
                        appendLine("$${options.modId.sanitizedId}.setLightStatusBars(!$${options.modId.sanitizedId}.isDarkMode())")
                        appendLine("</script>")
                    }
                }

                configStyleBase.exists {
                    val files = it.listFiles { f -> f.exists() && f.extension == "css" }

                    if (files == null) return@exists

                    files.forEach {
                        addInjection {
                            appendLine("<link data-internal rel=\"stylesheet\" href=\"https://mui.kernelsu.org/.adb/.config/${options.modId.id}/style/${it.name}\" type=\"text/css\" />")
                        }
                    }
                }

                addInjection(InjectionType.BODY) {
                    appendLine("<script data-internal data-internal-dont-use data-mod-id=\"${options.modId}\" data-input-stream=\"${options.modId.sanitizedIdWithFileInputStream}\" src=\"https://mui.kernelsu.org/internal/assets/ext/require.js\"></script>")
                }

                customJsHead.exists {
                    val files = it.listFiles { f -> f.exists() && f.extension == "js" }

                    if (files == null) return@exists

                    files.forEach {
                        addInjection(InjectionType.HEAD) {
                            appendLine("<script data-internal src=\"https://mui.kernelsu.org/.adb/.config/${options.modId.id}/js/head/${it.name}\" type=\"module\"></script>")
                        }
                    }
                }

                customJsBody.exists {
                    val files = it.listFiles { f -> f.exists() && f.extension == "js" }

                    if (files == null) return@exists

                    files.forEach {
                        addInjection(InjectionType.BODY) {
                            appendLine("<script data-internal src=\"https://mui.kernelsu.org/.adb/.config/${options.modId.id}/js/body/${it.name}\" type=\"module\"></script>")
                        }
                    }
                }

                addInjection(insets.cssInject)
            }

            val fileExt = file.extension

            if (fileExt == "html" || fileExt == "htm") {
                return@handler file.asResponse(injections)
            }

            return@handler file.asResponse()
        } catch (e: IOException) {
            Log.e("webrootPathHandler", "Error opening webroot path: $path", e)
            return@handler null
        }
    }
}