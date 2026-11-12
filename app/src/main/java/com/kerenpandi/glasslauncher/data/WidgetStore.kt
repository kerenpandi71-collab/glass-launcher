package com.kerenpandi.glasslauncher.data

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.Context

object WidgetHosts {
    const val HOST_ID = 1024

    @Volatile
    private var cached: AppWidgetHost? = null

    fun get(context: Context): AppWidgetHost =
        cached ?: synchronized(this) {
            cached ?: AppWidgetHost(context.applicationContext, HOST_ID).also { cached = it }
        }
}

class WidgetStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): List<Int> =
        prefs.getString(KEY_WIDGETS, null)
            ?.split(",")
            ?.mapNotNull { it.trim().toIntOrNull() }
            .orEmpty()

    fun save(ids: List<Int>) {
        prefs.edit().putString(KEY_WIDGETS, ids.joinToString(",")).apply()
    }

    fun prune(context: Context): List<Int> {
        val manager = AppWidgetManager.getInstance(context)
        val valid = load().filter { manager.getAppWidgetInfo(it) != null }
        if (valid.size != load().size) save(valid)
        return valid
    }

    private companion object {
        const val PREFS_NAME = "glass_launcher_widgets"
        const val KEY_WIDGETS = "widget_ids"
    }
}
