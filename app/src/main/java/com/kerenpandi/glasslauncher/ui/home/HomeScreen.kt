package com.kerenpandi.glasslauncher.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.graphics.drawable.toBitmap
import com.kerenpandi.glasslauncher.LauncherViewModel
import com.kerenpandi.glasslauncher.data.AppInfo
import com.kerenpandi.glasslauncher.data.LauncherFolder
import com.kerenpandi.glasslauncher.data.stableKey
import com.kerenpandi.glasslauncher.ui.common.AppIcon
import com.kerenpandi.glasslauncher.ui.common.AppMenuItem
import com.kerenpandi.glasslauncher.ui.common.ClockHeader
import com.kerenpandi.glasslauncher.ui.common.GlassIconButton
import com.kerenpandi.glasslauncher.ui.glass.GlassPanel
import com.kerenpandi.glasslauncher.ui.glass.GlassScrim

private sealed interface HomeEntry {
    data class App(val info: AppInfo) : HomeEntry
    data class Folder(val info: LauncherFolder) : HomeEntry
}

@Composable
fun HomeScreen(
    viewModel: LauncherViewModel,
    onOpenDesktopMode: (Int) -> Unit,
    onSetWallpaper: () -> Unit
) {
    var settingsOpen by remember { mutableStateOf(false) }
    var selectedFolder by remember { mutableStateOf<LauncherFolder?>(null) }
    var folderPickerApp by remember { mutableStateOf<AppInfo?>(null) }
    var createFolderApp by remember { mutableStateOf<AppInfo?>(null) }

    val apps = viewModel.apps
    val dockApps = viewModel.dockApps
    val folders = viewModel.folders
    val folderedKeys = remember(folders) { folders.flatMap { it.appKeys }.toSet() }
    val appEntries = apps.filterNot { it.stableKey() in folderedKeys }.map { HomeEntry.App(it) }
    val entries = appEntries + folders.map { HomeEntry.Folder(it) }
    val pages = entries.chunked(LauncherViewModel.APPS_PER_PAGE)
    val pageCount = pages.size.coerceAtLeast(1)
    val dockKeys = remember(dockApps) { dockApps.map { it.stableKey() }.toSet() }
    val dockFull = dockApps.size >= LauncherViewModel.DOCK_CAPACITY

    Box(Modifier.fillMaxSize()) {
        GlassScrim()
        Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(top = 36.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(Modifier.size(44.dp))
                ClockHeader()
                GlassIconButton(
                    icon = Icons.Default.Settings,
                    contentDescription = "Pengaturan",
                    onClick = { settingsOpen = true }
                )
            }

            val pagerState = rememberPagerState(pageCount = { pageCount })
            HorizontalPager(
                state = pagerState,
                beyondViewportPageCount = 1,
                modifier = Modifier.weight(1f).padding(vertical = 12.dp)
            ) { page ->
                if (entries.isEmpty()) EmptyHomeHint()
                else AppGridPage(
                    entries = pages[page],
                    allApps = apps,
                    pageCount = pageCount,
                    dockKeys = dockKeys,
                    dockFull = dockFull,
                    folders = folders,
                    onMovePage = { key, direction -> viewModel.movePage(key, direction) },
                    onAddToDock = { viewModel.addToDock(it) },
                    onAppClick = { viewModel.launch(it) },
                    onOpenFolder = { selectedFolder = it },
                    onCreateFolder = { createFolderApp = it },
                    onMoveToFolder = { folderPickerApp = it },
                    onRenameFolder = { folder, name -> viewModel.renameFolder(folder.id, name) },
                    onDeleteFolder = { viewModel.deleteFolder(it.id) }
                )
            }

            Row(
                Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(pageCount) { index ->
                    Box(Modifier.padding(horizontal = 4.dp).size(7.dp).clip(CircleShape)
                        .background(if (pagerState.currentPage == index) Color.White else Color.White.copy(alpha = 0.35f)))
                }
            }
            Dock(
                apps = dockApps,
                onAppClick = { viewModel.launch(it) },
                onRemoveFromDock = { viewModel.removeFromDock(it) },
                modifier = Modifier.padding(bottom = 20.dp)
            )
        }

        AnimatedVisibility(
            visible = settingsOpen,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            SettingsSheet(
                onOpenDesktopMode = onOpenDesktopMode,
                onSetWallpaper = onSetWallpaper,
                onDismiss = { settingsOpen = false }
            )
        }
    }

    selectedFolder?.let { folder ->
        FolderDialog(
            folder = folders.firstOrNull { it.id == folder.id } ?: folder,
            allApps = apps,
            folders = folders,
            onDismiss = { selectedFolder = null },
            onLaunch = { viewModel.launch(it) },
            onRemove = { viewModel.removeAppFromFolder(it, folder.id) },
            onRename = { viewModel.renameFolder(folder.id, it) },
            onAdd = { viewModel.addAppToFolder(it, folder.id) }
        )
    }

    folderPickerApp?.let { app ->
        FolderPickerDialog(
            folders = folders,
            app = app,
            onDismiss = { folderPickerApp = null },
            onSelect = { folder -> viewModel.addAppToFolder(app, folder.id); folderPickerApp = null }
        )
    }

    createFolderApp?.let { app ->
        CreateFolderDialog(
            apps = apps.filter { it.stableKey() != app.stableKey() && it.stableKey() !in folderedKeys },
            firstApp = app,
            onDismiss = { createFolderApp = null },
            onCreate = { second, name -> viewModel.createFolder(app, second, name); createFolderApp = null }
        )
    }
}

