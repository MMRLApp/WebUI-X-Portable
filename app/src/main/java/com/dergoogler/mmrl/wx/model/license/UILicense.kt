package com.dergoogler.mmrl.wx.model.license

data class UiLicense(
    val original: Artifact
) {
    val version: String
        inline get() = original.version

    val spdxLicenses: Set<Artifact.Spdx>
        inline get() = original.spdxLicenses

    val unknownLicenses: Set<Artifact.Unknown>
        inline get() = original.unknownLicenses

    val hasUrl by lazy {
        original.scm.url.isNotEmpty()
    }

    val url by lazy {
        original.scm.url
    }

    val name by lazy {
        original.name.ifEmpty { original.artifactId }
    }

    val dependency by lazy {
        "${original.groupId}:${original.artifactId}"
    }
}