package com.dergoogler.mmrl.wx.util

import com.topjohnwu.superuser.Shell

fun createRootShell(globalMnt: Boolean = false): Shell {
    val builder = Shell.Builder.create()
    if (globalMnt) builder.setFlags(Shell.FLAG_MOUNT_MASTER)

    return builder.build("su")
}

inline fun <T> withNewRootShell(globalMnt: Boolean = false, block: Shell.() -> T): T =
    createRootShell(globalMnt).use(block)