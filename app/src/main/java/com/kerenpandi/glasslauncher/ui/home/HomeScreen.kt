package com.kerenpandi.glasslauncher.ui.home

// Feature-branch build verification.

import android.os.Build
import android.widget.Toast
import android.view.WindowManager

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.Image
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogWindowProvider
import com.kerenpandi.glasslauncher.LauncherViewModel
import com.kerenpandi.glasslauncher.SystemActions
import com.kerenpandi.glasslauncher.data.AppInfo
import com.kerenpandi.glasslauncher.data.LauncherFolder
import com.kerenpandi.glasslauncher.data.stableKey
import com.kerenpandi.glasslauncher.ui.common.AppIcon
import com.kerenpandi.glasslauncher.ui.common.AppMenuItem
import com.kerenpandi.glasslauncher.ui.common.ClockHeader
import com.kerenpandi.glasslauncher.ui.common.GlassIconButton
import com.kerenpandi.glasslauncher.ui.common.cachedAppIcon
import com.kerenpandi.glasslauncher.ui.glass.GlassPanel
import com.kerenpandi.glasslauncher.ui.glass.GlassScrim
import com.kerenpandi.glasslauncher.ui.search.SearchOverlay
import com.kerenpandi.glasslauncher.ui.widgets.WidgetArea

private sealed interface HomeEntry {
    data class App(val info: AppInfo) : HomeEntry
    data class Folder(val info: LauncherFolder) : HomeEntry
}

