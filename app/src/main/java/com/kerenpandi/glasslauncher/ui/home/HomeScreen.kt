package com.kerenpandi.glasslauncher.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kerenpandi.glasslauncher.LauncherViewModel
import com.kerenpandi.glasslauncher.data.AppInfo
import com.kerenpandi.glasslauncher.data.stableKey
import com.kerenpandi.glasslauncher.ui.common.AppIcon
import com.kerenpandi.glasslauncher.ui.common.AppMenuItem
import com.kerenpandi.glasslauncher.ui.common.ClockHeader
import com.kerenpandi.glasslauncher.ui.common.GlassIconButton
import com.kerenpandi.glasslauncher.ui.glass.GlassPanel
import com.kerenpandi.glasslauncher.ui.glass.GlassScrim

@Composable
fun HomeScreen(
    viewModel: LauncherViewModel,
    onOpenDesktopMode: (Int) -> Unit,
    onSetWallpaper: () -> Unit
) {
    var settingsOpen by remember { mutableStateOf(false) }
    val apps = viewModel.apps
    val dockApps = viewModel.dockApps
    val appPages = remember(apps) { apps.chunked(LauncherViewModel.APPS_PER_PAGE) }
    val pageCount = appPages.size.coerceAtLeast(1)
    val dockKeys = remember(dockApps) { dockApps.map { it.stableKey() }.toSet() }
    val dockFull = dockApps.size >= LauncherViewModel.DOCK_CAPACITY

    Box(Modifier.fillMaxSize()) {
        GlassScrim()

        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 36.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(Modifier.size(44.dp))
                ClockHeader()
                GlassIconButton(
                    icon = Icons.Default.Settings,
                    contentDescription = "Pengaturan",
                    onClick = { settingsOpen = true }
                )
            }

            val pagerState = rememberPagerState(pageCount = { pageCount })
            HorizontalPager(
                state = pagerState,
                beyondViewportPageCount = 1,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 12.dp)
            ) { page ->
                if (appPages.isEmpty()) {
                    EmptyHomeHint()
                } else {
                    AppGridPage(
                        apps = appPages[page],
                        pageCount = pageCount,
                        dockKeys = dockKeys,
                        dockFull = dockFull,
                        onMovePage = { key, direction -> viewModel.movePage(key, direction) },
                        onAddToDock = { viewModel.addToDock(it) },
                        onAppClick = { viewModel.launch(it) }
                    )
                }
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(pageCount) { index ->
                    Box(
                        Modifier
                            .padding(horizontal = 4.dp)
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(
                                if (pagerState.currentPage == index) Color.White
                                else Color.White.copy(alpha = 0.35f)
                            )
                    )
                }
            }

            Dock(
                apps = dockApps,
                onAppClick = { viewModel.launch(it) },
                onRemoveFromDock = { viewModel.removeFromDock(it) },
                modifier = Modifier.padding(bottom = 20.dp)
            )
        }

        AnimatedVisibility(
            visible = settingsOpen,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            SettingsSheet(
                onOpenDesktopMode = onOpenDesktopMode,
                onSetWallpaper = onSetWallpaper,
                onDismiss = { settingsOpen = false }
            )
        }
    }
}

@Composable
private fun EmptyHomeHint() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        GlassPanel(shape = RoundedCornerShape(24.dp)) {
            Text(
                text = "Tidak ada aplikasi",
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 18.dp)
            )
        }
    }
}

@Composable
fun Dock(
    apps: List<AppInfo>,
    onAppClick: (AppInfo) -> Unit,
    onRemoveFromDock: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    GlassPanel(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (apps.isEmpty()) {
                Text(
                    text = "Tahan ikon di atas lalu pilih \"Tambahkan ke dock\"",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 18.dp)
                )
            } else {
                apps.forEach { app ->
                    val appKey = app.stableKey()
                    AppIcon(
                        app = app,
                        showLabel = false,
                        onClick = { onAppClick(app) },
                        extraMenuItems = listOf(
                            AppMenuItem("Lepas dari dock") { onRemoveFromDock(appKey) }
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun AppGridPage(
    apps: List<AppInfo>,
    pageCount: Int,
    dockKeys: Set<String>,
    dockFull: Boolean,
    onMovePage: (String, Int) -> Unit,
    onAddToDock: (AppInfo) -> Unit,
    onAppClick: (AppInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    val columns = 4
    val rows = apps.chunked(columns)

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        rows.forEach { rowApps ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowApps.forEach { app ->
                    val appKey = app.stableKey()
                    AppIcon(
                        app = app,
                        modifier = Modifier.weight(1f),
                        onClick = { onAppClick(app) },
                        extraMenuItems = buildList {
                            if (!dockFull && appKey !in dockKeys) {
                                add(AppMenuItem("Tambahkan ke dock") { onAddToDock(app) })
                            }
                            if (pageCount > 1) {
                                add(AppMenuItem("Pindah ke halaman sebelumnya") { onMovePage(appKey, -1) })
                                add(AppMenuItem("Pindah ke halaman berikutnya") { onMovePage(appKey, 1) })
                            }
                        }
                    )
                }
                repeat(columns - rowApps.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}
