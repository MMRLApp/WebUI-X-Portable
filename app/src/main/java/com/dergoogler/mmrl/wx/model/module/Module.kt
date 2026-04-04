package com.dergoogler.mmrl.wx.model.module

import java.io.Serializable

data class Module(
    val id: String,
    val name: String,
    val version: String,
    val versionCode: Int,
    val author: String,
    val description: String,
    val state: ModuleState,
    val path: String,
    val metaModule: Boolean,
    val banner: String?,
    val iconPath: String?,
    val size: Long,
    val lastUpdated: Long,
    val hasWebUI: Boolean = false,
    val paths: ModulePaths,
) : Serializable, Comparable<Module> {
    override fun compareTo(other: Module): Int = id.compareTo(other.id)
}