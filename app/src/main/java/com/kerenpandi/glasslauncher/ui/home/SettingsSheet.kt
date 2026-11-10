package com.kerenpandi.glasslauncher.ui.home

import android.content.Context
import android.graphics.Point
import android.hardware.display.Display
import android.hardware.display.DisplayManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kerenpandi.glasslauncher.ui.glass.GlassPanel

@Composable
fun SettingsSheet(
    onAddWidget: () -> Unit,
    onLaunchSecondary: (Int) -> Unit,
    onSetWallpaper: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val displays: List<Display> = remember {
        val manager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        manager.displays.toList()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.35f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            )
    ) {
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { }
                .padding(20.dp)
                .navigationBarsPadding()
        ) {
            GlassPanel(shape = RoundedCornerShape(30.dp)) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Pengaturan",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(10.dp))
                    SettingsAction(Icons.Default.Add, "Tambah widget") { onAddWidget() }
                    SettingsAction(null, "Pilih wallpaper") { onSetWallpaper() }

                    HorizontalDivider(
                        color = Color.White.copy(alpha = 0.15f),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    Text(
                        text = "Layar sekunder",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Aktifkan simulasi lewat Setelan → Developer options → Simulate secondary displays, lalu buka sheet ini lagi.",
                        color = Color.White.copy(alpha = 0.65f),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp, bottom = 6.dp)
                    )
                    displays.forEach { display ->
                        DisplayRow(display = display, onLaunch = onLaunchSecondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun DisplayRow(
    display: Display,
    onLaunch: (Int) -> Unit
) {
    val size = remember(display.displayId) {
        Point().also { display.getRealSize(it) }
    }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Layar ${display.displayId} • ${size.x}×${size.y}",
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 13.sp,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "Buka",
            color = Color(0xFFA8C8FF),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable { onLaunch(display.displayId) }
                .padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun SettingsAction(
    icon: ImageVector?,
    label: String,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(14.dp))
        }
        Text(label, color = Color.White, fontSize = 15.sp)
    }
}
