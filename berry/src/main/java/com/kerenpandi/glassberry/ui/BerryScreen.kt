package com.kerenpandi.glassberry.ui

import android.view.SurfaceView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import com.kerenpandi.glassberry.AppEntry
import com.kerenpandi.glassberry.BerryController
import com.kerenpandi.glassberry.ShizukuState
import kotlinx.coroutines.delay

@Composable
fun BerryScreen(c: BerryController) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Berry.Bezel)
            .statusBarsPadding(),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(Berry.ScreenOff)
                .border(1.dp, Color(0x2EFFFFFF)),
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
            CursorOverlay(c, Modifier.fillMaxSize())
        }
        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Brush.verticalGradient(listOf(Berry.BodyTop, Berry.Body)))
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            SpeakerGrill()
            NavStrip(c)
            BerryKeyboard(c)
            Trackpad(c, Modifier.weight(1f))
        }
    }

    if (c.showDisplaySettings) {
        DisplaySettingsDialog(c)
    }
}

@Composable
private fun SpeakerGrill() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(5.dp, 3.dp).clip(CircleShape).background(Berry.KeySpecial))
        Spacer(Modifier.width(8.dp))
        Text(
            "GLASSBERRY",
            color = Berry.InkDim,
            fontSize = 9.sp,
            letterSpacing = 3.sp,
            fontFamily = FontFamily.Monospace,
        )
        Spacer(Modifier.width(8.dp))
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
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(color = Berry.Accent, strokeWidth = 2.dp, modifier = Modifier.size(26.dp))
        Spacer(Modifier.height(12.dp))
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
    val ctx = LocalContext.current
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
                PillButton("BUKA SHIZUKU") { c.openShizuku(ctx) }
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
        Spacer(Modifier.height(6.dp))
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
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onPress)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(Berry.Key),
            contentAlignment = Alignment.Center,
        ) {
            app.icon?.let { icon ->
                Image(
                    bitmap = icon,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(30.dp),
                )
            } ?: Box(
                Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(Berry.KeySpecial),
            )
        }
        Spacer(Modifier.width(10.dp))
        Column {
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
}

@Composable
private fun MessageBanner(text: String, modifier: Modifier, onDismiss: () -> Unit) {
    LaunchedEffect(text) {
        delay(4000)
        onDismiss()
    }
    Box(
        modifier
            .padding(12.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Berry.Bezel)
            .border(1.dp, Color(0x2EFFFFFF), RoundedCornerShape(10.dp))
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
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NavKey("MENU", Modifier.weight(1f), Berry.Key) { c.sendMenu() }
        NavKey("HOME", Modifier.weight(1f), Berry.Key) { c.sendHomeKey() }
        NavKey("BACK", Modifier.weight(1f), Berry.Accent) { c.sendBackKey() }
    }
}

@Composable
private fun Trackpad(c: BerryController, modifier: Modifier = Modifier) {
    val haptic = LocalHapticFeedback.current
    Box(
        modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Berry.Key.copy(alpha = 0.6f))
            .border(1.dp, Color(0x24FFFFFF), RoundedCornerShape(12.dp))
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    val startTime = System.currentTimeMillis()
                    var twoFinger = false
                    var dragging = false
                    var settingsShown = false
                    var moved = 0f
                    while (true) {
                        val event = awaitPointerEvent()
                        val pressed = event.changes.filter { it.pressed }
                        if (pressed.size >= 2) twoFinger = true
                        if (twoFinger) {
                            if (!dragging) {
                                c.trackpadStart()
                                dragging = true
                            }
                            pressed.lastOrNull()?.let { change ->
                                val delta = change.positionChange()
                                c.trackpadMove(delta.x, delta.y)
                                change.consume()
                            }
                        } else {
                            pressed.firstOrNull()?.let { change ->
                                val delta = change.positionChange()
                                moved += delta.getDistance()
                                c.trackpadHover(delta.x, delta.y)
                                change.consume()
                                if (!settingsShown &&
                                    System.currentTimeMillis() - startTime > 550 &&
                                    moved < 12f
                                ) {
                                    settingsShown = true
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    c.showDisplaySettings = true
                                }
                            }
                        }
                        if (event.changes.none { it.pressed }) {
                            if (dragging) {
                                c.trackpadEnd()
                            } else if (!twoFinger && !settingsShown &&
                                System.currentTimeMillis() - startTime < 260 &&
                                moved < 12f
                            ) {
                                c.trackpadTap()
                            }
                            break
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "TOUCHPAD",
            color = Berry.InkDim.copy(alpha = 0.45f),
            fontSize = 10.sp,
            letterSpacing = 2.sp,
        )
    }
}