@Composable
private fun EmptyHomeHint() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        GlassPanel(shape = RoundedCornerShape(24.dp)) {
            Text("Tidak ada aplikasi", color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center, fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 18.dp))
        }
    }
}

@Composable
fun Dock(
    apps: List<AppInfo>,
    onAppClick: (AppInfo) -> Unit,
    onRemoveFromDock: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    GlassPanel(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(30.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            if (apps.isEmpty()) {
                Text("Tahan ikon di atas lalu pilih \"Tambahkan ke dock\"",
                    color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp,
                    textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 18.dp))
            } else apps.forEach { app ->
                AppIcon(app = app, showLabel = false, onClick = { onAppClick(app) },
                    extraMenuItems = listOf(AppMenuItem("Lepas dari dock") { onRemoveFromDock(app.stableKey()) }))
            }
        }
    }
}

@Composable
private fun AppGridPage(
    entries: List<HomeEntry>,
    allApps: List<AppInfo>,
    pageCount: Int,
    dockKeys: Set<String>,
    dockFull: Boolean,
    folders: List<LauncherFolder>,
    onMovePage: (String, Int) -> Unit,
    onAddToDock: (AppInfo) -> Unit,
    onAppClick: (AppInfo) -> Unit,
    onOpenFolder: (LauncherFolder) -> Unit,
    onCreateFolder: (AppInfo) -> Unit,
    onMoveToFolder: (AppInfo) -> Unit,
    onRenameFolder: (LauncherFolder, String) -> Unit,
    onDeleteFolder: (LauncherFolder) -> Unit,
    modifier: Modifier = Modifier
) {
    val rows = entries.chunked(4)
    Column(modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        rows.forEach { rowEntries ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowEntries.forEach { entry ->
                    when (entry) {
                        is HomeEntry.App -> {
                            val app = entry.info
                            val key = app.stableKey()
                            AppIcon(
                                app = app, modifier = Modifier.weight(1f), onClick = { onAppClick(app) },
                                extraMenuItems = buildList {
                                    if (!dockFull && key !in dockKeys) add(AppMenuItem("Tambahkan ke dock") { onAddToDock(app) })
                                    if (pageCount > 1) {
                                        add(AppMenuItem("Pindah ke halaman sebelumnya") { onMovePage(key, -1) })
                                        add(AppMenuItem("Pindah ke halaman berikutnya") { onMovePage(key, 1) })
                                    }
                                    if (appsCanMakeFolder(entries, app)) add(AppMenuItem("Buat folder dengan aplikasi lain") { onCreateFolder(app) })
                                    if (folders.isNotEmpty()) add(AppMenuItem("Pindahkan ke folder") { onMoveToFolder(app) })
                                }
                            )
                        }
                        is HomeEntry.Folder -> FolderTile(
                            modifier = Modifier.weight(1f),
                            folder = entry.info,
                            allApps = allApps,
                            onClick = { onOpenFolder(entry.info) },
                            onRename = { name -> onRenameFolder(entry.info, name) },
                            onDelete = { onDeleteFolder(entry.info) }
                        )
                    }
                }
                repeat(4 - rowEntries.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

private fun appsCanMakeFolder(entries: List<HomeEntry>, app: AppInfo): Boolean =
    entries.any { it is HomeEntry.App && it.info.stableKey() != app.stableKey() }

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FolderTile(
    modifier: Modifier = Modifier,
    folder: LauncherFolder,
    allApps: List<AppInfo>,
    onClick: () -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    var renameOpen by remember { mutableStateOf(false) }
    var name by remember(folder.name) { mutableStateOf(folder.name) }
    val appByKey = remember(allApps) { allApps.associateBy { it.stableKey() } }
    val previewApps = folder.appKeys.take(4).mapNotNull { appByKey[it] }
    Box(modifier.padding(horizontal = 4.dp)) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                .combinedClickable(onClick = onClick, onLongClick = { menuOpen = true })
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            GlassPanel(shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.size(64.dp).padding(7.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    previewApps.chunked(2).forEach { row ->
                        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            row.forEach { app ->
                                Image(
                                    bitmap = remember(app) { app.icon.toBitmap(48, 48).asImageBitmap() },
                                    contentDescription = app.label,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(5.dp))
                                )
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
            Text(folder.name, color = Color.White, fontSize = 11.sp, maxLines = 1,
                overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 5.dp))
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false },
            containerColor = Color(0xFF141C2E)) {
            DropdownMenuItem(text = { Text("Ubah nama", color = Color.White) }, onClick = { menuOpen = false; renameOpen = true })
            DropdownMenuItem(text = { Text("Hapus folder", color = Color.White) }, onClick = { menuOpen = false; onDelete() })
        }
    }
    if (renameOpen) AlertDialog(
        onDismissRequest = { renameOpen = false },
        title = { Text("Nama folder") },
        text = { OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true) },
        confirmButton = { TextButton(onClick = { onRename(name); renameOpen = false }) { Text("Simpan") } },
        dismissButton = { TextButton(onClick = { renameOpen = false }) { Text("Batal") } }
    )
}

@Composable
private fun FolderDialog(
    folder: LauncherFolder,
    allApps: List<AppInfo>,
    folders: List<LauncherFolder>,
    onDismiss: () -> Unit,
    onLaunch: (AppInfo) -> Unit,
    onRemove: (AppInfo) -> Unit,
    onRename: (String) -> Unit,
    onAdd: (AppInfo) -> Unit
) {
    var name by remember(folder.id, folder.name) { mutableStateOf(folder.name) }
    var addMode by remember { mutableStateOf(false) }
    val members = allApps.filter { it.stableKey() in folder.appKeys }
    val available = allApps.filter { app ->
        folders.none { f -> app.stableKey() in f.appKeys } && app.stableKey() !in folder.appKeys
    }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = Color(0xF21B2638)) {
            Column(Modifier.fillMaxWidth().padding(18.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(), label = { Text("Nama folder") })
                Spacer(Modifier.height(12.dp))
                if (addMode) {
                    Text("Pilih aplikasi", color = Color.White, fontSize = 14.sp)
                    available.forEach { app ->
                        TextButton(onClick = { onAdd(app); addMode = false },
                            modifier = Modifier.fillMaxWidth()) {
                            Text(app.label, color = Color.White, textAlign = TextAlign.Start,
                                modifier = Modifier.fillMaxWidth())
                        }
                    }
                    TextButton(onClick = { addMode = false }) { Text("Kembali") }
                } else {
                    val rows = members.chunked(3)
                    rows.forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            row.forEach { app ->
                                AppIcon(app = app, iconSize = 42.dp, onClick = { onLaunch(app) },
                                    extraMenuItems = listOf(AppMenuItem("Keluarkan dari folder") { onRemove(app) }))
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(onClick = { addMode = true }) { Text("Tambah aplikasi") }
                        TextButton(onClick = { onRename(name); onDismiss() }) { Text("Simpan") }
                    }
                }
            }
        }
    }
}

@Composable
private fun FolderPickerDialog(
    folders: List<LauncherFolder>,
    app: AppInfo,
    onDismiss: () -> Unit,
    onSelect: (LauncherFolder) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pindahkan ${app.label} ke folder") },
        text = {
            Column {
                folders.forEach { folder ->
                    TextButton(onClick = { onSelect(folder) }, modifier = Modifier.fillMaxWidth()) {
                        Text(folder.name, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}

@Composable
private fun CreateFolderDialog(
    apps: List<AppInfo>,
    firstApp: AppInfo,
    onDismiss: () -> Unit,
    onCreate: (AppInfo, String) -> Unit
) {
    var name by remember { mutableStateOf("Folder") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Buat folder") },
        text = {
            Column {
                Text("Pilih aplikasi kedua untuk digabung dengan ${firstApp.label}.")
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nama folder") }, singleLine = true)
                Spacer(Modifier.height(8.dp))
                Column(Modifier.heightIn(max = 280.dp).fillMaxWidth()) {
                    apps.forEach { app ->
                        TextButton(onClick = { onCreate(app, name) }, modifier = Modifier.fillMaxWidth()) {
                            Text(app.label, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}
