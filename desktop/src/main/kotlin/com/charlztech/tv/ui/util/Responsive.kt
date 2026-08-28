package com.charlztech.tv.ui.util

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class WindowSize { Compact, Medium, Expanded }

val LocalWindowWidth = compositionLocalOf { 1280.dp }
val LocalWindowHeight = compositionLocalOf { 800.dp }

@Composable
fun ResponsiveWindowProvider(content: @Composable () -> Unit) {
    BoxWithConstraints {
        CompositionLocalProvider(
            LocalWindowWidth provides maxWidth,
            LocalWindowHeight provides maxHeight
        ) {
            content()
        }
    }
}

object Responsive {
    @Composable
    fun windowSize(): WindowSize {
        val width = LocalWindowWidth.current.value.toInt()
        return when {
            width >= 1100 -> WindowSize.Expanded
            width >= 760 -> WindowSize.Medium
            else -> WindowSize.Compact
        }
    }

    @Composable
    fun contentPadding(): Dp = when (windowSize()) {
        WindowSize.Compact -> 12.dp
        WindowSize.Medium -> 20.dp
        WindowSize.Expanded -> 28.dp
    }

    @Composable
    fun sidebarWidth(collapsed: Boolean): Dp = when {
        collapsed -> 64.dp
        windowSize() == WindowSize.Compact -> 72.dp
        else -> 200.dp
    }

    @Composable
    fun eventCardWidth(): Dp {
        val width = LocalWindowWidth.current.value.toInt()
        return when {
            width >= 1100 -> 260.dp
            width >= 760 -> 240.dp
            else -> 220.dp
        }
    }

    @Composable
    fun providerColumns(): Int = when (windowSize()) {
        WindowSize.Compact -> 1
        WindowSize.Medium -> 2
        WindowSize.Expanded -> 3
    }

    @Composable
    fun useCompactSidebar(): Boolean = LocalWindowWidth.current < 900.dp
}
