package com.kerenpandi.glasslauncher.ui.widgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.os.Bundle
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.kerenpandi.glasslauncher.data.WidgetHosts

@Composable
fun WidgetArea(
    widgetIds: List<Int>,
    onRemove: (Int) -> Unit
) {
    if (widgetIds.isEmpty()) return
    val context = androidx.compose.ui.platform.LocalContext.current
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
        widgetIds.forEach { id ->
            val info: AppWidgetProviderInfo? = manager.getAppWidgetInfo(id)
            if (info != null) {
                Box(
                    Modifier.fillMaxWidth().height(130.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AndroidView(
                        factory = { ctx ->
                            runCatching {
                                host.createView(ctx, id, info).apply {
                                    updateAppWidgetSize(
                                        Bundle(),
                                        220, 100, 400, 130
                                    )
                                }
                            }.getOrDefault(FrameLayout(ctx))
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.55f))
                            .clickable { onRemove(id) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("×", color = Color.White, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
