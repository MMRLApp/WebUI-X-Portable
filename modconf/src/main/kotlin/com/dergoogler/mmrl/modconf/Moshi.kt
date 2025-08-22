package com.dergoogler.mmrl.modconf

import com.dergoogler.mmrl.modconf.config.ModConfConfig
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.lang.reflect.ParameterizedType

val moshi: Moshi by lazy {
    Moshi.Builder()
        .build()
}

@Suppress("ClassName")
internal object __modconf__adapters__ {
    val MapType: ParameterizedType =
        Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
    val MapAdapter: JsonAdapter<Map<String, Any?>> = moshi.adapter(MapType)
    val ConfigAdapter: JsonAdapter<ModConfConfig> = moshi.adapter(ModConfConfig::class.java)
}
