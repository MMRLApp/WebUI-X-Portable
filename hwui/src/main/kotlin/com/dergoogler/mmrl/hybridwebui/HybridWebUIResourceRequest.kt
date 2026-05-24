package com.dergoogler.mmrl.hybridwebui

import android.net.Uri

/**
 * Represents a resource request originating from a WebUI component.
 *
 * @property method The HTTP method used for the request (e.g., "GET", "POST").
 * @property requestHeaders A map of header keys and values associated with the request.
 * @property url The full [Uri] of the resource being requested.
 * @property path The path portion of the request URL.
 * @property hasGesture Whether the request was associated with a user gesture (e.g., a click).
 * @property isForMainFrame Whether the request is intended for the main frame or a subframe.
 * @property isRedirect Whether the request was redirected by a server.
 */
data class HybridWebUIResourceRequest(
    val method: String,
    val requestHeaders: Map<String, String>,
    val url: Uri,
    val path: String,
    val hasGesture: Boolean,
    val isForMainFrame: Boolean,
    val isRedirect: Boolean,
)