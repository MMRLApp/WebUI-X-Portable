package com.dergoogler.mmrl.wx.model.module

sealed interface ModuleUIState {
    data object Loading : ModuleUIState

    data class Ready(
        val module: Module,
    ) : ModuleUIState

    sealed interface Error : ModuleUIState {
        val message: String
        val error: Exception?

        data class SuInitFailed(
            override val message: String = "Failed to initialize SuFile",
            override val error: Exception? = null,
        ) : Error

        data class MissingAdbPath(
            override val message: String = "Missing adb path",
            override val error: Exception? = null,
        ) : Error

        data class ModuleNotFound(
            override val message: String = "Module not found",
            override val error: Exception? = null,
        ) : Error

        data class InvalidModule(
            override val message: String = "Invalid module",
            override val error: Exception? = null,
        ) : Error
    }
}