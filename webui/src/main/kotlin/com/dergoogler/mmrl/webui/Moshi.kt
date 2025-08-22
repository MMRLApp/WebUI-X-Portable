package com.dergoogler.mmrl.webui

import com.dergoogler.mmrl.webui.model.JSONCollectionAdapter
import com.dergoogler.mmrl.webui.model.JSONNullAdapter
import com.dergoogler.mmrl.webui.model.WebUIConfig
import com.dergoogler.mmrl.webui.model.WebUIConfigAdditionalConfig
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.lang.reflect.ParameterizedType

val moshi: Moshi by lazy {
    Moshi.Builder()
        .add(JSONCollectionAdapter())
        .add(JSONNullAdapter())
        .build()
}

@Suppress("ClassName")
internal object __webui__adapters__ {
    val MapType: ParameterizedType =
        Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
    val MapAdapter: JsonAdapter<Map<String, Any?>> = moshi.adapter<Map<String, Any?>>(MapType)
    val ConfigAdapter: JsonAdapter<WebUIConfig> = moshi.adapter(WebUIConfig::class.java)
    val AdditionalConfigAdapter: JsonAdapter<WebUIConfigAdditionalConfig> =
        moshi.adapter(WebUIConfigAdditionalConfig::class.java)
}
