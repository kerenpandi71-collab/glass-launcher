package com.kerenpandi.glasslauncher

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import com.kerenpandi.glasslauncher.data.WidgetHosts
import com.kerenpandi.glasslauncher.ui.common.LocalBadges
import com.kerenpandi.glasslauncher.ui.common.LocalLauncherSettings
import com.kerenpandi.glasslauncher.ui.home.HomeScreen
import com.kerenpandi.glasslauncher.ui.theme.GlassTheme

class LauncherActivity : ComponentActivity() {
    private val viewModel: LauncherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CompositionLocalProvider(
                LocalLauncherSettings provides viewModel.settings,
                LocalBadges provides viewModel.badges
            ) {
                GlassTheme(
                    themeMode = viewModel.settings.themeMode,
                    dynamicColor = viewModel.settings.dynamicColor
                ) {
                    HomeScreen(
                        viewModel = viewModel,
                        onSetWallpaper = { openWallpaperPicker() }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }

    override fun onStart() {
        super.onStart()
        runCatching { WidgetHosts.get(this).startListening() }
    }

    override fun onStop() {
        runCatching { WidgetHosts.get(this).stopListening() }
        super.onStop()
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
}