@Composable
private fun CursorOverlay(c: BerryController, modifier: Modifier) {
    if (c.displayId < 0 || c.pickerOpen) return
    Canvas(modifier) {
        val (dw, dh) = c.displaySize
        if (dw <= 0 || dh <= 0) return@Canvas
        val scaleX = size.width / dw
        val scaleY = size.height / dh
        val x = (c.cursorX * scaleX).coerceIn(0f, size.width)
        val y = (c.cursorY * scaleY).coerceIn(0f, size.height)
        val s = 1.15f
        val arrow = Path().apply {
            moveTo(x, y)
            lineTo(x + 2.2f * s, y + 15.5f * s)
            lineTo(x + 5.6f * s, y + 12.2f * s)
            lineTo(x + 8.0f * s, y + 17.6f * s)
            lineTo(x + 10.4f * s, y + 16.5f * s)
            lineTo(x + 8.1f * s, y + 11.0f * s)
            lineTo(x + 12.2f * s, y + 11.0f * s)
            close()
        }
        drawPath(arrow, color = Color.Black.copy(alpha = 0.75f), style = Stroke(width = 2.5f))
        drawPath(arrow, color = Color.White)
    }
}

@Composable
private fun DisplaySettingsDialog(c: BerryController) {
    var dpi by remember(c.showDisplaySettings) { mutableStateOf(c.dpiScale) }
    var res by remember(c.showDisplaySettings) { mutableStateOf(c.resScale) }
    val (baseW, baseH) = c.surfaceSize
    Dialog(onDismissRequest = { c.showDisplaySettings = false }) {
        Column(
            Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(Berry.Key)
                .border(1.dp, Color(0x2EFFFFFF), RoundedCornerShape(16.dp))
                .padding(20.dp),
        ) {
            Text(
                "Pengaturan Layar Virtual",
                color = Berry.Ink,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "Ketajaman (DPI): ${(c.deviceDensityDpi * dpi).toInt()}dpi",
                color = Berry.InkDim,
                fontSize = 13.sp,
            )
            Slider(
                value = dpi,
                onValueChange = { dpi = it },
                valueRange = 0.4f..1.2f,
                colors = SliderDefaults.colors(
                    thumbColor = Berry.Accent,
                    activeTrackColor = Berry.Accent,
                    inactiveTrackColor = Berry.KeySpecial,
                ),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Resolusi: ${(baseW * res).toInt()}x${(baseH * res).toInt()}",
                color = Berry.InkDim,
                fontSize = 13.sp,
            )
            Slider(
                value = res,
                onValueChange = { res = it },
                valueRange = 0.5f..1f,
                colors = SliderDefaults.colors(
                    thumbColor = Berry.Accent,
                    activeTrackColor = Berry.Accent,
                    inactiveTrackColor = Berry.KeySpecial,
                ),
            )
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Berry.KeySpecial)
                        .clickable { c.showDisplaySettings = false }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Batal", color = Berry.Ink, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Berry.Accent)
                        .clickable {
                            c.applyDisplayConfig(dpi, res)
                            c.showDisplaySettings = false
                        }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Terapkan", color = Berry.Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun NavKey(label: String, modifier: Modifier, bg: Color, onPress: () -> Unit) {
    Box(
        modifier
            .height(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .clickable(onClick = onPress),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = Berry.Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}
