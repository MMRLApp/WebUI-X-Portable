package com.dergoogler.mmrl.webui.interfaces

import com.sun.jna.Structure

abstract class WXStructure : Structure {
    protected constructor() : super(0)

    protected constructor(mapper: WXTypeMapper) : super(mapper)

    protected constructor(alignType: Int) : super(alignType)

    protected constructor(alignType: Int, mapper: WXTypeMapper) : super(alignType, mapper)

    protected constructor(pointer: WXPointer) : super(pointer)

    protected constructor(pointer: WXPointer, alignType: Int) : super(pointer, alignType)

    protected constructor(pointer: WXPointer, alignType: Int, mapper: WXTypeMapper) : super(
        pointer,
        alignType,
        mapper
    )

    companion object {
        fun Structure.toWXStructure(): WXStructure = this as WXStructure
        fun WXStructure.toStructure(): Structure = this as Structure
    }
}