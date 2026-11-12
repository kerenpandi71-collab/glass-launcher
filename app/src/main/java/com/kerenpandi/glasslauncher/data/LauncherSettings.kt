package com.kerenpandi.glasslauncher.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.concurrent.TimeUnit

object ThemeModes {
    const val SYSTEM = "system"
    const val DARK = "dark"
    const val LIGHT = "light"
}

object IconShapes {
    const val CIRCLE = "circle"
    const val ROUNDED = "rounded"
    const val SQUIRCLE = "squircle"
    const val SQUARE = "square"
}

data class WeatherNow(
    val temperatureC: Double,
    val code: Int,
    val city: String
)

class LauncherSettings(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var themeMode by mutableStateOf(prefs.getString(KEY_THEME, ThemeModes.DARK) ?: ThemeModes.DARK)
        private set
    var dynamicColor by mutableStateOf(prefs.getBoolean(KEY_DYNAMIC_COLOR, false))
        private set
    var gridColumns by mutableStateOf(prefs.getInt(KEY_GRID_COLUMNS, 4))
        private set
    var iconSizeDp by mutableStateOf(prefs.getInt(KEY_ICON_SIZE, 52))
        private set
    var showLabels by mutableStateOf(prefs.getBoolean(KEY_SHOW_LABELS, true))
        private set
    var iconShape by mutableStateOf(prefs.getString(KEY_ICON_SHAPE, IconShapes.CIRCLE) ?: IconShapes.CIRCLE)
        private set
    var iconPackPackage by mutableStateOf(prefs.getString(KEY_ICON_PACK, "") ?: "")
        private set
    var notificationBadges by mutableStateOf(prefs.getBoolean(KEY_BADGES, true))
        private set
    var showGlance by mutableStateOf(prefs.getBoolean(KEY_GLANCE, true))
        private set
    var weatherCity by mutableStateOf(prefs.getString(KEY_WEATHER_CITY, "") ?: "")
        private set
    var hiddenApps by mutableStateOf(prefs.getStringSet(KEY_HIDDEN, emptySet())?.toSet() ?: emptySet<String>())
        private set

    fun setThemeMode(value: String) {
        themeMode = value
        prefs.edit().putString(KEY_THEME, value).apply()
    }

    fun setDynamicColor(value: Boolean) {
        dynamicColor = value
        prefs.edit().putBoolean(KEY_DYNAMIC_COLOR, value).apply()
    }

    fun setGridColumns(value: Int) {
        gridColumns = value
        prefs.edit().putInt(KEY_GRID_COLUMNS, value).apply()
    }

    fun setIconSizeDp(value: Int) {
        iconSizeDp = value
        prefs.edit().putInt(KEY_ICON_SIZE, value).apply()
    }

    fun setShowLabels(value: Boolean) {
        showLabels = value
        prefs.edit().putBoolean(KEY_SHOW_LABELS, value).apply()
    }

    fun setIconShape(value: String) {
        iconShape = value
        prefs.edit().putString(KEY_ICON_SHAPE, value).apply()
    }

    fun setIconPackPackage(value: String) {
        iconPackPackage = value
        prefs.edit().putString(KEY_ICON_PACK, value).apply()
    }

    fun setNotificationBadges(value: Boolean) {
        notificationBadges = value
        prefs.edit().putBoolean(KEY_BADGES, value).apply()
    }

    fun setShowGlance(value: Boolean) {
        showGlance = value
        prefs.edit().putBoolean(KEY_GLANCE, value).apply()
    }

    fun setWeatherCity(value: String) {
        weatherCity = value.trim()
        prefs.edit().putString(KEY_WEATHER_CITY, weatherCity).apply()
    }

    fun toggleHiddenApp(key: String) {
        val next = hiddenApps.toMutableSet()
        if (!next.add(key)) next.remove(key)
        hiddenApps = next
        prefs.edit().putStringSet(KEY_HIDDEN, next).apply()
    }

    fun cachedWeather(): WeatherNow? {
        val city = prefs.getString(KEY_WX_CITY, null) ?: return null
        val time = prefs.getLong(KEY_WX_TIME, 0L)
        if (System.currentTimeMillis() - time > TimeUnit.MINUTES.toMillis(30)) return null
        return WeatherNow(
            temperatureC = prefs.getFloat(KEY_WX_TEMP, 0f).toDouble(),
            code = prefs.getInt(KEY_WX_CODE, 0),
            city = city
        )
    }

    fun cacheWeather(weather: WeatherNow) {
        prefs.edit()
            .putFloat(KEY_WX_TEMP, weather.temperatureC.toFloat())
            .putInt(KEY_WX_CODE, weather.code)
            .putString(KEY_WX_CITY, weather.city)
            .putLong(KEY_WX_TIME, System.currentTimeMillis())
            .apply()
    }

    fun reload() {
        themeMode = prefs.getString(KEY_THEME, ThemeModes.DARK) ?: ThemeModes.DARK
        dynamicColor = prefs.getBoolean(KEY_DYNAMIC_COLOR, false)
        gridColumns = prefs.getInt(KEY_GRID_COLUMNS, 4)
        iconSizeDp = prefs.getInt(KEY_ICON_SIZE, 52)
        showLabels = prefs.getBoolean(KEY_SHOW_LABELS, true)
        iconShape = prefs.getString(KEY_ICON_SHAPE, IconShapes.CIRCLE) ?: IconShapes.CIRCLE
        iconPackPackage = prefs.getString(KEY_ICON_PACK, "") ?: ""
        notificationBadges = prefs.getBoolean(KEY_BADGES, true)
        showGlance = prefs.getBoolean(KEY_GLANCE, true)
        weatherCity = prefs.getString(KEY_WEATHER_CITY, "") ?: ""
        hiddenApps = prefs.getStringSet(KEY_HIDDEN, emptySet())?.toSet() ?: emptySet()
    }

    private companion object {
        const val PREFS_NAME = "glass_launcher_settings"
        const val KEY_THEME = "theme_mode"
        const val KEY_DYNAMIC_COLOR = "dynamic_color"
        const val KEY_GRID_COLUMNS = "grid_columns"
        const val KEY_ICON_SIZE = "icon_size_dp"
        const val KEY_SHOW_LABELS = "show_labels"
        const val KEY_ICON_SHAPE = "icon_shape"
        const val KEY_ICON_PACK = "icon_pack_package"
        const val KEY_BADGES = "notification_badges"
        const val KEY_GLANCE = "show_glance"
        const val KEY_WEATHER_CITY = "weather_city"
        const val KEY_HIDDEN = "hidden_apps"
        const val KEY_WX_TEMP = "wx_temp"
        const val KEY_WX_CODE = "wx_code"
        const val KEY_WX_CITY = "wx_city"
        const val KEY_WX_TIME = "wx_time"
    }
}
