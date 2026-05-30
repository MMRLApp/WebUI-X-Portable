package com.dergoogler.mmrl.wx.ui.webui.util

import android.webkit.WebResourceResponse
import com.dergoogler.mmrl.webui.MimeUtil
import dev.mmrlx.nio.SuFile
import dev.mmrlx.nio.inputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.zip.GZIPInputStream

enum class ResponseStatus(val code: Int, val reasonPhrase: String) {
    OK(200, "OK"),
    NOT_FOUND(404, "Not Found"),
    FORBIDDEN(403, "Forbidden"),
}

private fun SuFile.checkStatus(): ResponseStatus {
    if (!exists()) return ResponseStatus.NOT_FOUND

    return ResponseStatus.OK
}

const val encoding = "UTF-8"

val headers
    get() = mapOf(
        "Client-Via" to "MMRL WebUI",
        "Access-Control-Allow-Origin" to "*",
    )


fun InputStream.inject(fromTag: (ByteArray) -> Int, code: String): InputStream {
    val cssBytes = code.toByteArray()

    val outputStream = ByteArrayOutputStream()
    val buffer = ByteArray(1024)
    var bytesRead: Int
    while (this.read(buffer).also { bytesRead = it } != -1) {
        outputStream.write(buffer, 0, bytesRead)
    }

    val modifiedHtml = outputStream.toByteArray()
    val index = fromTag(modifiedHtml)

    return if (index != -1) {
        ByteArrayInputStream(
            modifiedHtml.copyOfRange(
                0,
                index
            ) + cssBytes + modifiedHtml.copyOfRange(index, modifiedHtml.size)
        )
    } else {
        ByteArrayInputStream(modifiedHtml)
    }
}

fun InputStream.headInject(code: String): InputStream = inject(::findHeadTag, code)
fun InputStream.bodyInject(code: String): InputStream = inject(::findBodyTag, code)

private fun findHeadTag(htmlBytes: ByteArray): Int {
    val headTag = "</head>".toByteArray()
    for (i in 0..htmlBytes.size - headTag.size) {
        if (htmlBytes.copyOfRange(i, i + headTag.size).contentEquals(headTag)) {
            return i
        }
    }
    return -1
}

private fun findBodyTag(htmlBytes: ByteArray): Int {
    val bodyTag = "</body>".toByteArray()
    for (i in 0..htmlBytes.size - bodyTag.size) {
        if (htmlBytes.copyOfRange(i, i + bodyTag.size).contentEquals(bodyTag)) {
            return i
        }
    }
    return -1
}

enum class InjectionType {
    HEAD, BODY
}

fun MutableList<Injection>.addInjection(
    type: InjectionType = InjectionType.HEAD,
    code: StringBuilder.() -> Unit,
) = addInjection(buildString(code), type)

fun MutableList<Injection>.addInjection(
    code: String,
    type: InjectionType = InjectionType.HEAD,
) = add(Injection(type, code))

data class Injection(
    val type: InjectionType,
    val code: String,
)

@Throws(IOException::class)
fun SuFile.asResponse(injects: List<Injection>? = null): WebResourceResponse {
    val mimeType = MimeUtil.getMimeFromFileName(path)
    val status = checkStatus()

    val err = WebResourceResponse(
        null,
        encoding,
        status.code,
        status.reasonPhrase,
        headers,
        null
    )

    return when (status) {
        ResponseStatus.FORBIDDEN -> err

        ResponseStatus.OK -> {
            val stream = inputStream() as InputStream
            val modifiedStream = injects?.fold(stream) { currentStream, inject ->
                when (inject.type) {
                    InjectionType.HEAD -> currentStream.headInject(inject.code)
                    InjectionType.BODY -> currentStream.bodyInject(inject.code)
                }
            } ?: stream

            val `is` = handleSvgzStream(modifiedStream)

            WebResourceResponse(
                mimeType,
                encoding,
                status.code,
                status.reasonPhrase,
                headers,
                `is`
            )
        }

        ResponseStatus.NOT_FOUND -> err
    }
}

@Throws(IOException::class)
fun SuFile.handleSvgzStream(
    stream: InputStream,
): InputStream {
    return if (extension == "svgz") GZIPInputStream(stream) else stream
}

fun String.asStyleResponse(): WebResourceResponse {
    val inputStream: InputStream =
        ByteArrayInputStream(this.toByteArray(StandardCharsets.UTF_8))

    return WebResourceResponse(
        "text/css",
        "UTF-8",
        inputStream
    )
}

fun String.asScriptResponse(): WebResourceResponse {
    val inputStream: InputStream =
        ByteArrayInputStream(this.toByteArray(StandardCharsets.UTF_8))

    return WebResourceResponse(
        "text/javascript",
        "UTF-8",
        inputStream
    )
}

val notFoundResponse = WebResourceResponse(
    null,
    encoding,
    ResponseStatus.NOT_FOUND.code,
    ResponseStatus.NOT_FOUND.reasonPhrase,
    headers,
    null
)

val forbiddenResponse = WebResourceResponse(
    null,
    encoding,
    ResponseStatus.FORBIDDEN.code,
    ResponseStatus.FORBIDDEN.reasonPhrase,
    headers,
    null
)