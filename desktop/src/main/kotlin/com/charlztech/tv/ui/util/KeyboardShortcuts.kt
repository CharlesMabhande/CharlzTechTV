package com.charlztech.tv.ui.util

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type

fun Modifier.appKeyboardShortcuts(
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onToggleWindowFullscreen: () -> Unit,
    onToggleVideoFullscreen: (() -> Unit)? = null,
    onExitVideoFullscreen: (() -> Unit)? = null,
    isVideoFullscreen: Boolean = false
): Modifier = onPreviewKeyEvent { event ->
    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
    when (event.key) {
        Key.Escape -> {
            if (isVideoFullscreen) {
                onExitVideoFullscreen?.invoke()
                true
            } else false
        }
        Key.F11 -> {
            if (onToggleVideoFullscreen != null && !isVideoFullscreen) {
                onToggleVideoFullscreen()
            } else if (isVideoFullscreen) {
                onExitVideoFullscreen?.invoke()
            } else {
                onToggleWindowFullscreen()
            }
            true
        }
        Key.F5 -> {
            onRefresh()
            true
        }
        Key.Backspace -> {
            if (event.isCtrlPressed) {
                onBack()
                true
            } else false
        }
        else -> false
    }
}
