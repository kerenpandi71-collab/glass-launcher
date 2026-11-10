package com.kerenpandi.glasslauncher.data

import android.content.Context

class OrderStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun applyOrder(loaded: List<AppInfo>): List<AppInfo> {
        val saved = loadOrder()
        val byKey = loaded.associateBy { it.stableKey() }
        val ordered = saved.mapNotNull { byKey[it] }
        val savedKeys = saved.toSet()
        val rest = loaded
            .filter { it.stableKey() !in savedKeys }
            .sortedBy { it.label.lowercase() }
        return ordered + rest
    }

    fun saveOrder(apps: List<AppInfo>) {
        prefs.edit()
            .putString(KEY_ORDER, apps.joinToString("|") { it.stableKey() })
            .apply()
    }

    fun applyDockOrder(loaded: List<AppInfo>): List<AppInfo> {
        val byKey = loaded.associateBy { it.stableKey() }
        return loadDockOrder().mapNotNull { byKey[it] }
    }

    fun saveDockOrder(apps: List<AppInfo>) {
        prefs.edit()
            .putString(KEY_DOCK, apps.joinToString("|") { it.stableKey() })
            .apply()
    }

    private fun loadOrder(): List<String> =
        prefs.getString(KEY_ORDER, null)
            ?.split("|")
            ?.filter { it.isNotBlank() }
            .orEmpty()

    private fun loadDockOrder(): List<String> =
        prefs.getString(KEY_DOCK, null)
            ?.split("|")
            ?.filter { it.isNotBlank() }
            .orEmpty()

    companion object {
        private const val PREFS_NAME = "glass_launcher_home"
        private const val KEY_ORDER = "app_order"
        private const val KEY_DOCK = "dock_order"
    }
}
