package com.kerenpandi.glasslauncher.data

import android.content.Context
import android.net.Uri
import android.provider.CalendarContract
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object WeatherService {

    fun fetch(city: String): WeatherNow? = runCatching {
        val geoUrl = "https://geocoding-api.open-meteo.com/v1/search" +
            "?name=${URLEncoder.encode(city, "UTF-8")}&count=1&language=id&format=json"
        val first = JSONObject(URL(geoUrl).readText())
            .optJSONArray("results")
            ?.optJSONObject(0) ?: return@runCatching null
        val latitude = first.getDouble("latitude")
        val longitude = first.getDouble("longitude")
        val resolvedCity = first.optString("name", city)
        val forecastUrl = "https://api.open-meteo.com/v1/forecast" +
            "?latitude=$latitude&longitude=$longitude&current=temperature_2m,weather_code&timezone=auto"
        val current = JSONObject(URL(forecastUrl).readText()).getJSONObject("current")
        WeatherNow(
            temperatureC = current.getDouble("temperature_2m"),
            code = current.optInt("weather_code", 0),
            city = resolvedCity
        )
    }.getOrNull()

    fun describe(code: Int): String = when (code) {
        0 -> "Cerah"
        1, 2 -> "Cerah berawan"
        3 -> "Berawan"
        45, 48 -> "Berkabut"
        in 51..57 -> "Gerimis"
        in 61..67 -> "Hujan"
        in 71..77 -> "Salju"
        in 80..82 -> "Hujan lebat"
        in 85..86 -> "Salju lebat"
        in 95..99 -> "Badai petir"
        else -> "Cuaca"
    }

    fun emoji(code: Int): String = when (code) {
        0 -> "☀️"
        1, 2 -> "🌤️"
        3 -> "☁️"
        45, 48 -> "🌫️"
        in 51..57 -> "🌦️"
        in 61..67 -> "🌧️"
        in 71..77 -> "🌨️"
        in 80..82 -> "🌧️"
        in 85..86 -> "🌨️"
        in 95..99 -> "⛈️"
        else -> "🌡️"
    }
}

object CalendarEvents {

    fun next(context: Context): String? {
        val uri = CalendarContract.Instances.CONTENT_URI
            .buildUpon()
            .appendPath(System.currentTimeMillis().toString())
            .appendPath((System.currentTimeMillis() + TWO_DAYS_MS).toString())
            .build()
        return runCatching {
            context.contentResolver.query(
                uri,
                arrayOf(
                    CalendarContract.Instances.TITLE,
                    CalendarContract.Instances.BEGIN
                ),
                null,
                null,
                CalendarContract.Instances.BEGIN
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val title = cursor.getString(0) ?: return@use null
                    val begin = cursor.getLong(1)
                    val format = SimpleDateFormat("HH:mm", Locale.getDefault())
                    "${format.format(Date(begin))} · $title"
                } else null
            }
        }.getOrNull()
    }

    private const val TWO_DAYS_MS = 2L * 24L * 60L * 60L * 1000L
}

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
