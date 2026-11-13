package com.kerenpandi.glasslauncher.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object ThemeModes {
    const val SYSTEM = "system"
    const val DARK = "dark"
    const val LIGHT = "light"
}

object IconShapes {
    const val CIRCLE = "circle"
    const val SQUIRCLE = "squircle"
}

class LauncherSettings(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var themeMode by mutableStateOf(prefs.getString(KEY_THEME, ThemeModes.DARK) ?: ThemeModes.DARK)
        private set
    var dynamicColor by mutableStateOf(prefs.getBoolean(KEY_DYNAMIC_COLOR, false))
        private set
    var gridColumns by mutableStateOf(prefs.getInt(KEY_GRID_COLUMNS, 4))
        private set
    var homeScreens by mutableStateOf(prefs.getInt(KEY_HOME_SCREENS, 4))
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
    var hiddenApps by mutableStateOf(prefs.getStringSet(KEY_HIDDEN, emptySet())?.toSet() ?: emptySet<String>())
        private set

    fun updateThemeMode(value: String) {
        themeMode = value
        prefs.edit().putString(KEY_THEME, value).apply()
    }

    fun updateDynamicColor(value: Boolean) {
        dynamicColor = value
        prefs.edit().putBoolean(KEY_DYNAMIC_COLOR, value).apply()
    }

    fun updateGridColumns(value: Int) {
        gridColumns = value
        prefs.edit().putInt(KEY_GRID_COLUMNS, value).apply()
    }

    fun updateHomeScreens(value: Int) {
        homeScreens = value
        prefs.edit().putInt(KEY_HOME_SCREENS, value).apply()
    }

    fun updateIconSizeDp(value: Int) {
        iconSizeDp = value
        prefs.edit().putInt(KEY_ICON_SIZE, value).apply()
    }

    fun updateShowLabels(value: Boolean) {
        showLabels = value
        prefs.edit().putBoolean(KEY_SHOW_LABELS, value).apply()
    }

    fun updateIconShape(value: String) {
        iconShape = value
        prefs.edit().putString(KEY_ICON_SHAPE, value).apply()
    }

    fun updateIconPackPackage(value: String) {
        iconPackPackage = value
        prefs.edit().putString(KEY_ICON_PACK, value).apply()
    }

    fun updateNotificationBadges(value: Boolean) {
        notificationBadges = value
        prefs.edit().putBoolean(KEY_BADGES, value).apply()
    }

    fun toggleHiddenApp(key: String) {
        val next = hiddenApps.toMutableSet()
        if (!next.add(key)) next.remove(key)
        hiddenApps = next
        prefs.edit().putStringSet(KEY_HIDDEN, next).apply()
    }

    fun reset() {
        prefs.edit().clear().apply()
        reload()
    }

    fun reload() {
        themeMode = prefs.getString(KEY_THEME, ThemeModes.DARK) ?: ThemeModes.DARK
        dynamicColor = prefs.getBoolean(KEY_DYNAMIC_COLOR, false)
        gridColumns = prefs.getInt(KEY_GRID_COLUMNS, 4)
        homeScreens = prefs.getInt(KEY_HOME_SCREENS, 4)
        iconSizeDp = prefs.getInt(KEY_ICON_SIZE, 52)
        showLabels = prefs.getBoolean(KEY_SHOW_LABELS, true)
        iconShape = prefs.getString(KEY_ICON_SHAPE, IconShapes.CIRCLE) ?: IconShapes.CIRCLE
        iconPackPackage = prefs.getString(KEY_ICON_PACK, "") ?: ""
        notificationBadges = prefs.getBoolean(KEY_BADGES, true)
        hiddenApps = prefs.getStringSet(KEY_HIDDEN, emptySet())?.toSet() ?: emptySet()
    }

    private companion object {
        const val PREFS_NAME = "glass_launcher_settings"
        const val KEY_THEME = "theme_mode"
        const val KEY_DYNAMIC_COLOR = "dynamic_color"
        const val KEY_GRID_COLUMNS = "grid_columns"
        const val KEY_HOME_SCREENS = "home_screens"
        const val KEY_ICON_SIZE = "icon_size_dp"
        const val KEY_SHOW_LABELS = "show_labels"
        const val KEY_ICON_SHAPE = "icon_shape"
        const val KEY_ICON_PACK = "icon_pack_package"
        const val KEY_BADGES = "notification_badges"
        const val KEY_HIDDEN = "hidden_apps"
    }
}
