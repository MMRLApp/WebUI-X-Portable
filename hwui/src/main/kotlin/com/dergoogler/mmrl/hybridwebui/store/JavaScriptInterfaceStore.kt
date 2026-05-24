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

    /**
     * Executes a block on EVERY registered interface.
     * Used for events like onProgressChanged or onReceivedTitle.
     */
    fun forEach(action: JavaScriptInterface.() -> Unit) {
        synchronized(_interfaces) {
            for (wrapper in _interfaces) {
                wrapper.instance.action()
            }
        }
    }

    /**
     * Finds the first interface that returns a non-null result (usually Boolean).
     * Used for events like onShowFileChooser or onJsAlert.
     */
    fun <T> findFirst(caller: JavaScriptInterface.() -> T?): T? {
        synchronized(_interfaces) {
            for (wrapper in _interfaces) {
                val result = wrapper.instance.caller()
               return result
            }
        }
        return null
    }

    fun dispatchBoolean(caller: JavaScriptInterface.() -> Boolean): Boolean {
        var anyHandled = false
        synchronized(_interfaces) {
            for (wrapper in _interfaces) {
                val result = wrapper.instance.caller()
                if (result) anyHandled = true
            }
        }
        return anyHandled
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