package com.kerenpandi.glasslauncher

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.kerenpandi.glasslauncher.data.WidgetRepository
import com.kerenpandi.glasslauncher.ui.home.HomeScreen
import com.kerenpandi.glasslauncher.ui.theme.GlassTheme

class LauncherActivity : ComponentActivity() {

    private val viewModel: LauncherViewModel by viewModels()

    private val widgetRepository: WidgetRepository
        get() = (application as GlassLauncherApp).widgetRepository

    private var pendingWidgetId: Int = -1

    private val widgetPickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val id = pendingWidgetId
            pendingWidgetId = -1
            if (id < 0) return@registerForActivityResult
            val bound = result.resultCode == RESULT_OK && widgetRepository.widgetInfo(id) != null
            if (bound) {
                viewModel.addWidgetId(id)
            } else {
                widgetRepository.deleteAppWidgetId(id)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        widgetRepository.startListening()
        setContent {
            GlassTheme {
                HomeScreen(
                    viewModel = viewModel,
                    onAddWidget = { addWidget() },
                    onOpenDesktopMode = { displayId -> launchDesktopMode(displayId) },
                    onSetWallpaper = { openWallpaperPicker() }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }

    override fun onDestroy() {
        widgetRepository.stopListening()
        super.onDestroy()
    }

    private fun addWidget() {
        val id = widgetRepository.allocateAppWidgetId()
        pendingWidgetId = id
        widgetPickerLauncher.launch(widgetRepository.widgetPickerIntent(id))
    }

    private fun launchDesktopMode(displayId: Int) {
        val options = ActivityOptions.makeBasic().setLaunchDisplayId(displayId)
        startActivity(
            Intent(this, DesktopModeActivity::class.java)
                .putExtra(DesktopModeActivity.EXTRA_DISPLAY_ID, displayId)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
            options.toBundle()
        )
    }

    private fun openWallpaperPicker() {
        try {
            startActivity(
                Intent.createChooser(
                    Intent(Intent.ACTION_SET_WALLPAPER)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    "Pilih wallpaper"
                )
            )
        } catch (e: Exception) {
        }
    }
}
