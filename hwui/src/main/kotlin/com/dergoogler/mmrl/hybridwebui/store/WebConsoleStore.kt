package com.dergoogler.mmrl.hybridwebui.store

import com.dergoogler.mmrl.hybridwebui.ConsoleEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Collections
import java.util.LinkedList

class WebConsoleStore(private val maxHistory: Int = 100) {
    private val _console = Collections.synchronizedList(LinkedList<ConsoleEntry>())

    private val _flow = MutableStateFlow<List<ConsoleEntry>>(emptyList())
    val flow: StateFlow<List<ConsoleEntry>> = _flow.asStateFlow()

    fun add(request: ConsoleEntry) {
        synchronized(_console) {
            _console.add(request)
            while (_console.size > maxHistory) {
                _console.removeAt(0)
            }
            // Emit a fresh snapshot to the Flow
            _flow.value = _console.toList()
        }
    }

    fun clear() {
        synchronized(_console) {
            _console.clear()
            _flow.value = emptyList()
        }
    }

    val all: List<ConsoleEntry> get() = synchronized(_console) { _console.toList() }
}
