package com.kerenpandi.glassberry.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object Berry {
    val Body = Color(0xFF17171A)
    val BodyTop = Color(0xFF202026)
    val Key = Color(0xFF26262B)
    val KeySpecial = Color(0xFF3A3A40)
    val Accent = Color(0xFFDF2029)
    val Bezel = Color(0xFF0B0B0D)
    val ScreenOff = Color(0xFF0A0A0C)
    val Ink = Color(0xFFF4F4F5)
    val InkDim = Color(0xFF9C9CA3)
    val ScreenBg = Color(0xFF101012)
}

private val BerryScheme = darkColorScheme(
    primary = Berry.Accent,
    onPrimary = Color.White,
    background = Berry.Body,
    onBackground = Berry.Ink,
    surface = Berry.Key,
    onSurface = Berry.Ink,
    surfaceVariant = Berry.KeySpecial,
    onSurfaceVariant = Berry.InkDim,
)

@Composable
fun BerryTheme(content: @Composable () -> Unit) {
    @Suppress("UNUSED_EXPRESSION")
    isSystemInDarkTheme()
    MaterialTheme(colorScheme = BerryScheme, content = content)
}
