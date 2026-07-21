package com.example.tempo.theme

import androidx.compose.ui.graphics.Color

// Premium Dark Theme Palette for Tempo
val DarkBackground = Color(0xFF0F172A)     // Deep slate navy
val DarkSurface = Color(0xFF1E293B)        // Elevated card surface
val DarkSurfaceVariant = Color(0xFF334155) // Border / subtle card fill
val PrimaryIndigo = Color(0xFF6366F1)      // Electric Indigo accent
val PrimaryViolet = Color(0xFF8B5CF6)      // Deep Violet accent
val SecondaryEmerald = Color(0xFF10B981)   // Vibrant Emerald accent
val AccentRose = Color(0xFFF43F5E)         // Rose red highlight
val AccentAmber = Color(0xFFF59E0B)        // Warm Amber accent
val AccentCyan = Color(0xFF06B6D4)         // Cyan glow

val TextPrimary = Color(0xFFF8FAFC)        // Pure crisp text
val TextSecondary = Color(0xFF94A3B8)      // Muted slate text
val FavoriteGold = Color(0xFFFBBF24)       // Star favorite yellow

// Preset Palette Colors for Habit Creation
val PresetHabitColors = listOf(
    "#6366F1", // Indigo
    "#10B981", // Emerald
    "#F59E0B", // Amber
    "#EC4899", // Pink
    "#8B5CF6", // Purple
    "#06B6D4", // Cyan
    "#F43F5E", // Rose
    "#3B82F6"  // Blue
)

fun parseHexColor(colorHex: String): Color {
    return try {
        val cleaned = colorHex.replace("#", "")
        val longVal = cleaned.toLong(16)
        if (cleaned.length == 6) {
            Color(0xFF000000 or longVal)
        } else {
            Color(longVal)
        }
    } catch (e: Exception) {
        PrimaryIndigo
    }
}
