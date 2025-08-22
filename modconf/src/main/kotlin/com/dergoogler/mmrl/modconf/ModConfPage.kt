package com.dergoogler.mmrl.modconf

import androidx.compose.runtime.Composable

interface ModConfPage {
    /**
     * Unique identifier for the page.
     */
    val id: String
    val name: String get() = ""
    val description: String get() = ""

    /**
     * The version of the mod configuration. This is used to determine
     * if the configuration needs to be updated.
     */
    val version: Long

    /**
     * Minimum version of the module that this configuration page is for.
     * This is used to determine if the configuration page is compatible with the module.
     * If the module version is less than this value, the configuration page will not be shown.
     * If this value is -1, the configuration page will always be shown.
     */
    val minVersion: Long get() = -1L

    /**
     * The minimum SDK version required for this ModConfPage.
     * Defaults to -1, which means no minimum SDK version is required.
     */
    val minSdk: Int get() = -1

    /**
     * A list of mod IDs that this ModConfPage is associated with or should be applied to.
     *
     * This property defines which modules the ModConf system will run this specific configuration page for.
     * If this list is empty, the ModConf page might be considered global or not specific to any particular mod,
     * depending on the ModConf system's implementation.
     *
     * @return A list of mod IDs.
     */
    val reserved: List<String> get() = emptyList()

    /**
     * This function defines the content to be displayed on the module configuration page.
     * It should be implemented by classes that represent a module configuration page.
     */
    @Composable
    fun Content()
}