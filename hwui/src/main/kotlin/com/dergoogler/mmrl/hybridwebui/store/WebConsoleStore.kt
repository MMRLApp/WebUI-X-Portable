package com.dergoogler.mmrl.hybridwebui.store

import com.dergoogler.mmrl.hybridwebui.ConsoleEntry
import java.util.Collections
import java.util.LinkedList

class WebConsoleStore(private val maxHistory: Int = 100) {
    // Synchronized wrapper around a LinkedList for O(1) additions
    private val _console = Collections.synchronizedList(LinkedList<ConsoleEntry>())

    /**
     * Thread-safe addition with automated cleanup.
     * Called from WebView background threads.
     */
    fun add(request: ConsoleEntry) {
        synchronized(_console) {
            _console.add(request)

            // Automated Cleanup: Keep the list from growing infinitely
            while (_console.size > maxHistory) {
                _console.removeAt(0)
            }
        }
    }

    val size get(): Int {
        synchronized(_console) {
            return _console.toList().size // Return a snapshot
        }
    }

    val all get(): List<ConsoleEntry> {
        synchronized(_console) {
            return _console.toList() // Return a snapshot
        }
    }

    fun clear() {
        _console.clear()
    }
}