package com.dergoogler.mmrl.webui.devtools

import org.json.JSONObject

sealed class ResultNode {
    abstract val key: String?
    abstract val depth: Int

    data class Primitive(
        override val key: String?,
        val value: String,
        val kind: PrimitiveKind,
        override val depth: Int,
    ) : ResultNode()

    data class Expandable(
        override val key: String?,
        val label: String,
        val closeToken: String,
        val children: List<ResultNode>,
        override val depth: Int,
        val id: String,
    ) : ResultNode()

    companion object {
        private var _nodeIdCounter = 0

        fun parse(obj: JSONObject, key: String?, depth: Int): ResultNode {
            return when (obj.optString("type")) {
                "expandable" -> {
                    val label = obj.optString("label", "")
                    val closeToken = obj.optString("closeToken", "}")
                    val childArray = obj.optJSONArray("children")
                    val children = mutableListOf<ResultNode>()
                    if (childArray != null) {
                        for (i in 0 until childArray.length()) {
                            val childObj = childArray.getJSONObject(i)
                            val childKey = if (childObj.has("key")) childObj.getString("key") else null
                            children.add(parse(childObj, childKey, depth + 1))
                        }
                    }
                    Expandable(
                        key = key,
                        label = label,
                        closeToken = closeToken,
                        children = children,
                        depth = depth,
                        id = "node_${_nodeIdCounter++}"
                    )
                }
                else -> {
                    val kind = when (obj.optString("kind")) {
                        "string" -> PrimitiveKind.STRING
                        "number" -> PrimitiveKind.NUMBER
                        "boolean" -> PrimitiveKind.BOOLEAN
                        "null", "undefined" -> PrimitiveKind.NULL_UNDEFINED
                        "function" -> PrimitiveKind.FUNCTION
                        else -> PrimitiveKind.OTHER
                    }
                    Primitive(key, obj.optString("value", ""), kind, depth)
                }
            }
        }
    }
}

enum class PrimitiveKind {
    STRING, NUMBER, BOOLEAN, NULL_UNDEFINED, FUNCTION, OTHER
}