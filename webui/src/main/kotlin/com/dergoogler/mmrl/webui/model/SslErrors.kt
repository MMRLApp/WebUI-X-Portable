package com.dergoogler.mmrl.webui.model

enum class SslErrors(val code: Int) {
    SSL_NOTYETVALID(0),
    SSL_EXPIRED(1),
    SSL_IDMISMATCH(2),
    SSL_UNTRUSTED(3),
    SSL_DATE_INVALID(4),
    SSL_INVALID(5);

    companion object {
        fun from(code: Int): SslErrors? {
            return entries.firstOrNull { it.code == code }
        }
    }
}