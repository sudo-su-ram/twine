package com.tether.app.ui.theme

import androidx.compose.ui.graphics.Color

// Primary brand colors
val TetherPrimary = Color(0xFF6366F1) // Indigo
val TetherPrimaryVariant = Color(0xFF4F46E5)
val TetherSecondary = Color(0xFFF472B6) // Pink
val TetherSecondaryVariant = Color(0xFFEC4899)

// Background colors
val TetherBackground = Color(0xFFFFF5E1) // Warm cream
val TetherSurface = Color(0xFFFFFFFF)
val TetherError = Color(0xFFB00020)

// Text colors
val TetherOnPrimary = Color(0xFFFFFFFF)
val TetherOnSecondary = Color(0xFFFFFFFF)
val TetherOnBackground = Color(0xFF1F2937)
val TetherOnSurface = Color(0xFF1F2937)
val TetherOnError = Color(0xFFFFFFFF)

// Drawing palette colors
val DrawingColors = listOf(
    Color(0xFFEF4444), // Red
    Color(0xFFF97316), // Orange
    Color(0xFFF59E0B), // Amber
    Color(0xFF10B981), // Green
    Color(0xFF06B6D4), // Cyan
    Color(0xFF3B82F6), // Blue
    Color(0xFF6366F1), // Indigo
    Color(0xFFEC4899), // Pink
    Color(0xFF78350F), // Brown (custom slot)
    Color(0xFF000000)  // Black
)

// Gradient presets
data class GradientPreset(val start: Color, val end: Color)

val GradientPresets = listOf(
    GradientPreset(Color(0xFF667EEA), Color(0xFF764BA2)), // Purple to deep purple
    GradientPreset(Color(0xFFF093FB), Color(0xFFF5576C)), // Pink gradient
    GradientPreset(Color(0xFF4FACFE), Color(0xFF00F2FE)), // Blue to cyan
    GradientPreset(Color(0xFF43E97B), Color(0xFF38F9D7)), // Green gradient
    GradientPreset(Color(0xFFFF9A9E), Color(0xFFFECFEF)), // Warm pink
    GradientPreset(Color(0xFFFA709A), Color(0xFFFEE140)), // Sunset
    GradientPreset(Color(0xFFA8EDFE), Color(0xFFFED6E3)), // Soft pastel
    GradientPreset(Color(0xFFD299C2), Color(0xFFFEF9D7))  // Lavender to cream
)

// Solid color presets
val SolidColorPresets = listOf(
    Color(0xFFFFF5E1), // Warm cream
    Color(0xFFF0F4F8), // Light gray-blue
    Color(0xFFFFF0F5), // Lavender blush
    Color(0xFFF0FFF4), // Honeydew
    Color(0xFFFFFBEB), // Light yellow
    Color(0xFFEFF6FF), // Light blue
    Color(0xFFFDF2F8), // Light pink
    Color(0xFFF3E8FF), // Light purple
    Color(0xFFDCFCE7), // Light green
    Color(0xFFFEF3C7), // Light amber
    Color(0xFFDBEAFE), // Light indigo
    Color(0xFFE0E7FF), // Periwinkle
    Color(0xFFCEE7F8), // Sky tint
    Color(0xFFFFE4E1), // Rose tint
    Color(0xFFE8F5E9), // Mint
    Color(0xFFFFF8E1), // Warm white
    Color(0xFF2D3748), // Dark gray (for dark mode support)
    Color(0xFF1A202C)  // Near black (for dark mode)
)
