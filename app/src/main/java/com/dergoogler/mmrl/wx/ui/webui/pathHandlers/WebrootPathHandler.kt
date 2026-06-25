package com.dergoogler.mmrl.wx.ui.webui.pathHandlers

import android.util.Log
import android.webkit.WebResourceResponse
import com.dergoogler.mmrl.ext.isNotNullOrBlank
import com.dergoogler.mmrl.wx.model.module.DEFAULT_CSP
import com.dergoogler.mmrl.wx.model.module.autoStatusBarsStyle
import com.dergoogler.mmrl.wx.model.module.caching
import com.dergoogler.mmrl.wx.model.module.cachingMaxAge
import com.dergoogler.mmrl.wx.model.module.contentSecurityPolicy
import com.dergoogler.mmrl.wx.model.module.historyFallback
import com.dergoogler.mmrl.wx.model.module.historyFallbackFile
import com.dergoogler.mmrl.wx.ui.webui.autoOpenEruda
import com.dergoogler.mmrl.wx.ui.webui.enableErudaConsole
import com.dergoogler.mmrl.wx.ui.webui.module
import com.dergoogler.mmrl.wx.ui.webui.sufile
import com.dergoogler.mmrl.wx.ui.webui.util.Injection
import com.dergoogler.mmrl.wx.ui.webui.util.InjectionType
import com.dergoogler.mmrl.wx.ui.webui.util.addInjection
import com.dergoogler.mmrl.wx.ui.webui.util.asResponse
import com.dergoogler.mmrl.wx.ui.webui.util.errorResponse
import dev.mmrlx.nio.SuFile
import dev.mmrlx.utilities.security.ContentSecurityPolicyManager
import dev.mmrlx.webui.PathHandler
import dev.mmrlx.webui.ResponseStatus
import dev.mmrlx.webui.WebUI
import dev.mmrlx.webui.WebUIInsets
import dev.mmrlx.webui.WebUIResourceRequest
import java.io.IOException

private const val DefaultContentSecurityPolicy: String =
    "default-src 'self' data: blob: {domain}; " +
            "script-src 'self' 'unsafe-inline' 'unsafe-eval' {domain}; " +
            "style-src 'self' 'unsafe-inline' {domain}; connect-src *"

