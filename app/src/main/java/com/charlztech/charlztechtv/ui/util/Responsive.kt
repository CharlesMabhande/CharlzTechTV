package com.charlztech.charlztechtv.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class WindowSize { Compact, Medium, Expanded }

object Responsive {
    @Composable
    fun windowSize(): WindowSize {
        val width = LocalConfiguration.current.screenWidthDp
        return when {
            width >= 840 -> WindowSize.Expanded
            width >= 600 -> WindowSize.Medium
            else -> WindowSize.Compact
        }
    }

    @Composable
    fun contentPadding(): Dp = when (windowSize()) {
        WindowSize.Compact -> 16.dp
        WindowSize.Medium -> 24.dp
        WindowSize.Expanded -> 32.dp
    }

    @Composable
    fun eventCardWidth(): Dp {
        val width = LocalConfiguration.current.screenWidthDp
        return when {
            width >= 840 -> 280.dp
            width >= 600 -> 260.dp
            else -> (width * 0.72f).dp.coerceIn(200.dp, 260.dp)
        }
    }

    @Composable
    fun providerColumns(): Int = when (windowSize()) {
        WindowSize.Compact -> 1
        WindowSize.Medium -> 2
        WindowSize.Expanded -> 3
    }
}
