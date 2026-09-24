package com.dergoogler.mmrl.wx.util

import java.io.Serializable

data class HelpMessage(
    val content: String,
) : Serializable

class BrickException(
    message: String,
    cause: Throwable? = null,
    val help: HelpMessage? = null,
) : Exception(message, cause), Serializable