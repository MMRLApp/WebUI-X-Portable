package com.dergoogler.mmrl.wx.ui.webui.util

object Permissions {
    interface Name {
        val permissionName: String
    }

    class Group internal constructor(
        val permissions: Set<Name>
    )

    infix fun Name.or(other: Name): Group =
        Group(setOf(this, other))

    infix fun Group.or(other: Name): Group =
        Group(permissions + other)

    enum class MX : Name {
        MODINFO;

        override val permissionName: String
            get() = "mx.permission.$name"
    }

    enum class KSU : Name {
        SHELL,
        PACKAGES,
        MODINFO,
        IO;

        override val permissionName: String
            get() = "kernelsu.permission.$name"
    }
}