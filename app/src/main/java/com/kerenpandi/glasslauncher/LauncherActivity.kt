package com.kerenpandi.glasslauncher

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.view.Display
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.kerenpandi.glasslauncher.ui.home.HomeScreen
import com.kerenpandi.glasslauncher.ui.theme.GlassTheme

class LauncherActivity : ComponentActivity() {

    private val viewModel: LauncherViewModel by viewModels()

    private var desktopPresentation: DesktopPresentation? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GlassTheme {
                HomeScreen(
                    viewModel = viewModel,
                    onOpenDesktopMode = { displayId -> openDesktopMode(displayId) },
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
        dismissDesktopMode()
        super.onDestroy()
    }

    private fun openDesktopMode(displayId: Int) {
        if (displayId == Display.DEFAULT_DISPLAY) {
            Toast.makeText(this, "Desktop Mode tidak aktif di layar utama", Toast.LENGTH_SHORT).show()
            return
        }
        val displayManager = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        val display = displayManager.getDisplay(displayId)
        if (display == null) {
            Toast.makeText(this, "Layar tidak ditemukan", Toast.LENGTH_SHORT).show()
            return
        }
        dismissDesktopMode()
        val apps = viewModel.apps
        desktopPresentation = try {
            DesktopPresentation(
                outerContext = this,
                display = display,
                apps = apps,
                onLaunchApp = { app -> viewModel.launch(app, displayId) },
                onDismissed = { desktopPresentation = null }
            ).also { it.show() }
        } catch (e: Exception) {
            launchDesktopModeActivity(display)
        }
    }

    private fun launchDesktopModeActivity(display: Display): DesktopPresentation? {
        try {
            val options = ActivityOptions.makeBasic().setLaunchDisplayId(display.displayId)
            startActivity(
                Intent(this, DesktopModeActivity::class.java)
                    .putExtra(DesktopModeActivity.EXTRA_DISPLAY_ID, display.displayId)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
                options.toBundle()
            )
        } catch (e: Exception) {
            Toast.makeText(this, "Gagal membuka Desktop Mode", Toast.LENGTH_SHORT).show()
        }
        return null
    }

    private fun dismissDesktopMode() {
        try {
            desktopPresentation?.dismiss()
        } catch (e: Exception) {
        }
        desktopPresentation = null
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
