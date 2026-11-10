package com.kerenpandi.glasslauncher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.kerenpandi.glasslauncher.data.AppRepository
import com.kerenpandi.glasslauncher.ui.desktop.DesktopScreen
import com.kerenpandi.glasslauncher.ui.theme.GlassTheme

class DesktopModeActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repository = AppRepository(this)
        val apps = repository.loadApps()
        val displayId = intent.getIntExtra(EXTRA_DISPLAY_ID, display?.displayId ?: 0)

        setContent {
            GlassTheme {
                DesktopScreen(
                    apps = apps,
                    displayId = displayId,
                    onAppClick = { app -> repository.launch(this, app, displayId) }
                )
            }
        }
    }

    companion object {
        const val EXTRA_DISPLAY_ID = "extra_display_id"
    }
}
