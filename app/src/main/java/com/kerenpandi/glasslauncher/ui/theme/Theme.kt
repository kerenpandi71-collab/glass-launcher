package com.kerenpandi.glasslauncher.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val GlassDarkColors = darkColorScheme(
    primary = Color(0xFFA8C8FF),
    onPrimary = Color(0xFF08182B),
    secondary = Color(0xFFD0BCFF),
    onSecondary = Color(0xFF1B1230),
    background = Color.Transparent,
    onBackground = Color.White,
    surface = Color(0xFF141C2E),
    onSurface = Color.White,
    surfaceVariant = Color.White.copy(alpha = 0.12f),
    onSurfaceVariant = Color.White.copy(alpha = 0.85f),
    outline = Color.White.copy(alpha = 0.35f)
)

private val GlassLightColors = lightColorScheme(
    primary = Color(0xFF3B6FD4),
    onPrimary = Color.White,
    secondary = Color(0xFF6250B2),
    onSecondary = Color.White,
    background = Color.Transparent,
    onBackground = Color(0xFF16202E),
    surface = Color(0xFFF2F5FB),
    onSurface = Color(0xFF16202E),
    surfaceVariant = Color(0x1416202E),
    onSurfaceVariant = Color(0x9916202E),
    outline = Color(0x6616202E)
)

@Composable
fun GlassTheme(
    themeMode: String = com.kerenpandi.glasslauncher.data.ThemeModes.DARK,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val dark = when (themeMode) {
        com.kerenpandi.glasslauncher.data.ThemeModes.LIGHT -> false
        com.kerenpandi.glasslauncher.data.ThemeModes.SYSTEM -> isSystemInDarkTheme()
        else -> true
    }
    val scheme = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else if (dark) GlassDarkColors else GlassLightColors

    MaterialTheme(
        colorScheme = scheme,
        typography = Typography(),
        content = content
    )
}
