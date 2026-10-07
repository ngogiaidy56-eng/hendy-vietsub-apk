package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Neo-Brutalist Dark Studio Palette (CapCut / Resolve inspired)
val ObsidianBg = Color(0xFF0B0D12)
val StudioSurface = Color(0xFF131720)
val StudioSurfaceElevated = Color(0xFF1B202C)
val StudioCardBorder = Color(0xFF283042)
val TimelineTrackBg = Color(0xFF0E1118)

// Accents
val ElectricCyan = Color(0xFF00F0FF)
val ElectricCyanDim = Color(0xFF007A85)
val KineticLime = Color(0xFFCCFF00)
val HotCoral = Color(0xFFFF3366)
val AmberWarning = Color(0xFFFFB800)
val EmeraldReady = Color(0xFF10B981)
val VioletAccent = Color(0xFF9D4EDD)

// Text & Hierarchy
val TextPrimary = Color(0xFFF5F7FA)
val TextSecondary = Color(0xFF9BA4B5)
val TextMuted = Color(0xFF5E6778)

// Subtitle Swatch Presets
val SwatchPalette = listOf(
    "#FFFFFF" to Color(0xFFFFFFFF),
    "#CCFF00" to Color(0xFFCCFF00),
    "#FFE600" to Color(0xFFFFE600),
    "#00F0FF" to Color(0xFF00F0FF),
    "#FF3366" to Color(0xFFFF3366),
    "#FF7A00" to Color(0xFFFF7A00),
    "#10B981" to Color(0xFF10B981),
    "#A855F7" to Color(0xFFA855F7),
    "#000000" to Color(0xFF000000),
    "#131720" to Color(0xFF131720)
)

fun parseHexColor(hex: String, fallback: Color = Color.White): Color {
    return try {
        val clean = hex.trim().removePrefix("#")
        val longVal = when (clean.length) {
            6 -> ("FF$clean").toLong(16)
            8 -> clean.toLong(16)
            else -> return fallback
        }
        Color(longVal)
    } catch (_: Exception) {
        fallback
    }
}
