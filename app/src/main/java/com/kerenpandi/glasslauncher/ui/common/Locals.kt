package com.kerenpandi.glasslauncher.ui.common

import androidx.compose.runtime.compositionLocalOf
import com.kerenpandi.glasslauncher.data.LauncherSettings

val LocalLauncherSettings = compositionLocalOf<LauncherSettings?> { null }

val LocalBadges = compositionLocalOf<Map<String, Int>> { emptyMap() }
