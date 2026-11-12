package com.kerenpandi.glasslauncher.ui.home

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.view.WindowManager
import android.widget.Toast
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.app.NotificationManagerCompat
import com.kerenpandi.glasslauncher.BuildConfig
import com.kerenpandi.glasslauncher.LauncherViewModel
import com.kerenpandi.glasslauncher.ShizukuActions
import com.kerenpandi.glasslauncher.SystemActions
import com.kerenpandi.glasslauncher.data.IconPackStore
import com.kerenpandi.glasslauncher.data.IconShapes
import com.kerenpandi.glasslauncher.data.ThemeModes
import com.kerenpandi.glasslauncher.data.WidgetHosts
import com.kerenpandi.glasslauncher.ui.glass.GlassPanel
import com.kerenpandi.glasslauncher.ui.widgets.WidgetPickerDialog

@Composable
fun SettingsSheet(
    viewModel: LauncherViewModel,
    onSetWallpaper: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val settings = viewModel.settings
    var hiddenDialogOpen by remember { mutableStateOf(false) }
    var resetConfirmOpen by remember { mutableStateOf(false) }
    var pickerOpen by remember { mutableStateOf(false) }

    val host = remember { WidgetHosts.get(context) }
    val manager = remember { AppWidgetManager.getInstance(context) }
    var pendingWidgetId by remember { mutableStateOf(-1) }

    val configureLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val id = pendingWidgetId
        if (result.resultCode == Activity.RESULT_OK && id > 0) {
            viewModel.addWidget(id)
            Toast.makeText(context, "Widget ditambahkan", Toast.LENGTH_SHORT).show()
        } else if (id > 0) {
            runCatching { host.deleteAppWidgetId(id) }
        }
        pendingWidgetId = -1
    }
    val bindLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val id = pendingWidgetId
        if (result.resultCode == Activity.RESULT_OK && id > 0) {
            val configure = manager.getAppWidgetInfo(id)?.configure
            if (configure != null) {
                configureLauncher.launch(
                    Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE)
                        .setComponent(configure)
                        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                )
            } else {
                viewModel.addWidget(id)
                Toast.makeText(context, "Widget ditambahkan", Toast.LENGTH_SHORT).show()
                pendingWidgetId = -1
            }
        } else {
            if (id > 0) runCatching { host.deleteAppWidgetId(id) }
            pendingWidgetId = -1
        }
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let(viewModel::exportLayout)
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::importLayout)
    }

    var listenerEnabled by remember { mutableStateOf(false) }
    var adminActive by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                listenerEnabled = NotificationManagerCompat
                    .getEnabledListenerPackages(context)
                    .contains(context.packageName)
                adminActive = SystemActions.isAdminActive(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val iconPacks = remember { IconPackStore.installedPacks(context) }
    val installedProviders = remember { manager.installedProviders }
    var shizukuAvailable by remember { mutableStateOf(ShizukuActions.isBinderAvailable()) }
    var shizukuGranted by remember { mutableStateOf(ShizukuActions.hasPermission()) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        val dialogView = LocalView.current
        androidx.compose.runtime.SideEffect {
            val window = (dialogView.parent as? DialogWindowProvider)?.window
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && window != null) {
                window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                window.setBackgroundBlurRadius(38)
                window.setDimAmount(0.20f)
            }
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
            GlassPanel(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 40.dp)
                    .background(Color(0xF21B2A40), RoundedCornerShape(30.dp))
                    .heightIn(max = 660.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { },
                shape = RoundedCornerShape(30.dp)
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp)
                        .navigationBarsPadding()
                ) {
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
                        onSelect = { settings.updateThemeMode(it) }
                    )
                    SwitchRow("Warna dinamis (Material You)", settings.dynamicColor) {
                        settings.updateDynamicColor(it)
                    }
                    ChipRow(
                        options = listOf(4 to "4 kolom", 5 to "5 kolom", 6 to "6 kolom"),
                        selected = settings.gridColumns,
                        onSelect = { settings.updateGridColumns(it) }
                    )
                    Column(Modifier.padding(horizontal = 6.dp, vertical = 8.dp)) {
                        Text("Ukuran ikon: ${settings.iconSizeDp} dp", color = Color.White, fontSize = 14.sp)
                        Slider(
                            value = settings.iconSizeDp.toFloat(),
                            onValueChange = { settings.updateIconSizeDp(it.toInt()) },
                            valueRange = 40f..72f,
                            steps = 8
                        )
                    }
                    SwitchRow("Tampilkan label ikon", settings.showLabels) {
                        settings.updateShowLabels(it)
                    }
                    ChipRow(
                        options = listOf(
                            IconShapes.CIRCLE to "Lingkaran",
                            IconShapes.SQUIRCLE to "Squircle",
                            IconShapes.ROUNDED to "Membulat",
                            IconShapes.SQUARE to "Kotak"
                        ),
                        selected = settings.iconShape,
                        onSelect = { settings.updateIconShape(it) }
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
                            onSelect = { settings.updateIconPackPackage(it) }
                        )
                    }

                    SectionTitle("Notifikasi")
                    SwitchRow("Badge jumlah notifikasi", settings.notificationBadges) {
                        settings.updateNotificationBadges(it)
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
                    } else {
                        Text(
                            "Akses notifikasi aktif",
                            color = Color(0xFF8BC34A),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 6.dp)
                        )
                    }

                    SectionTitle("Widget")
                    SettingRow("Tambah widget") { pickerOpen = true }
                    Text(
                        "Tarik titik di pojok kanan bawah widget untuk mengubah ukuran.",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )
                    viewModel.widgets.forEach { entry ->
                        val info = manager.getAppWidgetInfo(entry.id)
                        val label = info?.provider?.let { cn ->
                            runCatching {
                                context.packageManager.getApplicationLabel(
                                    context.packageManager.getApplicationInfo(cn.packageName, 0)
                                ).toString()
                            }.getOrNull()
                        } ?: "Widget #${entry.id}"
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "$label · ${entry.widthDp}×${entry.heightDp} dp",
                                color = Color.White,
                                fontSize = 14.sp,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = {
                                runCatching { host.deleteAppWidgetId(entry.id) }
                                viewModel.removeWidget(entry.id)
                            }) { Text("Hapus") }
                        }
                    }

                    SectionTitle("Shizuku")
                    Text(
                        when {
                            !shizukuAvailable -> "Shizuku tidak terhubung"
                            !shizukuGranted -> "Izin Shizuku belum diberikan"
                            else -> "Shizuku aktif"
                        },
                        color = when {
                            !shizukuAvailable -> Color.White.copy(alpha = 0.6f)
                            !shizukuGranted -> Color(0xFFFFB74D)
                            else -> Color(0xFF8BC34A)
                        },
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )
                    SettingRow("Minta izin Shizuku") {
                        ShizukuActions.requestPermission { granted ->
                            shizukuGranted = granted && ShizukuActions.hasPermission()
                            shizukuAvailable = ShizukuActions.isBinderAvailable()
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
                        if (adminActive) "Kunci layar sekarang"
                        else "Aktifkan kunci layar (double-tap)"
                    ) {
                        if (adminActive) {
                            SystemActions.lockScreen(context)
                            onDismiss()
                        } else {
                            SystemActions.requestAdmin(context)
                        }
                    }
                    Text(
                        "Geser ke atas: pencarian · Geser ke bawah: panel notifikasi · Double-tap: kunci layar",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp)
                    )

                    SectionTitle("Tentang")
                    Text(
                        "Glass Launcher v${BuildConfig.VERSION_NAME} (build ${BuildConfig.VERSION_CODE})",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )
                    SettingRow("Reset pengaturan") { resetConfirmOpen = true }

                    Spacer(Modifier.height(12.dp))
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Tutup") }
                }
            }
        }
    }

    if (pickerOpen) {
        WidgetPickerDialog(
            providers = installedProviders,
            onSelect = { info ->
                pickerOpen = false
                val id = host.allocateAppWidgetId()
                pendingWidgetId = id
                val bound = manager.bindAppWidgetIdIfAllowed(id, info.provider)
                if (bound) {
                    val configure = manager.getAppWidgetInfo(id)?.configure
                    if (configure != null) {
                        configureLauncher.launch(
                            Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE)
                                .setComponent(configure)
                                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                        )
                    } else {
                        viewModel.addWidget(id)
                        pendingWidgetId = -1
                        Toast.makeText(context, "Widget ditambahkan", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    bindLauncher.launch(
                        Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)
                            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info)
                    )
                }
            },
            onDismiss = { pickerOpen = false }
        )
    }

    if (hiddenDialogOpen) {
        HiddenAppsDialog(
            apps = viewModel.allApps,
            hiddenKeys = settings.hiddenApps,
            onToggle = { viewModel.toggleHiddenApp(it) },
            onDismiss = { hiddenDialogOpen = false }
        )
    }

    if (resetConfirmOpen) {
        AlertDialog(
            onDismissRequest = { resetConfirmOpen = false },
            title = { Text("Reset pengaturan") },
            text = { Text("Semua pengaturan tampilan akan dikembalikan ke bawaan. Susunan ikon, folder, dock, dan widget tidak diubah.") },
            confirmButton = {
                TextButton(onClick = {
                    settings.reset()
                    resetConfirmOpen = false
                }) { Text("Reset") }
            },
            dismissButton = {
                TextButton(onClick = { resetConfirmOpen = false }) { Text("Batal") }
            }
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
