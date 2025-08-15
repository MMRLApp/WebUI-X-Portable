package com.dergoogler.mmrl.webui.interfaces

import com.sun.jna.Pointer

abstract class WXPointer : Pointer {
    constructor(peer: Long) : super(peer)

    companion object {
        fun Pointer.toWXPointer(): WXPointer = this as WXPointer
        fun WXPointer.toPointer(): Pointer = this as Pointer
    }
}