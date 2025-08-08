package com.dergoogler.mmrl.wx.model.license

import kotlinx.serialization.Serializable

@Serializable
data class Artifact(
    val groupId: String,
    val artifactId: String,
    val version: String,
    val name: String = "",
    val spdxLicenses: Set<Spdx> = emptySet(),
    val unknownLicenses: Set<Unknown> = emptySet(),
    val scm: Scm = Scm()
) {
    @Serializable
    class Spdx(
        val identifier: String,
        val name: String,
        val url: String
    )

    @Serializable
    class Unknown(
        val name: String,
        val url: String
    )

    @Serializable
    class Scm(
        val url: String = ""
    )
}