@Composable
fun HomeScreen(
    viewModel: LauncherViewModel,
    onSetWallpaper: () -> Unit
) {
    var settingsOpen by remember { mutableStateOf(false) }
    var searchOpen by remember { mutableStateOf(false) }
    var lockPromptOpen by remember { mutableStateOf(false) }
    var selectedFolder by remember { mutableStateOf<LauncherFolder?>(null) }
    var folderPickerApp by remember { mutableStateOf<AppInfo?>(null) }
    var createFolderApp by remember { mutableStateOf<AppInfo?>(null) }

    val context = LocalContext.current
    val settings = viewModel.settings
    val apps = viewModel.apps
    val dockApps = viewModel.dockApps
    val folders = viewModel.folders
    val folderedKeys = remember(folders) { folders.flatMap { it.appKeys }.toSet() }
    val entries = remember(apps, folders) {
        apps.filterNot { it.stableKey() in folderedKeys }.map { HomeEntry.App(it) } +
            folders.map { HomeEntry.Folder(it) }
    }
    val perPage = viewModel.appsPerPage
    val pages = remember(entries, perPage) { entries.chunked(perPage) }
    val pageCount = maxOf(pages.size, settings.homeScreens)
    val dockKeys = remember(dockApps) { dockApps.map { it.stableKey() }.toSet() }
    val dockFull = dockApps.size >= LauncherViewModel.DOCK_CAPACITY

    val pagerState = rememberPagerState(pageCount = { pageCount })
    val scope = rememberCoroutineScope()
    BackHandler(enabled = settingsOpen) { settingsOpen = false }
    BackHandler(enabled = searchOpen) { searchOpen = false }
    BackHandler(enabled = !settingsOpen && !searchOpen && pagerState.currentPage > 0) {
        scope.launch { pagerState.animateScrollToPage(0) }
    }

    val onSwipeUp: () -> Unit = { if (!searchOpen && !settingsOpen) searchOpen = true }
    val onSwipeDown: () -> Unit = {
        if (!searchOpen && !settingsOpen) {
            SystemActions.expandNotificationsPanel(context)
        }
    }
    val onDoubleTap: () -> Unit = {
        if (!searchOpen && !settingsOpen) {
            if (SystemActions.isAdminActive(context)) {
                if (!SystemActions.lockScreen(context)) {
                    Toast.makeText(context, "Gagal mengunci layar", Toast.LENGTH_SHORT).show()
                }
            } else {
                lockPromptOpen = true
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        GlassScrim(darken = if (settings.themeMode == com.kerenpandi.glasslauncher.data.ThemeModes.LIGHT) 0.10f else 0.22f)
        Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(top = 36.dp)
                    .homeGestures(onSwipeUp, onSwipeDown, onDoubleTap),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassIconButton(
                    icon = Icons.Default.Search,
                    contentDescription = "Cari aplikasi",
                    onClick = { searchOpen = true }
                )
                ClockHeader(modifier = Modifier.weight(1f))
                GlassIconButton(
                    icon = Icons.Default.Settings,
                    contentDescription = "Pengaturan",
                    onClick = { settingsOpen = true }
                )
            }

            HorizontalPager(
                state = pagerState,
                beyondViewportPageCount = 1,
                modifier = Modifier.weight(1f).padding(vertical = 12.dp)
            ) { page ->
                Column(Modifier.fillMaxSize()) {
                    WidgetArea(
                        widgets = viewModel.widgets.filter { it.page == page },
                        onRemove = { viewModel.removeWidget(it) },
                        onResize = { id, w, h -> viewModel.resizeWidget(id, w, h) },
                        onResizeEnd = { viewModel.saveWidgets() }
                    )
                    val pageEntries = pages.getOrElse(page) { emptyList() }
                    Box(
                        Modifier.fillMaxWidth().weight(1f)
                            .homeGestures(onSwipeUp, onSwipeDown, onDoubleTap)
                    ) {
                        if (pageEntries.isEmpty()) EmptyHomeHint()
                        else AppGridPage(
                            entries = pageEntries,
                            allApps = apps,
                            pageCount = pageCount,
                            dockKeys = dockKeys,
                            dockFull = dockFull,
                            folders = folders,
                            columns = settings.gridColumns,
                            onMovePage = { key, direction -> viewModel.movePage(key, direction) },
                            onAddToDock = { viewModel.addToDock(it) },
                            onAppClick = { viewModel.launch(it) },
                            onHideApp = { viewModel.toggleHiddenApp(it) },
                            onOpenFolder = { selectedFolder = it },
                            onCreateFolder = { createFolderApp = it },
                            onMoveToFolder = { folderPickerApp = it },
                            onRenameFolder = { folder, name -> viewModel.renameFolder(folder.id, name) },
                            onDeleteFolder = { viewModel.deleteFolder(it.id) }
                        )
                    }
                }
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
                    .homeGestures(onSwipeUp, onSwipeDown, onDoubleTap)
            )
        }

        AnimatedVisibility(
            visible = settingsOpen,
            enter = slideInVertically(initialOffsetY = { -it / 3 }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it / 3 }) + fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            SettingsSheet(
                viewModel = viewModel,
                homeScreenPage = pagerState.currentPage,
                onSetWallpaper = onSetWallpaper,
                onDismiss = { settingsOpen = false }
            )
        }

        AnimatedVisibility(
            visible = searchOpen,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            SearchOverlay(
                apps = apps,
                onLaunch = {
                    viewModel.launch(it)
                    searchOpen = false
                },
                onDismiss = { searchOpen = false }
            )
        }
    }

    selectedFolder?.let { folder ->
        FolderDialog(
            folder = folders.firstOrNull { it.id == folder.id } ?: folder,
            allApps = apps,
            folders = folders,
            onDismiss = { selectedFolder = null },
            onLaunch = {
                viewModel.launch(it)
                selectedFolder = null
            },
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

    if (lockPromptOpen) {
        AlertDialog(
            onDismissRequest = { lockPromptOpen = false },
            title = { Text("Kunci layar") },
            text = { Text("Aktifkan admin perangkat agar double-tap pada layar dapat mengunci perangkat.") },
            confirmButton = {
                TextButton(onClick = {
                    lockPromptOpen = false
                    SystemActions.requestAdmin(context)
                }) { Text("Aktifkan") }
            },
            dismissButton = {
                TextButton(onClick = { lockPromptOpen = false }) { Text("Batal") }
            }
        )
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
            apps.forEach { app ->
                AppIcon(app = app, showLabel = false, onClick = { onAppClick(app) },
                    extraMenuItems = listOf(AppMenuItem("Lepas dari dock") { onRemoveFromDock(app.stableKey()) }))
            }
        }
    }
}

