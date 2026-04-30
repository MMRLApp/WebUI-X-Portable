package com.dergoogler.mmrl.wx.model.module

import java.io.Serializable

enum class ModuleState : Serializable {
    Enable,
    Remove,
    Disable,
    Update,
    Unavailable
}