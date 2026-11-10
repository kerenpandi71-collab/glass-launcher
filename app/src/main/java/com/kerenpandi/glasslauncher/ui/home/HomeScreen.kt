package com.kerenpandi.glasslauncher.ui.home

import android.os.Bundle
import android.widget.FrameLayout
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.kerenpandi.glasslauncher.GlassLauncherApp
import com.kerenpandi.glasslauncher.LauncherViewModel
import com.kerenpandi.glasslauncher.data.AppInfo
import com.kerenpandi.glasslauncher.ui.common.AppIcon
import com.kerenpandi.glasslauncher.ui.common.ClockHeader
import com.kerenpandi.glasslauncher.ui.common.GlassIconButton
import com.kerenpandi.glasslauncher.ui.glass.GlassPanel
import com.kerenpandi.glasslauncher.ui.glass.GlassScrim

private const val APPS_PER_PAGE = 24

@Composable
fun HomeScreen(
    viewModel: LauncherViewModel,
    onAddWidget: () -> Unit,
    onOpenDesktopMode: (Int) -> Unit,
    onSetWallpaper: () -> Unit
) {
    var settingsOpen by remember { mutableStateOf(false) }
    val apps = viewModel.apps
    val appPages = remember(apps) { apps.chunked(APPS_PER_PAGE) }
    val pageCount = appPages.size + 1

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
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 12.dp)
            ) { page ->
                when {
                    page == 0 -> WidgetPage(
                        widgetIds = viewModel.widgetIds,
                        onRemove = { id -> viewModel.removeWidgetId(id) },
                        onResize = { id, w, h -> viewModel.saveWidgetSize(id, w, h) }
                    )
                    else -> AppGridPage(
                        apps = appPages[page - 1],
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
                apps = apps.take(4),
                onAppClick = { viewModel.launch(it) },
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
                onAddWidget = onAddWidget,
                onOpenDesktopMode = onOpenDesktopMode,
                onSetWallpaper = onSetWallpaper,
                onDismiss = { settingsOpen = false }
            )
        }
    }
}

@Composable
fun Dock(
    apps: List<AppInfo>,
    onAppClick: (AppInfo) -> Unit,
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
            apps.forEach { app ->
                AppIcon(app = app, showLabel = false, onClick = { onAppClick(app) })
            }
        }
    }
}

@Composable
fun WidgetPage(
    widgetIds: List<Int>,
    onRemove: (Int) -> Unit,
    onResize: (Int, Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (widgetIds.isEmpty()) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            GlassPanel(shape = RoundedCornerShape(24.dp)) {
                Text(
                    text = "Belum ada widget.\nBuka Pengaturan untuk menambah widget.",
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 18.dp)
                )
            }
        }
        return
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        items(widgetIds, key = { it }) { id ->
            WidgetCard(
                id = id,
                onRemove = { onRemove(id) },
                onResize = onResize
            )
        }
    }
}

@Composable
fun WidgetCard(
    id: Int,
    onRemove: () -> Unit,
    onResize: (Int, Int, Int) -> Unit
) {
    val context = LocalContext.current
    val widgetRepository = (context.applicationContext as GlassLauncherApp).widgetRepository
    val info = remember(id) { widgetRepository.widgetInfo(id) }
    val hostView = remember(id) { widgetRepository.createHostView(id) }
    val density = LocalDensity.current
    val savedSize = remember(id) { widgetRepository.loadWidgetSize(id) }
    var widthDp by remember(id) {
        mutableStateOf(savedSize?.first ?: ((info?.minWidth ?: 220).coerceAtLeast(160)))
    }
    var heightDp by remember(id) {
        mutableStateOf(savedSize?.second ?: ((info?.minHeight ?: 140).coerceAtLeast(110)))
    }

    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val maxWidthDp = maxWidth.value.toInt()
        GlassPanel(shape = RoundedCornerShape(24.dp)) {
            Box(Modifier.padding(6.dp)) {
                if (hostView != null) {
                    val clampedWidth = widthDp.coerceAtMost(maxWidthDp)
                    val heightPx = with(density) { heightDp.dp.toPx() }.toInt()
                    AndroidView(
                        factory = { ctx ->
                            FrameLayout(ctx).apply {
                                addView(
                                    hostView,
                                    FrameLayout.LayoutParams(
                                        FrameLayout.LayoutParams.MATCH_PARENT,
                                        heightPx
                                    )
                                )
                            }
                        },
                        modifier = Modifier
                            .width(clampedWidth.dp)
                            .height(heightDp.dp)
                    )
                } else {
                    Text(
                        text = "Widget tidak tersedia",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.35f))
                        .clickable(onClick = onRemove),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Hapus widget",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .size(24.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.4f))
                        .pointerInput(id) {
                            detectDragGestures(
                                onDragEnd = {
                                    try {
                                        hostView.updateAppWidgetSize(
                                            Bundle(),
                                            widthDp,
                                            heightDp,
                                            widthDp,
                                            heightDp
                                        )
                                    } catch (e: Exception) {
                                    }
                                    onResize(id, widthDp, heightDp)
                                }
                            ) { change, dragAmount ->
                                change.consume()
                                widthDp = (widthDp + dragAmount.x / density.density)
                                    .toInt()
                                    .coerceIn(160, 1000)
                                heightDp = (heightDp + dragAmount.y / density.density)
                                    .toInt()
                                    .coerceIn(90, 1000)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.White.copy(alpha = 0.7f))
                    )
                }
            }
        }
    }
}

@Composable
fun AppGridPage(
    apps: List<AppInfo>,
    onAppClick: (AppInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        gridItems(apps) { app ->
            AppIcon(app = app, onClick = { onAppClick(app) })
        }
    }
}
