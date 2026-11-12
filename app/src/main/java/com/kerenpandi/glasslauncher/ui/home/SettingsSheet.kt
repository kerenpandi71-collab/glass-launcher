package com.kerenpandi.glasslauncher.ui.home

import android.Manifest
import android.app.Activity
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.IntentCompat
import com.kerenpandi.glasslauncher.LauncherViewModel
import com.kerenpandi.glasslauncher.SystemActions
import com.kerenpandi.glasslauncher.data.IconPackStore
import com.kerenpandi.glasslauncher.data.IconShapes
import com.kerenpandi.glasslauncher.data.ThemeModes
import com.kerenpandi.glasslauncher.data.WidgetHosts
import com.kerenpandi.glasslauncher.ui.glass.GlassPanel

@Composable
fun SettingsSheet(
    viewModel: LauncherViewModel,
    onSetWallpaper: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val settings = viewModel.settings
    var hiddenDialogOpen by remember { mutableStateOf(false) }
    var cityInput by remember(settings.weatherCity) { mutableStateOf(settings.weatherCity) }

    val host = remember { WidgetHosts.get(context) }
    val manager = remember { AppWidgetManager.getInstance(context) }
    var pendingWidgetId by remember { mutableStateOf(-1) }

    val bindLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val id = pendingWidgetId
        if (result.resultCode == Activity.RESULT_OK && id > 0) {
            viewModel.addWidget(id)
        } else if (id > 0) {
            host.deleteAppWidgetId(id)
        }
        pendingWidgetId = -1
    }
    val pickLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val id = pendingWidgetId
        if (result.resultCode == Activity.RESULT_OK && id > 0) {
            val provider = result.data?.let {
                IntentCompat.getParcelableExtra(
                    it,
                    AppWidgetManager.EXTRA_APPWIDGET_PROVIDER,
                    AppWidgetProviderInfo::class.java
                )
            }
            val bound = provider?.provider?.let { cn -> manager.bindAppWidgetIdIfAllowed(id, cn) } == true
            if (bound) {
                viewModel.addWidget(id)
            } else {
                bindLauncher.launch(
                    Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)
                        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, provider)
                )
            }
        } else if (id > 0) {
            host.deleteAppWidgetId(id)
        }
        pendingWidgetId = -1
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let(viewModel::exportLayout)
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::importLayout)
    }
    val calendarPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    val listenerEnabled = remember {
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
    }
    val iconPacks = remember { IconPackStore.installedPacks(context) }
    val calendarGranted = remember {
        androidx.core.content.ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_CALENDAR
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

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
                .verticalScroll(rememberScrollState())
        ) {
            GlassPanel(shape = RoundedCornerShape(30.dp)) {
                Column(Modifier.fillMaxWidth().padding(20.dp)) {
                    Text("Pengaturan", color = Color.White, fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))

                    SectionTitle("Tampilan")
                    SettingRow("Pilih wallpaper") { onSetWallpaper() }
                    ChipRow(
                        options = listOf(
                            ThemeModes.SYSTEM to "Sistem",
                            ThemeModes.DARK to "Gelap",
                            ThemeModes.LIGHT to "Terang"
                        ),
                        selected = settings.themeMode,
                        onSelect = { settings.setThemeMode(it) }
                    )
                    SwitchRow("Warna dinamis (Material You)", settings.dynamicColor) {
                        settings.setDynamicColor(it)
                    }
                    ChipRow(
                        options = listOf(4 to "4 kolom", 5 to "5 kolom", 6 to "6 kolom"),
                        selected = settings.gridColumns,
                        onSelect = { settings.setGridColumns(it) }
                    )
                    Column(Modifier.padding(horizontal = 6.dp, vertical = 8.dp)) {
                        Text("Ukuran ikon: ${settings.iconSizeDp} dp", color = Color.White, fontSize = 14.sp)
                        Slider(
                            value = settings.iconSizeDp.toFloat(),
                            onValueChange = { settings.setIconSizeDp(it.toInt()) },
                            valueRange = 40f..72f,
                            steps = 8
                        )
                    }
                    SwitchRow("Tampilkan label ikon", settings.showLabels) {
                        settings.setShowLabels(it)
                    }
                    ChipRow(
                        options = listOf(
                            IconShapes.CIRCLE to "Lingkaran",
                            IconShapes.SQUIRCLE to "Squircle",
                            IconShapes.ROUNDED to "Membulat",
                            IconShapes.SQUARE to "Kotak"
                        ),
                        selected = settings.iconShape,
                        onSelect = { settings.setIconShape(it) }
                    )
                    if (iconPacks.isEmpty()) {
                        Text(
                            "Tidak ada icon pack terpasang (format ADW)",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp)
                        )
                    } else {
                        ChipRow(
                            options = listOf("" to "Bawaan") + iconPacks,
                            selected = settings.iconPackPackage,
                            onSelect = { settings.setIconPackPackage(it) }
                        )
                    }

                    SectionTitle("Notifikasi")
                    SwitchRow("Badge jumlah notifikasi", settings.notificationBadges) {
                        settings.setNotificationBadges(it)
                    }
                    if (!listenerEnabled) {
                        SettingRow("Izinkan akses notifikasi") {
                            runCatching {
                                context.startActivity(
                                    Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            }
                        }
                    }

                    SectionTitle("At a Glance")
                    SwitchRow("Tampilkan cuaca & agenda", settings.showGlance) {
                        settings.setShowGlance(it)
                    }
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = cityInput,
                            onValueChange = { cityInput = it },
                            singleLine = true,
                            label = { Text("Kota cuaca") },
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { settings.setWeatherCity(cityInput) }) {
                            Text("Simpan")
                        }
                    }
                    if (!calendarGranted) {
                        SettingRow("Izinkan akses kalender") {
                            calendarPermission.launch(Manifest.permission.READ_CALENDAR)
                        }
                    }

                    SectionTitle("Widget")
                    SettingRow("Tambah widget") {
                        val id = host.allocateAppWidgetId()
                        pendingWidgetId = id
                        pickLauncher.launch(
                            Intent(AppWidgetManager.ACTION_APPWIDGET_PICK)
                                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                        )
                    }
                    viewModel.widgetIds.forEach { id ->
                        val info = manager.getAppWidgetInfo(id)
                        val label = info?.provider?.let { cn ->
                            runCatching {
                                context.packageManager.getApplicationLabel(
                                    context.packageManager.getApplicationInfo(cn.packageName, 0)
                                ).toString()
                            }.getOrNull()
                        } ?: "Widget #$id"
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                label,
                                color = Color.White,
                                fontSize = 14.sp,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = {
                                runCatching { host.deleteAppWidgetId(id) }
                                viewModel.removeWidget(id)
                            }) { Text("Hapus") }
                        }
                    }

                    SectionTitle("Aplikasi")
                    SettingRow("Kelola aplikasi tersembunyi") { hiddenDialogOpen = true }

                    SectionTitle("Cadangan")
                    SettingRow("Ekspor layout & pengaturan") {
                        exportLauncher.launch("glass-launcher-layout.json")
                    }
                    SettingRow("Impor layout & pengaturan") {
                        importLauncher.launch(arrayOf("application/json"))
                    }

                    SectionTitle("Gestur")
                    SettingRow(
                        if (SystemActions.isAdminActive(context)) "Kunci layar sekarang"
                        else "Aktifkan kunci layar (double-tap)"
                    ) {
                        if (SystemActions.isAdminActive(context)) {
                            SystemActions.lockScreen(context)
                            onDismiss()
                        } else {
                            SystemActions.requestAdmin(context)
                        }
                    }
                    Text(
                        "Geser ke atas: buka drawer · Geser ke bawah: panel notifikasi · Double-tap: kunci layar",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }

    if (hiddenDialogOpen) {
        HiddenAppsDialog(
            apps = viewModel.allApps,
            hiddenKeys = settings.hiddenApps,
            onToggle = { viewModel.toggleHiddenApp(it) },
            onDismiss = { hiddenDialogOpen = false }
        )
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        title,
        color = Color.White.copy(alpha = 0.7f),
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp, start = 6.dp)
    )
}

@Composable
private fun SettingRow(title: String, onClick: () -> Unit) {
    Text(
        title,
        color = Color.White,
        fontSize = 15.sp,
        modifier = Modifier.fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 6.dp)
    )
}

@Composable
private fun SwitchRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = Color.White, fontSize = 15.sp, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun <T> ChipRow(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { (value, label) ->
            FilterChip(
                selected = value == selected,
                onClick = { onSelect(value) },
                label = { Text(label, fontSize = 12.sp) }
            )
        }
    }
}
