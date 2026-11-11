package com.kerenpandi.glasslauncher.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class LauncherFolder(
    val id: String,
    val name: String,
    val appKeys: List<String>
)

class OrderStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun applyOrder(loaded: List<AppInfo>): List<AppInfo> {
        val saved = loadOrder()
        val byKey = loaded.associateBy { it.stableKey() }
        val ordered = saved.mapNotNull { byKey[it] }
        val savedKeys = saved.toSet()
        val rest = loaded.filter { it.stableKey() !in savedKeys }
            .sortedBy { it.label.lowercase() }
        return ordered + rest
    }

    fun saveOrder(apps: List<AppInfo>) {
        prefs.edit().putString(KEY_ORDER, apps.joinToString("|") { it.stableKey() }).apply()
    }

    fun hasDockOrder(): Boolean = prefs.getString(KEY_DOCK, null) != null

    fun applyDockOrder(loaded: List<AppInfo>): List<AppInfo> {
        val byKey = loaded.associateBy { it.stableKey() }
        return loadDockOrder().mapNotNull { byKey[it] }
    }

    fun saveDockOrder(apps: List<AppInfo>) {
        prefs.edit().putString(KEY_DOCK, apps.joinToString("|") { it.stableKey() }).apply()
    }

    fun loadFolders(loaded: List<AppInfo>): List<LauncherFolder> {
        val validKeys = loaded.map { it.stableKey() }.toSet()
        val raw = prefs.getString(KEY_FOLDERS, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val item = array.optJSONObject(i) ?: continue
                    val keysJson = item.optJSONArray("apps") ?: JSONArray()
                    val keys = buildList {
                        for (j in 0 until keysJson.length()) {
                            val key = keysJson.optString(j)
                            if (key in validKeys) add(key)
                        }
                    }.distinct()
                    if (keys.isNotEmpty()) {
                        add(LauncherFolder(
                            id = item.optString("id", "folder-$i"),
                            name = item.optString("name", "Folder").ifBlank { "Folder" },
                            appKeys = keys
                        ))
                    }
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveFolders(folders: List<LauncherFolder>) {
        val array = JSONArray()
        folders.forEach { folder ->
            val item = JSONObject()
                .put("id", folder.id)
                .put("name", folder.name)
            val keys = JSONArray()
            folder.appKeys.forEach { keys.put(it) }
            item.put("apps", keys)
            array.put(item)
        }
        prefs.edit().putString(KEY_FOLDERS, array.toString()).apply()
    }

    private fun loadOrder(): List<String> =
        prefs.getString(KEY_ORDER, null)?.split("|")?.filter { it.isNotBlank() }.orEmpty()

    private fun loadDockOrder(): List<String> =
        prefs.getString(KEY_DOCK, null)?.split("|")?.filter { it.isNotBlank() }.orEmpty()

    companion object {
        private const val PREFS_NAME = "glass_launcher_home"
        private const val KEY_ORDER = "app_order"
        private const val KEY_DOCK = "dock_order"
        private const val KEY_FOLDERS = "app_folders"
    }
}
