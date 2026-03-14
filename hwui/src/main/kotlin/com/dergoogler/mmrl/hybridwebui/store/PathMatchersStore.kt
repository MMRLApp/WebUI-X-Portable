package com.dergoogler.mmrl.hybridwebui.store

import com.dergoogler.mmrl.hybridwebui.HybridWebUI
import java.util.Collections
import java.util.LinkedList

class PathMatchersStore(private val maxHistory: Int = 100) {
    // Synchronized wrapper around a LinkedList for O(1) additions
    private val _matchers = Collections.synchronizedList(LinkedList<HybridWebUI.PathMatcher>())

    /**
     * Thread-safe addition with automated cleanup.
     * Called from WebView background threads.
     */
    fun add(request: HybridWebUI.PathMatcher) {
        synchronized(_matchers) {
            _matchers.add(request)

            // Automated Cleanup: Keep the list from growing infinitely
            while (_matchers.size > maxHistory) {
                _matchers.removeAt(0)
            }
        }
    }

    val size get(): Int {
        synchronized(_matchers) {
            return _matchers.toList().size // Return a snapshot
        }
    }

    val all get(): List<HybridWebUI.PathMatcher> {
        synchronized(_matchers) {
            return _matchers.toList() // Return a snapshot
        }
    }

    fun clear() {
        _matchers.clear()
    }
}