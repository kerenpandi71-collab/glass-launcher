package com.kerenpandi.glasslauncher

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.mutableStateListOf
import com.kerenpandi.glasslauncher.ui.home.HomeScreen
import com.kerenpandi.glasslauncher.ui.theme.GlassTheme

class LauncherActivity : ComponentActivity() {
    private val viewModel: LauncherViewModel by viewModels()
    private lateinit var appWidgetHost: AppWidgetHost
    private val widgetIds = mutableStateListOf<Int>()
    private val preferences by lazy { getSharedPreferences("launcher_widgets", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        appWidgetHost = AppWidgetHost(this, APP_WIDGET_HOST_ID)
        preferences.getString("widget_ids", "").orEmpty()
            .split(",").mapNotNull { it.toIntOrNull() }
            .filter { AppWidgetManager.getInstance(this).getAppWidgetInfo(it) != null }
            .forEach { widgetIds.add(it) }

        setContent {
            GlassTheme {
                HomeScreen(
                    viewModel = viewModel,
                    onSetWallpaper = { openWallpaperPicker() },
                    appWidgetHost = appWidgetHost,
                    widgetIds = widgetIds.toList(),
                    onAddWidget = { openWidgetPicker() },
                    onRemoveWidget = { removeWidget(it) }
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        runCatching { appWidgetHost.startListening() }
    }

    override fun onStop() {
        runCatching { appWidgetHost.stopListening() }
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }

    @Deprecated("Deprecated in Android, retained for AppWidget picker compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_WIDGET_PICK && requestCode != REQUEST_WIDGET_CONFIGURE) return

        val id = data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, pendingWidgetId)
            ?: pendingWidgetId
        if (id == AppWidgetManager.INVALID_APPWIDGET_ID) return

        if (requestCode == REQUEST_WIDGET_PICK && resultCode == RESULT_OK) {
            val info = AppWidgetManager.getInstance(this).getAppWidgetInfo(id)
            val configure = info?.configure
            if (configure != null) {
                pendingWidgetId = id
                runCatching {
                    startActivityForResult(
                        Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE)
                            .setComponent(configure)
                            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id),
                        REQUEST_WIDGET_CONFIGURE
                    )
                }.onFailure { removeWidgetId(id) }
            } else {
                saveWidget(id)
            }
        } else if (requestCode == REQUEST_WIDGET_CONFIGURE && resultCode == RESULT_OK) {
            saveWidget(id)
            pendingWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
        } else {
            removeWidgetId(id)
            pendingWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
        }
    }

    private fun openWidgetPicker() {
        val id = appWidgetHost.allocateAppWidgetId()
        pendingWidgetId = id
        runCatching {
            startActivityForResult(
                Intent(AppWidgetManager.ACTION_APPWIDGET_PICK)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id),
                REQUEST_WIDGET_PICK
            )
        }.onFailure { removeWidgetId(id) }
    }

    private fun saveWidget(id: Int) {
        if (id !in widgetIds) widgetIds.add(id)
        persistWidgetIds()
        pendingWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    }

    private fun removeWidget(id: Int) {
        widgetIds.remove(id)
        persistWidgetIds()
        removeWidgetId(id)
    }

    private fun removeWidgetId(id: Int) {
        runCatching { appWidgetHost.deleteAppWidgetId(id) }
        widgetIds.remove(id)
        persistWidgetIds()
    }

    private fun persistWidgetIds() {
        preferences.edit().putString("widget_ids", widgetIds.joinToString(",")).apply()
    }

    private fun openWallpaperPicker() {
        runCatching {
            startActivity(
                Intent.createChooser(
                    Intent(Intent.ACTION_SET_WALLPAPER).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    "Pilih wallpaper"
                )
            )
        }
    }

    companion object {
        private const val APP_WIDGET_HOST_ID = 0x474C
        private const val REQUEST_WIDGET_PICK = 5101
        private const val REQUEST_WIDGET_CONFIGURE = 5102
        private var pendingWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    }
}
