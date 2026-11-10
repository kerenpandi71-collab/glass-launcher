package com.kerenpandi.glasslauncher.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val GlassColors = darkColorScheme(
    primary = Color(0xFFA8C8FF),
    onPrimary = Color(0xFF08182B),
    secondary = Color(0xFFD0BCFF),
    onSecondary = Color(0xFF1B1230),
    background = Color.Transparent,
    onBackground = Color.White,
    surface = Color.Transparent,
    onSurface = Color.White,
    surfaceVariant = Color.White.copy(alpha = 0.12f),
    onSurfaceVariant = Color.White.copy(alpha = 0.85f),
    outline = Color.White.copy(alpha = 0.35f)
)

@Composable
fun GlassTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = GlassColors,
        typography = Typography(),
        content = content
    )
}
