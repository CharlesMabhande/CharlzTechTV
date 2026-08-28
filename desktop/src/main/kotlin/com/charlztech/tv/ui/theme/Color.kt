package com.charlztech.tv.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object AppColors {
    // Windows-style chrome
    val ChromeBackground = Color(0xFFE8E8E8)
    val ToolbarBackground = Color(0xFFF3F3F3)
    val SidebarBackground = Color(0xFFFAFAFA)
    val AddressBarBackground = Color(0xFFFFFFFF)
    val ContentBackground = Color(0xFFFFFFFF)
    val Border = Color(0xFFD1D1D1)

    // Content
    val Background = Color(0xFFF5F5F5)
    val Surface = Color(0xFFFFFFFF)
    val SurfaceElevated = Color(0xFFF0F0F0)
    val SurfaceHighlight = Color(0xFFE5E5E5)
    val Primary = Color(0xFF0078D4)
    val PrimaryGlow = Color(0xFF106EBE)
    val OnPrimary = Color(0xFFFFFFFF)
    val Secondary = Color(0xFF107C10)
    val Accent = Color(0xFF5C2D91)
    val Live = Color(0xFFD13438)
    val Upcoming = Color(0xFFCA5010)
    val Ended = Color(0xFF6B6B6B)
    val TextPrimary = Color(0xFF1A1A1A)
    val TextSecondary = Color(0xFF616161)
    val GradientStart = Color(0xFF0078D4)
    val GradientEnd = Color(0xFF00BCF2)
    val CardOverlay = Color(0x99000000)
}

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(10.dp),
    extraLarge = RoundedCornerShape(12.dp)
)
