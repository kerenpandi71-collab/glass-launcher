package com.kerenpandi.glasslauncher.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.kerenpandi.glasslauncher.LauncherViewModel
import com.kerenpandi.glasslauncher.data.AppInfo
import com.kerenpandi.glasslauncher.data.stableKey
import com.kerenpandi.glasslauncher.ui.common.AppIcon
import com.kerenpandi.glasslauncher.ui.common.AppMenuItem
import com.kerenpandi.glasslauncher.ui.common.ClockHeader
import com.kerenpandi.glasslauncher.ui.common.GlassIconButton
import com.kerenpandi.glasslauncher.ui.glass.GlassPanel
import com.kerenpandi.glasslauncher.ui.glass.GlassScrim
import kotlin.math.sqrt

@Composable
fun HomeScreen(
    viewModel: LauncherViewModel,
    onOpenDesktopMode: (Int) -> Unit,
    onSetWallpaper: () -> Unit
) {
    var settingsOpen by remember { mutableStateOf(false) }
    var dragging by remember { mutableStateOf(false) }
    var menuKey by remember { mutableStateOf<String?>(null) }
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
                userScrollEnabled = !dragging,
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
                        menuKey = menuKey,
                        onMenuOpen = { menuKey = it },
                        onMenuClosed = { menuKey = null },
                        onSwap = { keyA, keyB -> viewModel.swapApps(keyA, keyB) },
                        onMovePage = { key, direction -> viewModel.movePage(key, direction) },
                        onAddToDock = { viewModel.addToDock(it) },
                        onAppClick = { viewModel.launch(it) },
                        onDraggingChanged = { dragging = it }
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
                menuKey = menuKey,
                onMenuOpen = { menuKey = it },
                onMenuClosed = { menuKey = null },
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
    menuKey: String?,
    onMenuOpen: (String) -> Unit,
    onMenuClosed: () -> Unit,
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
                        menuOpen = menuKey == appKey,
                        onMenuOpenChange = { open ->
                            if (open) onMenuOpen(appKey) else onMenuClosed()
                        },
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
    menuKey: String?,
    onMenuOpen: (String) -> Unit,
    onMenuClosed: () -> Unit,
    onSwap: (String, String) -> Unit,
    onMovePage: (String, Int) -> Unit,
    onAddToDock: (AppInfo) -> Unit,
    onAppClick: (AppInfo) -> Unit,
    onDraggingChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val itemCenters = remember { mutableStateMapOf<String, Offset>() }
    var draggingKey by remember { mutableStateOf<String?>(null) }
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
                    key(appKey) {
                        DraggableAppIcon(
                            app = app,
                            appKey = appKey,
                            itemCenters = itemCenters,
                            draggingKey = draggingKey,
                            menuOpen = menuKey == appKey,
                            pageCount = pageCount,
                            canAddToDock = !dockFull && appKey !in dockKeys,
                            onMenuOpen = onMenuOpen,
                            onMenuClosed = onMenuClosed,
                            onSwap = onSwap,
                            onMovePage = onMovePage,
                            onAddToDock = onAddToDock,
                            onAppClick = onAppClick,
                            onDraggingChanged = onDraggingChanged,
                            onDragTarget = { targetKey ->
                                draggingKey = targetKey
                            },
                            onDragEnd = {
                                draggingKey = null
                                onDraggingChanged(false)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                repeat(columns - rowApps.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun DraggableAppIcon(
    app: AppInfo,
    appKey: String,
    itemCenters: SnapshotStateMap<String, Offset>,
    draggingKey: String?,
    menuOpen: Boolean,
    pageCount: Int,
    canAddToDock: Boolean,
    onMenuOpen: (String) -> Unit,
    onMenuClosed: () -> Unit,
    onSwap: (String, String) -> Unit,
    onMovePage: (String, Int) -> Unit,
    onAddToDock: (AppInfo) -> Unit,
    onAppClick: (AppInfo) -> Unit,
    onDraggingChanged: (Boolean) -> Unit,
    onDragTarget: (String) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDragging = draggingKey == appKey
    var fingerPos by remember { mutableStateOf(Offset.Zero) }

    DisposableEffect(appKey) {
        onDispose { itemCenters.remove(appKey) }
    }

    Box(
        modifier = modifier
            .onGloballyPositioned { coords ->
                itemCenters[appKey] = coords.localToWindow(
                    Offset(coords.size.width / 2f, coords.size.height / 2f)
                )
            }
            .zIndex(if (isDragging) 1f else 0f)
            .graphicsLayer {
                if (draggingKey == appKey) {
                    val center = itemCenters[appKey] ?: Offset.Zero
                    translationX = fingerPos.x - center.x
                    translationY = fingerPos.y - center.y
                    scaleX = 1.1f
                    scaleY = 1.1f
                }
            }
            .pointerInput(appKey) {
                var moved = false
                detectDragGesturesAfterLongPress(
                    onDragStart = {
                        onMenuClosed()
                        moved = false
                        onDragTarget(appKey)
                        fingerPos = itemCenters[appKey] ?: Offset.Zero
                        onDraggingChanged(true)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val next = fingerPos + dragAmount
                        fingerPos = next
                        var nearestKey: String? = null
                        var nearestDist = Float.MAX_VALUE
                        itemCenters.forEach { (candidateKey, itemCenter) ->
                            val d = distance(next, itemCenter)
                            if (d < nearestDist) {
                                nearestDist = d
                                nearestKey = candidateKey
                            }
                        }
                        if (nearestKey != null && nearestKey != appKey && nearestDist < 120f) {
                            onSwap(appKey, nearestKey)
                            onDragTarget(nearestKey)
                            moved = true
                        }
                    },
                    onDragEnd = {
                        if (!moved) onMenuOpen(appKey)
                        onDragEnd()
                    },
                    onDragCancel = {
                        onDragEnd()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        AppIcon(
            app = app,
            onClick = { onAppClick(app) },
            menuOpen = menuOpen,
            onMenuOpenChange = { open -> if (open) onMenuOpen(appKey) else onMenuClosed() },
            extraMenuItems = buildList {
                if (canAddToDock) {
                    add(AppMenuItem("Tambahkan ke dock") { onAddToDock(app) })
                }
                if (pageCount > 1) {
                    add(AppMenuItem("Pindah ke halaman sebelumnya") { onMovePage(appKey, -1) })
                    add(AppMenuItem("Pindah ke halaman berikutnya") { onMovePage(appKey, 1) })
                }
            }
        )
    }
}

private fun distance(a: Offset, b: Offset): Float {
    val dx = a.x - b.x
    val dy = a.y - b.y
    return sqrt(dx * dx + dy * dy)
}
