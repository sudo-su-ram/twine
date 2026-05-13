package com.tether.app.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = TetherPrimary,
    onPrimary = TetherOnPrimary,
    primaryContainer = TetherPrimaryVariant,
    onPrimaryContainer = Color.White,
    secondary = TetherSecondary,
    onSecondary = TetherOnSecondary,
    secondaryContainer = TetherSecondaryVariant,
    onSecondaryContainer = Color.White,
    tertiary = TetherPrimary,
    onTertiary = TetherOnPrimary,
    error = TetherError,
    onError = TetherOnError,
    background = TetherBackground,
    onBackground = TetherOnBackground,
    surface = TetherSurface,
    onSurface = TetherOnSurface
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF93C5FD),
    onPrimary = Color(0xFF1E3A8A),
    primaryContainer = Color(0xFF1E40AF),
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = Color(0xFFF9A8D4),
    onSecondary = Color(0xFF831843),
    secondaryContainer = Color(0xFF9D174D),
    onSecondaryContainer = Color(0xFFFCE7F3),
    tertiary = Color(0xFF93C5FD),
    onTertiary = Color(0xFF1E3A8A),
    error = Color(0xFFFCA5A5),
    onError = Color(0xFF7F1D1D),
    background = Color(0xFF1F2937),
    onBackground = Color(0xFFF3F4F6),
    surface = Color(0xFF111827),
    onSurface = Color(0xFFF3F4F6)
)

@Composable
fun TetherTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
