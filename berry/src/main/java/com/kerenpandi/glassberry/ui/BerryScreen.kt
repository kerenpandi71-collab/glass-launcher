package com.kerenpandi.glassberry.ui

import android.view.SurfaceView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.kerenpandi.glassberry.AppEntry
import com.kerenpandi.glassberry.BerryController
import com.kerenpandi.glassberry.ShizukuState

@Composable
fun BerryScreen(c: BerryController) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Berry.Body)
            .statusBarsPadding()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SpeakerGrill()
        Spacer(Modifier.height(10.dp))
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Berry.ScreenOff)
                .border(1.dp, Berry.Bezel, RoundedCornerShape(8.dp)),
        ) {
            VirtualSurface(c, Modifier.fillMaxSize())
            when {
                c.shizukuState != ShizukuState.READY -> ShizukuPanel(c)
                c.displayId < 0 -> LoadingPanel()
                c.pickerOpen -> AppPicker(c)
            }
            c.message?.let { text ->
                MessageBanner(text, Modifier.align(Alignment.BottomCenter)) { c.message = null }
            }
            TopStrip(c, Modifier.align(Alignment.TopCenter))
        }
        Spacer(Modifier.height(10.dp))
        NavStrip(c)
        Spacer(Modifier.height(8.dp))
        BerryKeyboard(c)
    }
}

@Composable
private fun SpeakerGrill() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(5.dp, 3.dp).clip(CircleShape).background(Berry.KeySpecial))
        Spacer(Modifier.width(6.dp))
        Box(Modifier.size(44.dp, 4.dp).clip(CircleShape).background(Berry.KeySpecial))
        Spacer(Modifier.width(6.dp))
        Box(Modifier.size(5.dp, 3.dp).clip(CircleShape).background(Berry.KeySpecial))
    }
}

@Composable
private fun VirtualSurface(c: BerryController, modifier: Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            SurfaceView(ctx).apply {
                holder.addCallback(object : android.view.SurfaceHolder.Callback {
                    override fun surfaceCreated(holder: android.view.SurfaceHolder) {}

                    override fun surfaceChanged(
                        holder: android.view.SurfaceHolder,
                        format: Int,
                        width: Int,
                        height: Int,
                    ) {
                        c.onSurfaceReady(holder.surface, width, height)
                    }

                    override fun surfaceDestroyed(holder: android.view.SurfaceHolder) {
                        c.onSurfaceGone()
                    }
                })
                setOnTouchListener { v, e -> c.forwardTouch(e, v.width, v.height) }
            }
        },
    )
}

@Composable
private fun TopStrip(c: BerryController, modifier: Modifier) {
    if (c.displayId < 0 || c.pickerOpen) return
    Row(
        modifier
            .fillMaxWidth()
            .background(Berry.Bezel.copy(alpha = 0.85f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            c.currentApp?.label ?: "Layar virtual",
            color = Berry.InkDim,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.weight(1f))
        Text(
            "display ${c.displayId}",
            color = Berry.InkDim,
            fontSize = 10.sp,
        )
    }
}

@Composable
private fun LoadingPanel() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            "Menyiapkan layar virtual…",
            color = Berry.InkDim,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ShizukuPanel(c: BerryController) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Berry.ScreenOff)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            when (c.shizukuState) {
                ShizukuState.NOT_INSTALLED -> "Shizuku belum terpasang"
                ShizukuState.NOT_RUNNING -> "Shizuku tidak berjalan"
                ShizukuState.NO_PERMISSION -> "Izin Shizuku dibutuhkan"
                else -> "Menyambung ke Shizuku…"
            },
            color = Berry.Ink,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "GlassBerry butuh Shizuku untuk membuat layar virtual ala HP BlackBerry kecil di dalam layar ini.",
            color = Berry.InkDim,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
        )
        c.message?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = Berry.Accent, fontSize = 12.sp, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(20.dp))
        when (c.shizukuState) {
            ShizukuState.NOT_INSTALLED, ShizukuState.NOT_RUNNING ->
                PillButton("BUKA SHIZUKU") { c.openShizuku(LocalContext.current) }
            ShizukuState.NO_PERMISSION -> PillButton("IZINKAN") { c.requestPermission() }
            else -> PillButton("COBA LAGI") { c.retryDisplay() }
        }
    }
}

@Composable
private fun PillButton(label: String, onPress: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(Berry.Accent)
            .clickable(onClick = onPress)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = Berry.Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun AppPicker(c: BerryController) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Berry.ScreenBg)
            .padding(vertical = 10.dp),
    ) {
        Text(
            if (c.search.isEmpty()) "Ketik untuk cari aplikasi…" else c.search,
            color = if (c.search.isEmpty()) Berry.InkDim else Berry.Ink,
            fontSize = 15.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(horizontal = 12.dp),
            maxLines = 1,
        )
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier
                .padding(horizontal = 12.dp)
                .fillMaxWidth()
                .height(1.dp)
                .background(Berry.KeySpecial),
        )
        Spacer(Modifier.height(4.dp))
        val list = c.filteredApps
        if (list.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Tidak ada hasil", color = Berry.InkDim, fontSize = 13.sp)
            }
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(list, key = { it.component }) { app ->
                    AppRow(app) { c.launch(app) }
                }
            }
        }
    }
}

@Composable
private fun AppRow(app: AppEntry, onPress: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onPress)
            .padding(horizontal = 12.dp, vertical = 9.dp),
    ) {
        Text(
            app.label,
            color = Berry.Ink,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            app.packageName,
            color = Berry.InkDim,
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun MessageBanner(text: String, modifier: Modifier, onDismiss: () -> Unit) {
    Box(
        modifier
            .padding(12.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Berry.Bezel)
            .clickable(onClick = onDismiss)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(
            text,
            color = Berry.Ink,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun NavStrip(c: BerryController) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        NavKey("MENU", Modifier.weight(1f), Berry.Key) { c.sendMenu() }
        NavKey("HOME", Modifier.weight(1f), Berry.Key) { c.sendHomeKey() }
        NavKey("BACK", Modifier.weight(1f), Berry.Accent) { c.sendBackKey() }
    }
}

@Composable
private fun NavKey(label: String, modifier: Modifier, bg: androidx.compose.ui.graphics.Color, onPress: () -> Unit) {
    Box(
        modifier
            .height(38.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .clickable(onClick = onPress),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = Berry.Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}
