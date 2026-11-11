package com.kerenpandi.glasslauncher.ui.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

fun Modifier.glassSurface(
    shape: Shape = RoundedCornerShape(28.dp),
    tint: Color = Color.White
): Modifier = composed {
    this
        .clip(shape)
        .background(
            Brush.verticalGradient(
                colors = listOf(tint.copy(alpha = 0.30f), tint.copy(alpha = 0.10f))
            )
        )
        .border(
            width = 1.dp,
            brush = Brush.verticalGradient(
                colors = listOf(tint.copy(alpha = 0.55f), tint.copy(alpha = 0.12f))
            ),
            shape = shape
        )
}

@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(28.dp),
    content: @Composable () -> Unit
) {
    Box(modifier = modifier.glassSurface(shape)) {
        content()
    }
}

@Composable
fun GlassScrim(
    modifier: Modifier = Modifier,
    darken: Float = 0.22f
) {
    Box(
        modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.Black.copy(alpha = darken * 0.5f),
                        Color.Black.copy(alpha = darken)
                    )
                )
            )
    )
}

