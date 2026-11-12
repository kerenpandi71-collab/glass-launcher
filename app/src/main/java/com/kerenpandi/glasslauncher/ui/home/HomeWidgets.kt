package com.kerenpandi.glasslauncher.ui.home

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.kerenpandi.glasslauncher.ui.glass.GlassPanel

@Composable
fun HomeWidgets(
    context: Context,
    host: AppWidgetHost,
    widgetIds: List<Int>,
    onRemove: (Int) -> Unit,
    onAdd: () -> Unit
) {
    Column(
        Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onAdd) { Text("＋ Widget", color = Color.White) }
        }
        widgetIds.forEach { id ->
            val info = AppWidgetManager.getInstance(context).getAppWidgetInfo(id)
            if (info != null) {
                GlassPanel(shape = RoundedCornerShape(22.dp)) {
                    Column(Modifier.fillMaxWidth().padding(8.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { onRemove(id) }) {
                                Text("Hapus", color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp)
                            }
                        }
                        AndroidView(
                            factory = { host.createView(it, id, info) },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 90.dp, max = 280.dp)
                        )
                    }
                }
            }
        }
    }
}
