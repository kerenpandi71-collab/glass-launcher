package com.kerenpandi.glasslauncher.ui.home

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

fun Modifier.homeGestures(
    onSwipeUp: () -> Unit,
    onSwipeDown: () -> Unit,
    onDoubleTap: () -> Unit
): Modifier = composed {
    var dragTotal by remember { mutableStateOf(0f) }
    val threshold = with(LocalDensity.current) { 90.dp.toPx() }
    val currentSwipeUp by rememberUpdatedState(onSwipeUp)
    val currentSwipeDown by rememberUpdatedState(onSwipeDown)
    val currentDoubleTap by rememberUpdatedState(onDoubleTap)
    this
        .pointerInput(Unit) {
            detectTapGestures(onDoubleTap = { currentDoubleTap() })
        }
        .pointerInput(Unit) {
            detectVerticalDragGestures(
                onDragStart = { dragTotal = 0f },
                onVerticalDrag = { _, amount -> dragTotal += amount },
                onDragEnd = {
                    when {
                        dragTotal < -threshold -> currentSwipeUp()
                        dragTotal > threshold -> currentSwipeDown()
                    }
                }
            )
        }
}
