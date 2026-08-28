package com.charlztech.tv

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class AppWindowState {
    var isWindowFullscreen by mutableStateOf(false)
    var isVideoFullscreen by mutableStateOf(false)
    var isSidebarCollapsed by mutableStateOf(false)

    fun toggleWindowFullscreen() {
        isWindowFullscreen = !isWindowFullscreen
    }

    /** Expands video inside the app — does not change native window decorations. */
    fun toggleVideoFullscreen() {
        isVideoFullscreen = !isVideoFullscreen
    }

    fun exitVideoFullscreen() {
        isVideoFullscreen = false
    }
}

val LocalAppWindowState = compositionLocalOf<AppWindowState> {
    error("AppWindowState not provided")
}
