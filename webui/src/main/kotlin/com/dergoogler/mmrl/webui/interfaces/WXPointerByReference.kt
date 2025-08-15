package com.dergoogler.mmrl.webui.interfaces

import com.sun.jna.Pointer
import com.sun.jna.ptr.PointerByReference

abstract class WXPointerByReference : PointerByReference {
    constructor() : super(null)
    constructor(value: Pointer) : super(value)
}