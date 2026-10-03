package com.dergoogler.mmrl.wx.ui.webui.util

object Permissions {
    interface Name {
        val permissionName: String
    }


    class Group @PublishedApi internal constructor(
        val permissions: Set<Name>,
    )

    infix fun Name.or(other: Name): Group =
        Group(setOf(this, other))

    infix fun Group.or(other: Name): Group =
        Group(permissions + other)

    enum class MX : Name {
        IO,
        MODINFO;

        override val permissionName: String
            get() = "mx.permission.$name"
    }

    enum class WX : Name {
        IO;

        override val permissionName: String
            get() = "wx.permission.$name"
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