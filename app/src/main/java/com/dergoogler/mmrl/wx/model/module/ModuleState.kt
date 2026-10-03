package com.dergoogler.mmrl.wx.model.module

import java.io.Serializable

enum class ModuleState : Serializable {
    ENABLE,
    REMOVE,
    DISABLE,
    UPDATE,
    UNAVAILABLE
}