package com.dergoogler.mmrl.modconf

import android.content.ContextWrapper

abstract class ModConfModule(kontext: Kontext) : ContextWrapper(kontext), ModConfPage {
    open fun onDestroy() {}
    open fun onStop() {}
    open fun onPause() {}
    open fun onResume() {}
    open fun onPostResume() {}
    open fun onLowMemory() {}
}