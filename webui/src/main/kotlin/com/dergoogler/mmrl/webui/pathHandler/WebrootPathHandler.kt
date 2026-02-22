package com.dergoogler.mmrl.webui.pathHandler

import android.util.Log
import android.webkit.WebResourceResponse
import com.dergoogler.mmrl.ext.isNotNullOrBlank
import com.dergoogler.mmrl.hybridwebui.HybridWebUI
import com.dergoogler.mmrl.hybridwebui.HybridWebUIInsets
import com.dergoogler.mmrl.hybridwebui.HybridWebUIResourceRequest
import com.dergoogler.mmrl.platform.file.SuFile
import com.dergoogler.mmrl.platform.file.SuFile.Companion.toSuFile
import com.dergoogler.mmrl.platform.model.ModId.Companion.moduleConfigDir
import com.dergoogler.mmrl.webui.Injection
import com.dergoogler.mmrl.webui.InjectionType
import com.dergoogler.mmrl.webui.addInjection
import com.dergoogler.mmrl.webui.asResponse
import com.dergoogler.mmrl.webui.util.WebUIOptions
import java.io.IOException


class WebrootPathHandler(
    private val options: WebUIOptions,
    private val insets: HybridWebUIInsets,
) : HybridWebUI.BasePathHandler() {

    private val configBase get() = options.modId.moduleConfigDir
    private val configStyleBase get() = SuFile(configBase, "style")
    private val configJsBase get() = SuFile(configBase, "js")
    private val customJsHead get() = SuFile(configJsBase, "head")
    private val customJsBody get() = SuFile(configJsBase, "body")

    private val directory: SuFile get() = SuFile(options.webRoot).getCanonicalDirPath().toSuFile()

    init {
        SuFile.createDirectories(customJsHead, customJsBody, configStyleBase)
    }

    private val reversedPaths = listOf(
        "mmrl/", "internal/", ".adb/", ".local/", ".config/", ".${options.modId.id}/", "__root__/"
    )

    private val jsExtensionRegex = Regex("^[cm]?js$")
    private val staticExtensions =
        listOf("js", "cjs", "mjs", "css", "png", "jpg", "jpeg", "gif", "svg", "woff", "woff2")

    private fun MutableList<Injection>.addScriptInjections(
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

    override fun handle(request: HybridWebUIResourceRequest): WebResourceResponse {
        val path = request.path.ifEmpty { options.indexFile }

        reversedPaths.forEach {
            if (path.endsWith(it)) return response(
                status = Companion.ResponseStatus.UNAUTHORIZED,
                data = null
            )
        }

        if (path.endsWith("favicon.ico") || path.startsWith("favicon.ico")) return notFoundResponse

        try {
            val file = directory.getCanonicalFileIfChild(path) ?: run {
                Log.e(
                    "webrootPathHandler",
                    "The requested file: $path is outside the mounted directory: $directory",
                )
                return forbiddenResponse
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

                return fallbackResponse
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

            return response
        } catch (e: IOException) {
            Log.e("webrootPathHandler", "Error opening webroot path: $path", e)
            return response(
                status = Companion.ResponseStatus.BAD_REQUEST,
                data = null
            )
        }
    }
}

val HybridWebUIInsets.cssInject
    get() = buildString {
        val sdg = css
            .replace(Regex("\t"), "\t\t")
            .replace(Regex("\n\\}"), "\n\t}")

        appendLine("<!-- WebUI X / Hybrid WebUI Insets Inject -->")
        appendLine("<style data-internal data-hybrid type=\"text/css\">")
        appendLine("\t$sdg")
        appendLine("</style>")
    }