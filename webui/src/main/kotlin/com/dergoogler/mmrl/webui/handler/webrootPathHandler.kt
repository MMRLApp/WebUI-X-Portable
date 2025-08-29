package com.dergoogler.mmrl.webui.handler

import android.util.Log
import com.dergoogler.mmrl.ext.isNotNullOrBlank
import com.dergoogler.mmrl.platform.PlatformManager
import com.dergoogler.mmrl.platform.file.SuFile
import com.dergoogler.mmrl.platform.file.SuFile.Companion.toSuFile
import com.dergoogler.mmrl.platform.model.ModId.Companion.moduleConfigDir
import com.dergoogler.mmrl.webui.Injection
import com.dergoogler.mmrl.webui.InjectionType
import com.dergoogler.mmrl.webui.PathHandler
import com.dergoogler.mmrl.webui.addInjection
import com.dergoogler.mmrl.webui.asResponse
import com.dergoogler.mmrl.webui.model.Insets
import com.dergoogler.mmrl.webui.notFoundResponse
import com.dergoogler.mmrl.webui.util.WebUIOptions
import java.io.IOException

fun webrootPathHandler(
    options: WebUIOptions,
    insets: Insets,
): PathHandler {

    val configBase = options.modId.moduleConfigDir
    val configStyleBase = SuFile(configBase, "style")
    val configJsBase = SuFile(configBase, "js")
    val customJsHead = SuFile(configJsBase, "head")
    val customJsBody = SuFile(configJsBase, "body")

    val directory = SuFile(options.webRoot).getCanonicalDirPath().toSuFile()
    SuFile.createDirectories(customJsHead, customJsBody, configStyleBase)

    val reversedPaths = listOf(
        "mmrl/", "internal/", ".adb/", ".local/", ".config/", ".${options.modId.id}/", "__root__/"
    )

    val jsExtensionRegex = Regex("^[cm]?js$")
    val staticExtensions =
        listOf("js", "cjs", "mjs", "css", "png", "jpg", "jpeg", "gif", "svg", "woff", "woff2")

    fun MutableList<Injection>.addScriptInjections(
        dir: SuFile,
        type: InjectionType,
        urlBase: String,
    ) {
        dir.exists { d ->
            d.list()?.map { SuFile(d, it) }
                ?.filter { it.exists() && jsExtensionRegex.matches(it.extension) }?.forEach {
                    addInjection(type) {
                        append("<script data-user-extension src=\"$urlBase/${it.name}\"")
                        if (it.extension == "mjs") {
                            append(" type=\"module\"")
                        }
                        append("></script>\n")
                    }
                }
        }
    }

    return handler@{ path ->
        reversedPaths.forEach {
            if (path.endsWith(it)) return@handler null
        }

        if (path.endsWith("favicon.ico") || path.startsWith("favicon.ico")) return@handler notFoundResponse

        try {
            val file = directory.getCanonicalFileIfChild(path) ?: run {
                Log.e(
                    "webrootPathHandler",
                    "The requested file: $path is outside the mounted directory: $directory",
                )
                return@handler notFoundResponse
            }

            if (!file.exists() && options.config.historyFallback) {
                val fallbackFile = SuFile(directory, options.config.historyFallbackFile)
                val fallbackResponse = fallbackFile.asResponse()

                if (options.config.contentSecurityPolicy.isNotNullOrBlank()) {
                    fallbackResponse.setResponseHeaders(
                        mapOf(
                            "Content-Security-Policy" to options.config.contentSecurityPolicy.replace(
                                "{domain}", options.domain.toString()
                            )
                        )
                    )
                }

                return@handler fallbackResponse
            }

            val injections = buildList {
                if (options.isErudaEnabled) {
                    addInjection {
                        appendLine("<script data-internal src=\"https://mui.kernelsu.org/internal/assets/eruda/eruda-editor.js\"></script>")
                        appendLine("<script data-internal type=\"module\">")
                        appendLine("\timport eruda from \"https://mui.kernelsu.org/internal/assets/eruda/eruda.mjs\";")
                        appendLine("\teruda.init();")
                        if (options.isAutoOpenErudaEnabled) {
                            appendLine("\teruda.show();")
                        }
                        appendLine("\tconst sheet = new CSSStyleSheet();")
                        appendLine("\tsheet.replaceSync(\".eruda-dev-tools { padding-bottom: ${insets.bottom}px }\");")
                        appendLine("\twindow.eruda.shadowRoot.adoptedStyleSheets.push(sheet);")
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
                    it.listFiles { f -> f.exists() && f.extension == "css" }?.forEach { css ->
                        addInjection {
                            appendLine("<link data-internal rel=\"stylesheet\" href=\"https://mui.kernelsu.org/.adb/.config/${options.modId.id}/style/${css.name}\" type=\"text/css\" />")
                        }
                    }
                }

                addInjection(InjectionType.BODY) {
                    appendLine("<script data-internal data-internal-dont-use data-mod-id=\"${options.modId}\" data-input-stream=\"${options.modId.sanitizedIdWithFileInputStream}\" src=\"https://mui.kernelsu.org/internal/assets/ext/require.js\"></script>")

                    if (options.config.pullToRefresh && options.config.useNativeRefreshInterceptor && options.config.pullToRefreshHelper) {
                        appendLine("<script data-internal data-internal-dont-use src=\"https://mui.kernelsu.org/internal/assets/ext/scroll.js\"></script>")
                    }
                }

                addScriptInjections(
                    customJsHead,
                    InjectionType.HEAD,
                    "https://mui.kernelsu.org/.adb/.config/${options.modId.id}/js/head"
                )
                addScriptInjections(
                    customJsBody,
                    InjectionType.BODY,
                    "https://mui.kernelsu.org/.adb/.config/${options.modId.id}/js/body"
                )

                addInjection(insets.cssInject)
            }
            PlatformManager
            val ext = file.extension
            val isHtml = ext == "html" || ext == "htm"
            val response = if (isHtml) file.asResponse(injections) else file.asResponse()

            val headers = mutableMapOf<String, String>()

            if (isHtml && options.config.contentSecurityPolicy.isNotNullOrBlank()) {
                headers["Content-Security-Policy"] = options.config.contentSecurityPolicy.replace(
                    "{domain}", options.domain.toString()
                )
            }

            if (ext in staticExtensions && options.config.caching) {
                headers["Cache-Control"] = "public, max-age=${options.config.cachingMaxAge}"
            }

            headers["ETag"] = "${file.length()}-${file.lastModified()}"
            response.setResponseHeaders(headers)

            return@handler response
        } catch (e: IOException) {
            Log.e("webrootPathHandler", "Error opening webroot path: $path", e)
            return@handler null
        }
    }
}
