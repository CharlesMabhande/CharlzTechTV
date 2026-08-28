package com.charlztech.charlztechtv.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object AppColors {
    val Background = Color(0xFF070B12)
    val Surface = Color(0xFF111827)
    val SurfaceElevated = Color(0xFF1A2332)
    val SurfaceHighlight = Color(0xFF243044)
    val Primary = Color(0xFF3B9EFF)
    val PrimaryGlow = Color(0xFF1B8CFF)
    val Secondary = Color(0xFF00D4AA)
    val Accent = Color(0xFF7C5CFF)
    val Live = Color(0xFFFF3B30)
    val Upcoming = Color(0xFFFFB020)
    val Ended = Color(0xFF6B7280)
    val TextPrimary = Color(0xFFF3F4F6)
    val TextSecondary = Color(0xFF9CA3AF)
    val GradientStart = Color(0xFF1B8CFF)
    val GradientEnd = Color(0xFF00D4AA)
    val CardOverlay = Color(0x99000000)
}

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)
