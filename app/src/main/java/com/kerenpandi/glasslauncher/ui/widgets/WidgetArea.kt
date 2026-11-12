package com.kerenpandi.glasslauncher.ui.widgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.os.Bundle
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.kerenpandi.glasslauncher.data.WidgetEntry
import com.kerenpandi.glasslauncher.data.WidgetHosts

@Composable
fun WidgetArea(
    widgets: List<WidgetEntry>,
    onRemove: (Int) -> Unit,
    onResize: (Int, Int, Int) -> Unit
) {
    if (widgets.isEmpty()) return
    val context = LocalContext.current
    val density = LocalDensity.current.density
    val host = remember { WidgetHosts.get(context) }
    val manager = remember { AppWidgetManager.getInstance(context) }
    DisposableEffect(Unit) {
        host.startListening()
        onDispose { host.stopListening() }
    }
    Column(
        Modifier.fillMaxWidth().padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        widgets.forEach { entry ->
            val info: AppWidgetProviderInfo? = manager.getAppWidgetInfo(entry.id)
            if (info != null) {
                val minWidthDp = info.minWidth.coerceAtLeast(80)
                val minHeightDp = info.minHeight.coerceAtLeast(60)
                val widthDp = entry.widthDp.coerceIn(minWidthDp, 420)
                val heightDp = entry.heightDp.coerceIn(minHeightDp, 320)
                Box(
                    Modifier.fillMaxWidth().height(heightDp.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AndroidView(
                        factory = { ctx ->
                            runCatching {
                                host.createView(ctx, entry.id, info)
                            }.getOrDefault(FrameLayout(ctx))
                        },
                        update = { view ->
                            runCatching {
                                view.updateAppWidgetSize(
                                    Bundle(),
                                    widthDp, heightDp, widthDp, heightDp
                                )
                            }
                        },
                        modifier = Modifier.width(widthDp.dp).fillMaxHeight()
                    )
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.55f))
                            .clickable { onRemove(entry.id) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("×", color = Color.White, fontSize = 13.sp)
                    }
                    val currentWidth by rememberUpdatedState(widthDp)
                    val currentHeight by rememberUpdatedState(heightDp)
                    Box(
                        Modifier
                            .align(Alignment.BottomEnd)
                            .size(26.dp)
                            .pointerInput(entry.id) {
                                detectDragGestures { change, amount ->
                                    change.consume()
                                    onResize(
                                        entry.id,
                                        (currentWidth + (amount.x / density).toInt()).coerceIn(minWidthDp, 420),
                                        (currentHeight + (amount.y / density).toInt()).coerceIn(minHeightDp, 320)
                                    )
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            Modifier.size(14.dp).clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.35f))
                        )
                    }
                }
            }
        }
    }
}
