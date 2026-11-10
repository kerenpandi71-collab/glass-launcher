package com.kerenpandi.glasslauncher.ui.glass

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap

fun Modifier.glassSurface(
    shape: Shape = RoundedCornerShape(28.dp),
    tint: Color = Color.White
): Modifier = composed {
    this
        .clip(shape)
        .background(
            Brush.verticalGradient(
                colors = listOf(tint.copy(alpha = 0.22f), tint.copy(alpha = 0.07f))
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
fun WallpaperBackdrop(
    modifier: Modifier = Modifier,
    blurRadius: Dp = 22.dp,
    darken: Float = 0.38f
) {
    val context = LocalContext.current
    val wallpaper = remember { loadWallpaperBitmap(context) }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF05070D))
    ) {
        if (wallpaper != null) {
            Image(
                bitmap = wallpaper.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(blurRadius)
            )
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = darken * 0.7f),
                            Color.Black.copy(alpha = darken)
                        )
                    )
                )
        )
    }
}

private fun loadWallpaperBitmap(context: Context): Bitmap? = try {
    val drawable: Drawable = WallpaperManager.getInstance(context).drawable ?: return null
    when (drawable) {
        is BitmapDrawable -> drawable.bitmap
        else -> drawable.toBitmap()
    }
} catch (e: Exception) {
    null
}
