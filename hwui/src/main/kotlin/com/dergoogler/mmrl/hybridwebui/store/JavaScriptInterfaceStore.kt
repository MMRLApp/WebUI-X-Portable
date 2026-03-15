package com.dergoogler.mmrl.hybridwebui.store

import com.dergoogler.mmrl.hybridwebui.interfaces.JavaScriptInterface
import com.dergoogler.mmrl.hybridwebui.interfaces.JavaScriptInterfaceImplementation
import java.util.Collections
import java.util.LinkedList

class JavaScriptInterfaceStore() {
    // Synchronized wrapper around a LinkedList for O(1) additions
    private val _interfaces =
        Collections.synchronizedList(LinkedList<JavaScriptInterfaceImplementation.Instance>())

    /**
     * Thread-safe addition with automated cleanup.
     * Called from WebView background threads.
     */
    fun add(jsObj: JavaScriptInterfaceImplementation.Instance) {
        synchronized(_interfaces) {
            _interfaces.add(jsObj)
        }
    }

    fun <T> loop(caller: JavaScriptInterface.() -> T): T? {
        synchronized(_interfaces) {
            for (jsInterface in _interfaces.toList()) {
                return@synchronized jsInterface.instance.caller()
            }
        }

        return null
    }

    operator fun plus(jsObj: JavaScriptInterfaceImplementation.Instance) {
        synchronized(_interfaces) {
            _interfaces.add(jsObj)
        }
    }

    operator fun plusAssign(jsObj: JavaScriptInterfaceImplementation.Instance) {
        synchronized(_interfaces) {
            _interfaces.add(jsObj)
        }
    }

    fun find(objectName: String): JavaScriptInterfaceImplementation.Instance? =
        synchronized(_interfaces) {
            return _interfaces.find { it.name == objectName }
        }

    fun has(objectName: String): Boolean = synchronized(_interfaces) {
        return _interfaces.any { it.name == objectName }
    }

    val size
        get(): Int {
            synchronized(_interfaces) {
                return _interfaces.toList().size // Return a snapshot
            }
        }

    val all
        get(): List<JavaScriptInterfaceImplementation.Instance> {
            synchronized(_interfaces) {
                return _interfaces.toList() // Return a snapshot
            }
        }

    fun clear() {
        _interfaces.clear()
    }
}