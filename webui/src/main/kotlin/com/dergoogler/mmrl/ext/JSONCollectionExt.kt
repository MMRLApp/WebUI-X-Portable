@file:Suppress("unused")

package com.dergoogler.mmrl.ext

import com.dergoogler.mmrl.platform.file.config.JSONArray
import com.dergoogler.mmrl.platform.file.config.JSONBoolean
import com.dergoogler.mmrl.platform.file.config.JSONCollection
import com.dergoogler.mmrl.platform.file.config.JSONNull
import com.dergoogler.mmrl.platform.file.config.JSONNumber
import com.dergoogler.mmrl.platform.file.config.JSONObject
import com.dergoogler.mmrl.platform.file.config.JSONString

// ==================== JSONBoolean Helpers ====================

fun JSONCollection.toBooleanOrNull(): Boolean? {
    return when (this) {
        is JSONBoolean -> this.boolean
        is JSONString -> this.string.toBooleanStrictOrNull()
        is JSONNumber -> when (this.number) {
            0 -> false
            1 -> true
            else -> null
        }
        else -> null
    }
}

fun JSONCollection.toBoolean(): Boolean {
    return toBooleanOrNull() ?: throw IllegalArgumentException("Cannot convert $this to Boolean")
}

val Boolean.json: JSONBoolean get() = JSONBoolean(this)
fun String.toJsonBooleanOrNull(): JSONBoolean? = toBooleanStrictOrNull()?.let { JSONBoolean(it) }
fun String.toJsonBoolean(): JSONBoolean = toJsonBooleanOrNull() ?: throw IllegalArgumentException("String '$this' is not a valid boolean")

// ==================== JSONString Helpers ====================

fun JSONCollection.toStringOrNull(): String? {
    return when (this) {
        is JSONString -> this.string
        is JSONBoolean -> this.boolean.toString()
        is JSONNumber -> this.number.toString()
        is JSONArray -> this.array.toString()
        is JSONObject -> this.properties.toString()
        is JSONNull -> null
    }
}

fun JSONCollection.toSString(): String {
    return toStringOrNull() ?: throw IllegalArgumentException("Cannot convert $this to String")
}

val String.json: JSONString get() = JSONString(this)
fun Any?.toJsonString(): JSONString = JSONString(this.toString())

// ==================== JSONNumber Helpers ====================

fun JSONCollection.toIntOrNull(): Int? {
    return when (this) {
        is JSONNumber -> this.number.toInt()
        is JSONString -> this.string.toIntOrNull()
        is JSONBoolean -> if (this.boolean) 1 else 0
        else -> null
    }
}

fun JSONCollection.toInt(): Int {
    return toIntOrNull() ?: throw IllegalArgumentException("Cannot convert $this to Int")
}

fun JSONCollection.toLongOrNull(): Long? {
    return when (this) {
        is JSONNumber -> this.number.toLong()
        is JSONString -> this.string.toLongOrNull()
        is JSONBoolean -> if (this.boolean) 1L else 0L
        else -> null
    }
}

fun JSONCollection.toLong(): Long {
    return toLongOrNull() ?: throw IllegalArgumentException("Cannot convert $this to Long")
}

fun JSONCollection.toDoubleOrNull(): Double? {
    return when (this) {
        is JSONNumber -> this.number.toDouble()
        is JSONString -> this.string.toDoubleOrNull()
        is JSONBoolean -> if (this.boolean) 1.0 else 0.0
        else -> null
    }
}

fun JSONCollection.toDouble(): Double {
    return toDoubleOrNull() ?: throw IllegalArgumentException("Cannot convert $this to Double")
}

fun JSONCollection.toFloatOrNull(): Float? {
    return when (this) {
        is JSONNumber -> this.number.toFloat()
        is JSONString -> this.string.toFloatOrNull()
        is JSONBoolean -> if (this.boolean) 1f else 0f
        else -> null
    }
}

fun JSONCollection.toFloat(): Float {
    return toFloatOrNull() ?: throw IllegalArgumentException("Cannot convert $this to Float")
}

val Int.json: JSONNumber get() = JSONNumber(this)
val Long.json: JSONNumber get() = JSONNumber(this)
val Double.json: JSONNumber get() = JSONNumber(this)
val Float.json: JSONNumber get() = JSONNumber(this)

fun Number.toJsonNumber(): JSONNumber = JSONNumber(this)
fun String.toJsonNumberOrNull(): JSONNumber? = toDoubleOrNull()?.let { JSONNumber(it) }
fun String.toJsonNumber(): JSONNumber = toJsonNumberOrNull() ?: throw IllegalArgumentException("String '$this' is not a valid number")

// ==================== JSONArray Helpers ====================

fun JSONCollection.toListOrNull(): List<Any?>? {
    return when (this) {
        is JSONArray -> this.array
        else -> null
    }
}

fun JSONCollection.toList(): List<Any?> {
    return toListOrNull() ?: throw IllegalArgumentException("Cannot convert $this to List")
}

