package com.dergoogler.mmrl.webui

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonClass
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson

interface JSONCollection

@JsonClass(generateAdapter = true)
data class JSONString(val string: String) : JSONCollection {
    companion object {
        val EMPTY = JSONString("")
        fun String.toJsonString() = JSONString(this)
    }
}

@JsonClass(generateAdapter = true)
data class JSONBoolean(val boolean: Boolean) : JSONCollection {
    companion object {
        val TRUE = JSONBoolean(true)
        val FALSE = JSONBoolean(false)
        fun Boolean.toJsonBoolean() = JSONBoolean(this)
    }
}

@JsonClass(generateAdapter = true)
data class JSONArray(val array: List<String>) : JSONCollection {
    companion object {
        val EMPTY = JSONArray(emptyList())
        fun List<String>.toJsonArray() = JSONArray(this)
    }
}

val moshi: Moshi
    get() = Moshi.Builder()
        .add(JSONCollectionAdapter())
        .build()

class JSONCollectionAdapter {
    @FromJson
    fun fromJson(reader: JsonReader): JSONCollection? {
        return when (reader.peek()) {
            JsonReader.Token.STRING -> JSONString(reader.nextString())
            JsonReader.Token.BEGIN_ARRAY -> {
                val list = mutableListOf<String>()
                reader.beginArray()
                while (reader.hasNext()) {
                    list.add(reader.nextString())
                }
                reader.endArray()
                JSONArray(list)
            }

            JsonReader.Token.BOOLEAN -> JSONBoolean(reader.nextBoolean())

            JsonReader.Token.NULL -> {
                reader.nextNull<Unit>()
                null
            }

            else -> throw JsonDataException("Expected STRING or ARRAY but was ${reader.peek()}")
        }
    }

    @ToJson
    fun toJson(writer: JsonWriter, value: JSONCollection?) {
        when (value) {
            is JSONString -> writer.value(value.string)
            is JSONArray -> {
                writer.beginArray()
                value.array.forEach { writer.value(it) }
                writer.endArray()
            }

            is JSONBoolean -> writer.value(value.boolean)
            null -> writer.nullValue()
            else -> throw JsonDataException("Unknown JSONCollection type: $value")
        }
    }
}
