package com.dergoogler.mmrl.hybridwebui.store

import android.webkit.WebResourceRequest
import java.util.Collections
import java.util.LinkedList

class NetworkRequestStore(private val maxHistory: Int = 100) {
    // Synchronized wrapper around a LinkedList for O(1) additions
    private val _requests = Collections.synchronizedList(LinkedList<WebResourceRequest>())

    /**
     * Thread-safe addition with automated cleanup.
     * Called from WebView background threads.
     */
    fun add(request: WebResourceRequest) {
        synchronized(_requests) {
            _requests.add(request)

            // Automated Cleanup: Keep the list from growing infinitely
            while (_requests.size > maxHistory) {
                _requests.removeAt(0)
            }
        }
    }

    val size get(): Int {
        synchronized(_requests) {
            return _requests.toList().size // Return a snapshot
        }
    }

    val all get(): List<WebResourceRequest> {
        synchronized(_requests) {
            return _requests.toList() // Return a snapshot
        }
    }


    fun clear() {
        _requests.clear()
    }
}