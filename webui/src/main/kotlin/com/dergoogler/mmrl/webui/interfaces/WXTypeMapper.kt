package com.dergoogler.mmrl.webui.interfaces

import com.sun.jna.TypeMapper

interface WXTypeMapper : TypeMapper {
    companion object {
        fun TypeMapper.toWXTypeMapper(): WXTypeMapper = this as WXTypeMapper
        fun WXTypeMapper.toTypeMapper(): TypeMapper = this as TypeMapper
    }
}