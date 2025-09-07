package com.dergoogler.mmrl.webui.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WXRefreshEventData(
    val isRefreshing: Boolean,
    val isShown: Boolean,
    val isEnabled: Boolean,
)

@JsonClass(generateAdapter = true)
data class WXInsetsEventData(
    val top: Int,
    val bottom: Int,
    val left: Int,
    val right: Int,
) {
    companion object {
        fun Insets.toEventData() = WXInsetsEventData(
            top = top,
            bottom = bottom,
            left = left,
            right = right,
        )
    }
}

@JsonClass(generateAdapter = true)
data class WXKeyboardEventData(
    val height: Int,
    val visible: Boolean,
)

enum class WXEvent {
    WX_ON_BACK,
    WX_ON_RESUME,
    WX_ON_REFRESH,
    WX_ON_PAUSE,
    WX_ON_INSETS,
    WX_ON_KEYBOARD;
}

/**
 * Represents an event handler for JavaScript `window.onmessage` events,
 * with a specific type and optional data.
 *
 * This class is designed to structure and deserialize messages received
 * via the `window.onmessage` mechanism, typically used for communication
 * between different browsing contexts (e.g., iframes, web workers, or different windows).
 * It encapsulates the type of the event and any associated data.
 *
 * The `type` can be a predefined [WXEvent] enum or a custom String identifier.
 * The `data` is generic and can hold any kind of payload associated with the event.
 *
 * @param T The type of the event identifier. This can be either a [WXEvent] enum value or a String.
 * @param D The type of the data associated with the event.
 * @property type The identifier of the event. If it's a [WXEvent], its `name` property will be used
 *                as the string representation of the type. If it's a String, it will be used directly.
 * @property data Optional data payload associated with the event. The type of this data is generic.
 * @throws IllegalArgumentException if the provided `type` is neither a [WXEvent] nor a String
 *                                  when `getType()` is called.
 */
@JsonClass(generateAdapter = true)
data class WXEventHandler<T, D>(
    val type: T,
    val data: D? = null,
) {
    internal fun getType() = when (type) {
        is WXEvent -> type.name
        is String -> type
        else -> throw IllegalArgumentException("Unknown WXEvent type: $type")
    }
}

@JsonClass(generateAdapter = true)
internal data class WXRawEvent(
    val type: String,
    val data: Any? = null,
)
