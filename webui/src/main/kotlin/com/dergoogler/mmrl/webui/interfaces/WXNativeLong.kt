package com.dergoogler.mmrl.webui.interfaces

import com.sun.jna.NativeLong

abstract class WXNativeLong : NativeLong {
    constructor() : super()
    constructor(value: Long) : super(value, false)
    constructor(value: Long, unsigned: Boolean) : super(value, unsigned)
}