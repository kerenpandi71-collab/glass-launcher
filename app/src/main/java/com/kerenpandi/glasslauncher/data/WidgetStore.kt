package com.kerenpandi.glasslauncher.data

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object WidgetHosts {
    const val HOST_ID = 1024

    @Volatile
    private var cached: AppWidgetHost? = null

    fun get(context: Context): AppWidgetHost =
        cached ?: synchronized(this) {
            cached ?: AppWidgetHost(context.applicationContext, HOST_ID).also { cached = it }
        }
}

data class WidgetEntry(
    val id: Int,
    val widthDp: Int,
    val heightDp: Int,
    val page: Int = 0,
    val offsetXDp: Int = 0,
    val stackId: Int = 0
)

class WidgetStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): List<WidgetEntry> {
        val raw = prefs.getString(KEY_WIDGETS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val item = array.optJSONObject(i) ?: continue
                    val id = item.optInt("id", -1)
                    if (id > 0) {
                        add(
                            WidgetEntry(
                                id = id,
                                widthDp = item.optInt("widthDp", DEFAULT_WIDTH),
                                heightDp = item.optInt("heightDp", DEFAULT_HEIGHT),
                                page = item.optInt("page", 0),
                                offsetXDp = item.optInt("offsetXDp", 0),
                                stackId = item.optInt("stackId", 0)
                            )
                        )
                    }
                }
            }
        }.getOrDefault(emptyList())
    }

    fun save(entries: List<WidgetEntry>) {
        val array = JSONArray()
        entries.forEach { entry ->
            array.put(
                JSONObject()
                    .put("id", entry.id)
                    .put("widthDp", entry.widthDp)
                    .put("heightDp", entry.heightDp)
                    .put("page", entry.page)
                    .put("offsetXDp", entry.offsetXDp)
                    .put("stackId", entry.stackId)
            )
        }
        prefs.edit().putString(KEY_WIDGETS, array.toString()).apply()
    }

    fun prune(context: Context): List<WidgetEntry> {
        val manager = AppWidgetManager.getInstance(context)
        val valid = load().filter { manager.getAppWidgetInfo(it.id) != null }
        if (valid.size != load().size) save(valid)
        return valid
    }

    companion object {
        const val DEFAULT_WIDTH = 320
        const val DEFAULT_HEIGHT = 140
        private const val PREFS_NAME = "glass_launcher_widgets"
        private const val KEY_WIDGETS = "widget_entries"
    }
}