fun <T> JSONCollection.toListOfOrNull(clazz: Class<T>): List<T>? {
    return (this as? JSONArray)?.array?.filterIsInstance(clazz)
}

inline fun <reified T> JSONCollection.toListOf(): List<T> {
    return (this as? JSONArray)?.array?.filterIsInstance<T>() ?: emptyList()
}

val List<Any?>.json: JSONArray get() = JSONArray(this)
fun <T> Array<T>.toJsonArray(): JSONArray = JSONArray(this.toList())
fun Iterable<Any?>.toJsonArray(): JSONArray = JSONArray(this.toList())

// Type-safe array access extensions
inline fun <reified T> JSONArray.mapElements(transform: (T) -> Any?): JSONArray {
    return JSONArray(array.map { if (it is T) transform(it) else it })
}

fun JSONArray.getStringList(): List<String> = array.filterIsInstance<String>()
fun JSONArray.getNumberList(): List<Number> = array.filterIsInstance<Number>()
fun JSONArray.getBooleanList(): List<Boolean> = array.filterIsInstance<Boolean>()
fun JSONArray.getObjectList(): List<JSONObject> = array.filterIsInstance<JSONObject>()
fun JSONArray.getArrayList(): List<JSONArray> = array.filterIsInstance<JSONArray>()
fun JSONArray.getNullList(): List<Any?> = array.filter { it == null }

// ==================== JSONObject Helpers ====================

fun JSONCollection.toMapOrNull(): Map<String, Any?>? {
    return when (this) {
        is JSONObject -> this.properties
        else -> null
    }
}

fun JSONCollection.toMap(): Map<String, Any?> {
    return toMapOrNull() ?: throw IllegalArgumentException("Cannot convert $this to Map")
}

val Map<String, Any?>.json: JSONObject get() = JSONObject(this)
fun Map<String, Any?>.toJsonObject(): JSONObject = JSONObject(this)

// Enhanced object access with default values
fun JSONObject.getString(key: String, defaultValue: String = ""): String = getString(key) ?: defaultValue
fun JSONObject.getNumber(key: String, defaultValue: Number = 0): Number = getNumber(key) ?: defaultValue
fun JSONObject.getBoolean(key: String, defaultValue: Boolean = false): Boolean = getBoolean(key) ?: defaultValue
fun JSONObject.getArray(key: String, defaultValue: JSONArray = JSONArray.EMPTY): JSONArray = getArray(key) ?: defaultValue
fun JSONObject.getObject(key: String, defaultValue: JSONObject = JSONObject.EMPTY): JSONObject = getObject(key) ?: defaultValue

// Safe casting with helpers
fun JSONObject.optString(key: String): String? = getString(key)
fun JSONObject.optNumber(key: String): Number? = getNumber(key)
fun JSONObject.optBoolean(key: String): Boolean? = getBoolean(key)
fun JSONObject.optArray(key: String): JSONArray? = getArray(key)
fun JSONObject.optObject(key: String): JSONObject? = getObject(key)

// ==================== JSONNull Helpers ====================

fun JSONCollection.isNull(): Boolean = this is JSONNull
fun JSONCollection.isNotNull(): Boolean = this !is JSONNull

val Any?.jsonNull: JSONNull? get() = if (this == null) JSONNull else null

// ==================== Generic Conversion Helpers ====================

fun JSONCollection.toAnyOrNull(): Any? {
    return when (this) {
        is JSONBoolean -> this.boolean
        is JSONString -> this.string
        is JSONNumber -> this.number
        is JSONArray -> this.array
        is JSONObject -> this.properties
        is JSONNull -> null
    }
}

inline fun <reified T> JSONCollection.asTypeOrNull(): T? {
    return when (T::class) {
        Boolean::class -> toBooleanOrNull() as? T
        String::class -> toStringOrNull() as? T
        Int::class -> toIntOrNull() as? T
        Long::class -> toLongOrNull() as? T
        Double::class -> toDoubleOrNull() as? T
        Float::class -> toFloatOrNull() as? T
        List::class -> toListOrNull() as? T
        Map::class -> toMapOrNull() as? T
        else -> null
    }
}

// ==================== Builder Pattern Helpers ====================

fun jsonObject(builder: JSONObjectBuilder.() -> Unit): JSONObject {
    return JSONObjectBuilder().apply(builder).build()
}

class JSONObjectBuilder {
    private val map = mutableMapOf<String, Any?>()

    fun put(key: String, value: Any?) {
        map[key] = when (value) {
            is Boolean -> JSONBoolean(value)
            is String -> JSONString(value)
            is Number -> JSONNumber(value)
            is List<*> -> JSONArray(value as List<Any?>)
            is Map<*, *> -> JSONObject(value as Map<String, Any?>)
            else -> value
        }
    }

    fun build(): JSONObject = JSONObject(map.toMap())
}
