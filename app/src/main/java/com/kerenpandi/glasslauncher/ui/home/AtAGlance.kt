package com.kerenpandi.glasslauncher.ui.home

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.kerenpandi.glasslauncher.data.CalendarEvents
import com.kerenpandi.glasslauncher.data.LauncherSettings
import com.kerenpandi.glasslauncher.data.WeatherService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun AtAGlance(
    settings: LauncherSettings,
    modifier: Modifier = Modifier
) {
    if (!settings.showGlance) return
    val context = LocalContext.current
    var weather by remember { mutableStateOf(settings.cachedWeather()) }
    var nextEvent by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(settings.weatherCity) {
        if (settings.weatherCity.isBlank()) {
            weather = null
            return@LaunchedEffect
        }
        weather?.let { if (it.city.equals(settings.weatherCity, ignoreCase = true)) return@LaunchedEffect }
        val fetched = withContext(Dispatchers.IO) { WeatherService.fetch(settings.weatherCity) }
        fetched?.let {
            weather = it
            settings.cacheWeather(it)
        }
    }

    LaunchedEffect(Unit) {
        nextEvent = withContext(Dispatchers.IO) { CalendarEvents.next(context) }
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val hasCalendar = ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
        weather?.let { wx ->
            GlanceChip(
                text = "${WeatherService.emoji(wx.code)} ${wx.temperatureC.toInt()}°C · ${wx.city}"
            )
        }
        if (hasCalendar) {
            nextEvent?.let { event ->
                if (weather != null) {
                    Text("  ", color = Color.White)
                }
                GlanceChip(text = "📅 $event")
            }
        }
    }
}

@Composable
private fun GlanceChip(text: String) {
    Text(
        text = text,
        color = Color.White,
        fontSize = 13.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.14f))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    )
}
