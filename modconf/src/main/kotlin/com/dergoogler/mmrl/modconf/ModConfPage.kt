package com.dergoogler.mmrl.modconf

import androidx.compose.runtime.Composable
import com.dergoogler.mmrl.modconf.model.TargetPackage

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
     * Specifies the target packages that this ModConfPage is designed for.
     * This property is used by the ModConf system to determine if the ModConfPage
     * is applicable to a specific version of a target application.
     *
     * If this list contains only one `TargetPackage`, the ModConfPage will only be
     * considered compatible with that specific package and its defined version constraints.
     *
     * If the list is empty, it implies that the ModConfPage is not restricted to any
     * particular package, or its applicability is determined by other means.
     *
     * Each `TargetPackage` in the list defines a package name and optional version constraints
     * (minimum and/or maximum version codes) for that package.
     *
     * @return A list of `TargetPackage` objects. Defaults to an empty list.
     * @see TargetPackage
     */
    val targetPackages: List<TargetPackage> get() = emptyList()

    /**
     * This function defines the content to be displayed on the module configuration page.
     * It should be implemented by classes that represent a module configuration page.
     */
    @Composable
    fun Content()
}