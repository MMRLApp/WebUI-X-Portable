package com.dergoogler.mmrl.hybridwebui.store

import com.dergoogler.mmrl.hybridwebui.ConsoleEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Collections
import java.util.LinkedList

class WebConsoleStore(
    private val maxHistory: Int = 100,
    internal val source: String,
) {
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

fun WebConsoleStore?.info(vararg args: Any?, line: Int = -1) {
    if (this == null) return
    add(ConsoleEntry.info(args, source = source, line = line))
}

fun WebConsoleStore?.warn(vararg args: Any?, line: Int = -1) {
    if (this == null) return
    add(ConsoleEntry.warn(args, source = source, line = line))
}

fun WebConsoleStore?.error(vararg args: Any?, line: Int = -1) {
    if (this == null) return
    add(ConsoleEntry.error(args, source = source, line = line))
}

fun WebConsoleStore?.debug(vararg args: Any?, line: Int = -1) {
    if (this == null) return
    add(ConsoleEntry.debug(args, source = source, line = line))
}

fun WebConsoleStore?.trace(vararg args: Any?, line: Int = -1) {
    if (this == null) return
    add(ConsoleEntry.trace(args, source = source, line = line))
}