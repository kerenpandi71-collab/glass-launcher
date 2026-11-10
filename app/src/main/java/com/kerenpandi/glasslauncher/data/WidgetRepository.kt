package com.kerenpandi.glasslauncher.data

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.Intent

class WidgetRepository(private val context: Context) {

    val host = AppWidgetHost(context, HOST_ID)
    private val appWidgetManager = AppWidgetManager.getInstance(context)
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun startListening() {
        host.startListening()
    }

    fun stopListening() {
        host.stopListening()
    }

    fun allocateAppWidgetId(): Int = host.allocateAppWidgetId()

    fun deleteAppWidgetId(id: Int) {
        host.deleteAppWidgetId(id)
    }

    fun widgetPickerIntent(appWidgetId: Int): Intent =
        Intent(AppWidgetManager.ACTION_APPWIDGET_PICK).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }

    fun widgetInfo(id: Int): AppWidgetProviderInfo? = appWidgetManager.getAppWidgetInfo(id)

    fun createHostView(id: Int): AppWidgetHostView? = try {
        val info = widgetInfo(id) ?: return null
        host.createView(context, id, info)
    } catch (e: Exception) {
        null
    }

    fun loadWidgetIds(): List<Int> =
        prefs.getStringSet(KEY_WIDGET_IDS, emptySet())
            ?.mapNotNull { it.toIntOrNull() }
            .orEmpty()

    fun saveWidgetIds(ids: List<Int>) {
        prefs.edit()
            .putStringSet(KEY_WIDGET_IDS, ids.map { it.toString() }.toSet())
            .apply()
    }

    companion object {
        const val HOST_ID = 0x474C
        private const val PREFS_NAME = "glass_launcher_widgets"
        private const val KEY_WIDGET_IDS = "widget_ids"
    }
}