class WebrootPathHandler(
    webui: WebUI,
) : PathHandler(webui) {
    override val id = "/"

    private val configBase get() = module.path.configDir
    private val configStyleBase get() = sufile(configBase, "style")
    private val configJsBase get() = sufile(configBase, "js")
    private val customJsHead get() = sufile(configJsBase, "head")
    private val customJsBody get() = sufile(configJsBase, "body")

    private val config get() = module.webrootConfig

    private val directory get() = sufile(sufile(module.path.webrootDir).getCanonicalDirPath())

    init {
        SuFile.createDirectories(customJsHead, customJsBody, configStyleBase)
    }

    private val reversedPaths = listOf(
        "mmrl/", "internal/", ".adb/", ".local/", ".config/", ".${module.id}/", "__root__/"
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
            d.list().map { sufile(d, it) }
                .filter { it.exists() && jsExtensionRegex.matches(it.extension) }.forEach {
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

    override fun handle(
        request: WebUIResourceRequest,
    ): WebResourceResponse {
        val path = request.path.ifEmpty { "index.html" }

        reversedPaths.forEach {
            if (path.endsWith(it)) return response(
                status = ResponseStatus.UNAUTHORIZED,
                data = null
            )
        }

        val contentSecurityPolicy = ContentSecurityPolicyManager(DEFAULT_CSP, baseUri.toString())
        val mergedCsp = contentSecurityPolicy.mergeToString(config.contentSecurityPolicy)

        if (path.endsWith("favicon.ico") || path.startsWith("favicon.ico")) return notFoundResponse

        try {
            val file = directory.getCanonicalFileIfChild(path) ?: run {
                Log.e(
                    "webrootPathHandler",
                    "The requested file: $path is outside the mounted directory: $directory",
                )
                return forbiddenResponse
            }

            if (!file.exists() && config.historyFallback) {
                val fallbackFile = sufile(directory, config.historyFallbackFile)
                val fallbackResponse = fallbackFile.asResponse()

                if (mergedCsp.isNotNullOrBlank()) {
                    fallbackResponse.setResponseHeaders(
                        mapOf(
                            "Content-Security-Policy" to mergedCsp
                        )
                    )
                }

                return fallbackResponse
            }

            val injections = buildList {
                if (settings.enableErudaConsole) {
                    addInjection {
                        appendLine("<script data-internal type=\"module\">")
                        appendLine("\timport eruda from \"https://mui.kernelsu.org/internal/assets/eruda/eruda.mjs\";")
                        appendLine("\teruda.init();")
                        if (settings.autoOpenEruda) {
                            appendLine("\teruda.show();")
                        }
                        appendLine("\tconst sheet = new CSSStyleSheet();")
                        appendLine("\tsheet.replaceSync(\".eruda-dev-tools { padding-bottom: ${insets.bottom}px }\");")
                        appendLine("\twindow.eruda.shadowRoot.adoptedStyleSheets.push(sheet);")
                        appendLine("</script>")
                    }
                }

                if (config.autoStatusBarsStyle) {
                    addInjection {
                        appendLine("<script src=\"https://mui.kernelsu.org/internal/assets/ext/statusbar.js\"></script>")
                    }
                }

//                if (config.backHandler == true && config.backInterceptor == "native") {
//                    addInjection {
//                        appendLine(
//                            """<script>
//                            document.addWNEventListener("backPressed", async () => {
//                                if (await webui.confirm({ title: "Leave?", message: "Are you sure that you want leave this app?" })) {
//                                    webui.exit();
//                                    return;
//                                }
//                            });
//                        </script>""".trimIndent()
//                        )
//                    }
//                }

                configStyleBase.exists {
                    it.listFiles { f -> f.exists() && f.extension == "css" }?.forEach { css ->
                        addInjection {
                            appendLine("<link data-internal rel=\"stylesheet\" href=\"https://mui.kernelsu.org/.adb/.config/${module.id}/style/${css.name}\" type=\"text/css\" />")
                        }
                    }
                }

//                addInjection(InjectionType.BODY) {
//                    appendLine("<script data-internal data-internal-dont-use data-mod-id=\"${modId}\" data-input-stream=\"${modId.sanitizedIdWithFileInputStream}\" src=\"https://mui.kernelsu.org/internal/assets/ext/require.js\"></script>")
//
//                    if (options.config.pullToRefresh && options.config.useNativeRefreshInterceptor && options.config.pullToRefreshHelper) {
//                        appendLine("<script data-internal data-internal-dont-use src=\"https://mui.kernelsu.org/internal/assets/ext/scroll.js\"></script>")
//                    }
//                }

                addScriptInjections(
                    customJsHead,
                    InjectionType.HEAD,
                    "https://mui.kernelsu.org/.adb/.config/${module.id}/js/head"
                )
                addScriptInjections(
                    customJsBody,
                    InjectionType.BODY,
                    "https://mui.kernelsu.org/.adb/.config/${module.id}/js/body"
                )

                addInjection(insets.cssInject)
            }

            val ext = file.extension
            val isHtml = ext == "html" || ext == "htm"
            val response = if (isHtml) file.asResponse(injections) else file.asResponse()

            val headers = mutableMapOf<String, String>()

            if (isHtml && mergedCsp.isNotNullOrBlank()) {
                headers["Content-Security-Policy"] = mergedCsp
            }

            if (ext in staticExtensions && config.caching) {
                headers["Cache-Control"] = "public, max-age=${config.cachingMaxAge}"
            }

            headers["ETag"] = "${file.length()}-${file.lastModified()}"
            response.setResponseHeaders(headers)

            return response
        } catch (e: IOException) {
            console.debugError("Error opening webroot path: $path", e)
            return errorResponse(
                title = "Failed",

                description = {
                    +e.message.toString()
                },
                errorCode = "FILE_NOT_FOUND",
            )
        }
    }
}

val WebUIInsets.cssInject
    get() = buildString {
        val sdg = css
            .replace(Regex("\t"), "\t\t")
            .replace(Regex("\n\\}"), "\n\t}")

        appendLine("<!-- WebUI X: Portable Insets Inject -->")
        appendLine("<style type=\"text/css\">")
        appendLine("\t$sdg")
        appendLine("</style>")
    }
