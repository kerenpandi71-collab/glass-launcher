package com.kerenpandi.glasslauncher.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kerenpandi.glasslauncher.ui.glass.GlassPanel

@Composable
fun SettingsSheet(
    onSetWallpaper: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        Modifier.fillMaxSize()
            .background(Color.Black.copy(alpha = 0.35f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            )
    ) {
        Column(
            Modifier.align(Alignment.TopCenter).fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { }
                .padding(start = 20.dp, end = 20.dp, top = 76.dp).navigationBarsPadding()
        ) {
            GlassPanel(shape = RoundedCornerShape(30.dp)) {
                Column(Modifier.fillMaxWidth().padding(20.dp)) {
                    Text("Pengaturan", color = Color.White, fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Pilih wallpaper",
                        color = Color.White,
                        fontSize = 15.sp,
                        modifier = Modifier.fillMaxWidth()
                            .clickable(onClick = onSetWallpaper)
                            .padding(vertical = 14.dp, horizontal = 6.dp)
                    )
                }
            }
        }
    }
}
