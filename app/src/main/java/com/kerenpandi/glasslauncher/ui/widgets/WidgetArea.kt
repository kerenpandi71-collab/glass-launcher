package com.kerenpandi.glasslauncher.ui.widgets

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.os.Bundle
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.kerenpandi.glasslauncher.data.WidgetEntry
import com.kerenpandi.glasslauncher.data.WidgetHosts
import kotlin.math.roundToInt

@Composable
fun WidgetArea(
    widgets: List<WidgetEntry>,
    onRemove: (Int) -> Unit,
    onResize: (Int, Int, Int) -> Unit,
    onResizeEnd: () -> Unit,
    onMoveCommit: (List<Int>, List<Int>, Int) -> Unit
) {
    if (widgets.isEmpty()) return
    val context = LocalContext.current
    val density = LocalDensity.current.density
    val host = remember { WidgetHosts.get(context) }
    val manager = remember { AppWidgetManager.getInstance(context) }
    var resizingGroup by remember { mutableStateOf<Int?>(null) }
    var movingGroup by remember { mutableStateOf<Int?>(null) }
    var liveOffsetX by remember { mutableStateOf(0f) }
    var liveTranslateY by remember { mutableStateOf(0f) }
    val gapDp = 8f

    val groups = remember(widgets) {
        val visible = widgets.filter { manager.getAppWidgetInfo(it.id) != null }
        val map = LinkedHashMap<Int, MutableList<WidgetEntry>>()
        visible.forEach { entry ->
            val key = if (entry.stackId > 0) entry.stackId else 1_000_000 + entry.id
            map.getOrPut(key) { mutableListOf() }.add(entry)
        }
        map.values.toList()
    }
    if (groups.isEmpty()) return

    fun groupHeightDp(group: List<WidgetEntry>): Int =
        group.maxOf { entry ->
            val min = manager.getAppWidgetInfo(entry.id)?.minHeight?.coerceAtLeast(60) ?: 60
            entry.heightDp.coerceIn(min, 320)
        }

    fun groupWidthDp(group: List<WidgetEntry>): Int =
        group.maxOf { entry ->
            val min = manager.getAppWidgetInfo(entry.id)?.minWidth?.coerceAtLeast(80) ?: 80
            entry.widthDp.coerceIn(min, 480)
        }

    BoxWithConstraints(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        val availableWidth = maxWidth
        val centers = run {
            var acc = 0f
            groups.map { group ->
                val c = acc + groupHeightDp(group) / 2f
                acc += groupHeightDp(group) + gapDp
                c
            }
        }
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            groups.forEachIndexed { gIndex, group ->
                val slotW = groupWidthDp(group)
                val slotH = groupHeightDp(group)
                val maxOffsetXDp = ((availableWidth - slotW.dp) / 2).coerceAtLeast(0.dp).value
                val baseOffsetX = group.first().offsetXDp.coerceIn(-maxOffsetXDp.toInt(), maxOffsetXDp.toInt())
                val currentW by rememberUpdatedState(slotW)
                val currentH by rememberUpdatedState(slotH)
                val isMoving = movingGroup == gIndex
                val isResizing = resizingGroup == gIndex
                val groupIds = group.map { it.id }
                var createFailed by remember(groupIds) { mutableStateOf(false) }
                var stackActive by remember(groupIds) { mutableStateOf(0) }

                LaunchedEffect(groupIds) {
                    if (group.size > 1) {
                        while (true) {
                            kotlinx.coroutines.delay(30_000)
                            stackActive = (stackActive + 1) % group.size
                        }
                    }
                }

                fun commitMove() {
                    val draggedCenter = centers[gIndex] + liveTranslateY
                    var target = 0
                    groups.forEachIndexed { j, _ ->
                        if (j != gIndex && centers[j] < draggedCenter) target++
                    }
                    val newGroups = groups.toMutableList()
                    val moved = newGroups.removeAt(gIndex)
                    newGroups.add(target.coerceIn(0, newGroups.size), moved)
                    onMoveCommit(newGroups.flatMap { it.map { e -> e.id } }, groupIds, liveOffsetX.roundToInt())
                    movingGroup = null
                    liveOffsetX = 0f
                    liveTranslateY = 0f
                }

                Box(
                    Modifier.fillMaxWidth().height(slotH.dp)
                        .offset(x = (if (isMoving) liveOffsetX else baseOffsetX.toFloat()).dp)
                        .graphicsLayer {
                            translationY = if (isMoving) liveTranslateY.dp.toPx() else 0f
                        }
                        .pointerInput(groupIds) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    if (resizingGroup == null) {
                                        movingGroup = gIndex
                                        liveOffsetX = baseOffsetX.toFloat()
                                        liveTranslateY = 0f
                                    }
                                },
                                onDrag = { change, amount ->
                                    if (movingGroup == gIndex) {
                                        change.consume()
                                        liveOffsetX = (liveOffsetX + amount.x / density)
                                            .coerceIn(-maxOffsetXDp, maxOffsetXDp)
                                        liveTranslateY += amount.y / density
                                    }
                                },
                                onDragEnd = { if (movingGroup == gIndex) commitMove() },
                                onDragCancel = { if (movingGroup == gIndex) commitMove() }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (group.size == 1) {
                        val entry = group.first()
                        val info = manager.getAppWidgetInfo(entry.id)
                        if (info != null) {
                            AndroidView(
                                factory = { ctx ->
                                    try {
                                        host.createView(ctx, entry.id, info)
                                    } catch (e: Exception) {
                                        createFailed = true
                                        FrameLayout(ctx)
                                    }
                                },
                                update = { view ->
                                    (view as? AppWidgetHostView)?.let { hostView ->
                                        runCatching {
                                            hostView.updateAppWidgetSize(
                                                Bundle(),
                                                slotW, slotH, slotW, slotH
                                            )
                                        }
                                    }
                                },
                                modifier = Modifier.width(slotW.dp).fillMaxHeight()
                            )
                        }
                    } else {
                        val activeIndex = stackActive.coerceIn(0, group.lastIndex)
                        val entry = group[activeIndex]
                        val info = manager.getAppWidgetInfo(entry.id)
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            if (info != null) {
                                AndroidView(
                                    factory = { ctx ->
                                        try {
                                            host.createView(ctx, entry.id, info)
                                        } catch (e: Exception) {
                                            createFailed = true
                                            FrameLayout(ctx)
                                        }
                                    },
                                    update = { view ->
                                        (view as? AppWidgetHostView)?.let { hostView ->
                                            runCatching {
                                                hostView.updateAppWidgetSize(
                                                    Bundle(),
                                                    slotW, slotH, slotW, slotH
                                                )
                                            }
                                        }
                                    },
                                    modifier = Modifier.width(slotW.dp).fillMaxHeight()
                                )
                            }
                            Column(
                                Modifier.align(Alignment.CenterEnd).padding(end = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                group.forEachIndexed { i, _ ->
                                    Box(
                                        Modifier.size(6.dp).clip(CircleShape)
                                            .background(
                                                if (i == activeIndex) Color.White
                                                else Color.White.copy(alpha = 0.4f)
                                            )
                                            .clickable { stackActive = i }
                                    )
                                }
                            }
                        }
                    }
                    if (isMoving) {
                        Box(
                            Modifier.width(slotW.dp).fillMaxHeight()
                                .background(Color.Black.copy(alpha = 0.25f))
                        )
                    }
                    if (createFailed) {
                        Text(
                            "Widget gagal dimuat",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp
                        )
                    }
                    Row(
                        Modifier.align(Alignment.TopEnd).padding(6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            Modifier.size(24.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.55f))
                                .clickable {
                                    if (isResizing) {
                                        resizingGroup = null
                                        onResizeEnd()
                                    } else {
                                        resizingGroup = gIndex
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Ubah ukuran widget",
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                        Box(
                            Modifier.size(24.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.55f))
                                .clickable { group.forEach { onRemove(it.id) } },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Hapus widget",
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                    if (isResizing) {
                        Box(
                            Modifier
                                .align(Alignment.BottomEnd)
                                .padding(4.dp)
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFA8C8FF).copy(alpha = 0.85f))
                                .pointerInput(groupIds) {
                                    detectDragGestures(
                                        onDragEnd = { onResizeEnd() },
                                        onDragCancel = { onResizeEnd() }
                                    ) { change, amount ->
                                        change.consume()
                                        val w = (currentW + (amount.x / density).toInt())
                                            .coerceIn(80, 480)
                                        val h = (currentH + (amount.y / density).toInt())
                                            .coerceIn(60, 320)
                                        group.forEach { onResize(it.id, w, h) }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                Modifier.size(12.dp).clip(CircleShape)
                                    .background(Color.White)
                            )
                        }
                    }
                }
            }
        }
    }
}
