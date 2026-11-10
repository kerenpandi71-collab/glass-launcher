package com.kerenpandi.glasslauncher

import android.app.ActivityOptions
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kerenpandi.glasslauncher.data.AppInfo
import com.kerenpandi.glasslauncher.data.AppRepository
import com.kerenpandi.glasslauncher.ui.common.AppIcon
import com.kerenpandi.glasslauncher.ui.common.ClockHeader
import com.kerenpandi.glasslauncher.ui.glass.WallpaperBackdrop
import com.kerenpandi.glasslauncher.ui.theme.GlassTheme

class SecondaryHomeActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repository = AppRepository(this)
        val apps = repository.loadApps()
        val displayId = intent.getIntExtra(EXTRA_DISPLAY_ID, display?.displayId ?: 0)

        setContent {
            GlassTheme {
                SecondaryHomeScreen(
                    apps = apps,
                    displayId = displayId,
                    onAppClick = { app ->
                        val options = ActivityOptions.makeBasic().setLaunchDisplayId(displayId)
                        startActivity(repository.launchIntent(app), options.toBundle())
                    }
                )
            }
        }
    }

    companion object {
        const val EXTRA_DISPLAY_ID = "extra_display_id"
    }
}

@Composable
private fun SecondaryHomeScreen(
    apps: List<AppInfo>,
    displayId: Int,
    onAppClick: (AppInfo) -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        WallpaperBackdrop()
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            Spacer(Modifier.height(40.dp))
            Text(
                text = "Glass Home • Layar $displayId",
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 12.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            ClockHeader(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 8.dp)
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                items(apps) { app ->
                    AppIcon(app = app, onClick = { onAppClick(app) })
                }
            }
        }
    }
}