@Composable
private fun EmptyHomeHint() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            "Layar kosong\nGeser ke atas untuk pencarian · Tahan ikon untuk opsi",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
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
    columns: Int,
    onMovePage: (String, Int) -> Unit,
    onAddToDock: (AppInfo) -> Unit,
    onAppClick: (AppInfo) -> Unit,
    onHideApp: (String) -> Unit,
    onOpenFolder: (LauncherFolder) -> Unit,
    onCreateFolder: (AppInfo) -> Unit,
    onMoveToFolder: (AppInfo) -> Unit,
    onRenameFolder: (LauncherFolder, String) -> Unit,
    onDeleteFolder: (LauncherFolder) -> Unit,
    modifier: Modifier = Modifier
) {
    val rows = entries.chunked(columns)
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
                                    add(AppMenuItem("Sembunyikan aplikasi") { onHideApp(key) })
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
                repeat(columns - rowEntries.size) { Spacer(Modifier.weight(1f)) }
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
                                    bitmap = remember(app.stableKey()) { cachedAppIcon(app, 48) },
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
            containerColor = MaterialTheme.colorScheme.surface) {
            DropdownMenuItem(text = { Text("Ubah nama", color = MaterialTheme.colorScheme.onSurface) }, onClick = { menuOpen = false; renameOpen = true })
            DropdownMenuItem(text = { Text("Hapus folder", color = MaterialTheme.colorScheme.onSurface) }, onClick = { menuOpen = false; onDelete() })
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
    var editingName by remember { mutableStateOf(false) }
    var addMode by remember { mutableStateOf(false) }
    val members = allApps.filter { it.stableKey() in folder.appKeys }
    val available = allApps.filter { app ->
        folders.none { f -> app.stableKey() in f.appKeys } && app.stableKey() !in folder.appKeys
    }

    BackHandler(enabled = addMode) { addMode = false }

    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        val dialogView = LocalView.current
        SideEffect {
            val window = (dialogView.parent as? DialogWindowProvider)?.window
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && window != null) {
                window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                window.setBackgroundBlurRadius(38)
                window.setDimAmount(0.06f)
            }
        }
        Surface(
            modifier = Modifier.fillMaxWidth(0.88f).heightIn(max = 560.dp),
            shape = RoundedCornerShape(32.dp),
            color = Color(0x551D2D43),
            tonalElevation = 0.dp,
            shadowElevation = 30.dp
        ) {
            Column(
                Modifier.fillMaxWidth().background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.30f), Color(0x222A4564))
                    )
                ).border(
                    width = 1.dp,
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.72f), Color.White.copy(alpha = 0.12f))
                    ),
                    shape = RoundedCornerShape(32.dp)
                ).padding(horizontal = 18.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (editingName) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        singleLine = true,
                        label = { Text("Nama folder") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { editingName = false; name = folder.name }) { Text("Batal") }
                        TextButton(onClick = { onRename(name); editingName = false }) { Text("Simpan") }
                    }
                } else {
                    Text(
                        text = folder.name,
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                        modifier = Modifier.clip(RoundedCornerShape(10.dp))
                            .clickable { editingName = true }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                Spacer(Modifier.height(18.dp))

                if (addMode) {
                    Text("Tambah aplikasi", color = Color.White, fontSize = 14.sp)
                    Spacer(Modifier.height(8.dp))
                    Column(
                        Modifier.fillMaxWidth().weight(1f, fill = false)
                            .heightIn(max = 350.dp)
                            .verticalScroll(androidx.compose.foundation.rememberScrollState())
                    ) {
                        available.forEach { app ->
                            Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                                    .clickable { onAdd(app); addMode = false }
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AppIcon(app = app, iconSize = 38.dp, showLabel = false, onClick = {
                                    onAdd(app); addMode = false
                                })
                                Text(app.label, color = Color.White, fontSize = 14.sp,
                                    modifier = Modifier.weight(1f).padding(start = 12.dp))
                                Text("Tambah", color = Color(0xFFA8C8FF), fontSize = 12.sp)
                            }
                        }
                    }
                    TextButton(onClick = { addMode = false }) { Text("Kembali") }
                } else {
                    Column(
                        Modifier.fillMaxWidth().weight(1f, fill = false)
                            .heightIn(max = 390.dp)
                            .verticalScroll(androidx.compose.foundation.rememberScrollState())
                    ) {
                        members.chunked(3).forEach { row ->
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                row.forEach { app ->
                                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                        AppIcon(
                                            app = app,
                                            iconSize = 50.dp,
                                            onClick = { onLaunch(app) },
                                            extraMenuItems = listOf(
                                                AppMenuItem("Keluarkan dari folder") { onRemove(app) }
                                            )
                                        )
                                    }
                                }
                                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { addMode = true }) { Text("Tambah aplikasi") }
                        TextButton(onClick = onDismiss) { Text("Selesai") }
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
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(apps, key = { it.stableKey() }) { app ->
                        AppIcon(
                            app = app,
                            iconSize = 48.dp,
                            onClick = { onCreate(app, name) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}
