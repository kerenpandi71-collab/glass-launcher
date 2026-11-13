package com.kerenpandi.glasslauncher.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object BackupManager {

    private val PREF_FILES = listOf(
        "glass_launcher_home",
        "glass_launcher_settings"
    )

    fun export(context: Context): String {
        val root = JSONObject()
        PREF_FILES.forEach { name ->
            val prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE)
            val obj = JSONObject()
            prefs.all.forEach { (key, value) ->
                when (value) {
                    is String -> obj.put(key, value)
                    is Boolean -> obj.put(key, value)
                    is Int -> obj.put(key, value)
                    is Long -> obj.put(key, value)
                    is Float -> obj.put(key, value.toDouble())
                    is Set<*> -> obj.put(key, JSONArray(value))
                }
            }
            root.put(name, obj)
        }
        return root.toString()
    }

    fun import(context: Context, json: String) {
        val root = JSONObject(json)
        PREF_FILES.forEach { name ->
            val obj = root.optJSONObject(name) ?: return@forEach
            val prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE)
            val editor = prefs.edit().clear()
            obj.keys().forEach { key ->
                when (val value = obj.get(key)) {
                    is String -> editor.putString(key, value)
                    is Boolean -> editor.putBoolean(key, value)
                    is Int -> editor.putInt(key, value)
                    is Long -> editor.putLong(key, value)
                    is Double -> editor.putFloat(key, value.toFloat())
                    is JSONArray -> {
                        val set = mutableSetOf<String>()
                        for (i in 0 until value.length()) set.add(value.optString(i))
                        editor.putStringSet(key, set)
                    }
                }
            }
            editor.apply()
        }
    }
}
