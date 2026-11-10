package com.kerenpandi.glasslauncher

import android.app.ActivityOptions
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kerenpandi.glasslauncher.data.AppInfo
import com.kerenpandi.glasslauncher.data.AppRepository
import com.kerenpandi.glasslauncher.ui.common.AppIcon
import com.kerenpandi.glasslauncher.ui.common.ClockHeader
import com.kerenpandi.glasslauncher.ui.glass.DesktopBackdrop
import com.kerenpandi.glasslauncher.ui.glass.GlassPanel
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
                    onAppClick = { app -> launchApp(repository, app, displayId) }
                )
            }
        }
    }

    private fun launchApp(repository: AppRepository, app: AppInfo, displayId: Int) {
        val options = ActivityOptions.makeBasic().setLaunchDisplayId(displayId)
        try {
            options.setLaunchWindowingMode(WINDOWING_MODE_FREEFORM)
        } catch (e: Exception) {
        }
        startActivity(repository.launchIntent(app), options.toBundle())
    }

    companion object {
        const val EXTRA_DISPLAY_ID = "extra_display_id"
        private const val WINDOWING_MODE_FREEFORM = 5
    }
}

@Composable
private fun DesktopScreen(
    apps: List<AppInfo>,
    displayId: Int,
    onAppClick: (AppInfo) -> Unit
) {
    var showAllApps by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize()) {
        DesktopBackdrop()
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            Spacer(Modifier.height(28.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = "Desktop Mode • Layar $displayId",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
                ClockHeader(timeFontSize = 28.sp, dateFontSize = 11.sp)
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(6),
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(bottom = 110.dp)
            ) {
                items(apps) { app ->
                    AppIcon(app = app, onClick = { onAppClick(app) })
                }
            }
        }

        Taskbar(
            apps = apps,
            onToggleAll = { showAllApps = !showAllApps },
            onAppClick = onAppClick,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
                .navigationBarsPadding()
        )

        if (showAllApps) {
            AllAppsPanel(
                apps = apps,
                onAppClick = onAppClick,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 100.dp)
            )
        }
    }
}

@Composable
private fun Taskbar(
    apps: List<AppInfo>,
    onToggleAll: () -> Unit,
    onAppClick: (AppInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    GlassPanel(modifier = modifier, shape = RoundedCornerShape(24.dp)) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.18f))
                    .clickable(onClick = onToggleAll),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Menu,
                    contentDescription = "Semua aplikasi",
                    tint = Color.White
                )
            }
            apps.take(6).forEach { app ->
                AppIcon(
                    app = app,
                    showLabel = false,
                    iconSize = 40.dp,
                    onClick = { onAppClick(app) }
                )
            }
        }
    }
}

@Composable
private fun AllAppsPanel(
    apps: List<AppInfo>,
    onAppClick: (AppInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    GlassPanel(
        modifier = modifier
            .fillMaxWidth()
            .height(420.dp),
        shape = RoundedCornerShape(28.dp)
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(6),
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(apps) { app ->
                AppIcon(app = app, onClick = { onAppClick(app) })
            }
        }
    }
}
