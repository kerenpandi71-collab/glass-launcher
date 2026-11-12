package com.kerenpandi.glasslauncher

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.kerenpandi.glasslauncher.data.AppInfo
import com.kerenpandi.glasslauncher.data.AppRepository
import com.kerenpandi.glasslauncher.data.BackupManager
import com.kerenpandi.glasslauncher.data.BadgeNotificationListener
import com.kerenpandi.glasslauncher.data.LauncherFolder
import com.kerenpandi.glasslauncher.data.LauncherSettings
import com.kerenpandi.glasslauncher.data.OrderStore
import com.kerenpandi.glasslauncher.data.WidgetEntry
import com.kerenpandi.glasslauncher.data.WidgetStore
import com.kerenpandi.glasslauncher.data.stableKey
import java.util.UUID

class LauncherViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = AppRepository(app)
    private val orderStore = OrderStore(app)
    private val widgetStore = WidgetStore(app)
    val settings = LauncherSettings(app)

    private var refreshJob: Job? = null
    private var loadedApps by mutableStateOf<List<AppInfo>>(emptyList())

    var allApps by mutableStateOf<List<AppInfo>>(emptyList())
        private set
    var apps by mutableStateOf<List<AppInfo>>(emptyList())
        private set
    var dockApps by mutableStateOf<List<AppInfo>>(emptyList())
        private set
    var folders by mutableStateOf<List<LauncherFolder>>(emptyList())
        private set
    var widgets by mutableStateOf<List<WidgetEntry>>(emptyList())
        private set
    var badges by mutableStateOf<Map<String, Int>>(emptyMap())
        private set

    val appsPerPage: Int get() = settings.gridColumns * GRID_ROWS

    init {
        refresh()
        viewModelScope.launch {
            snapshotFlow { settings.hiddenApps }.collect { reapplyFilters() }
        }
        viewModelScope.launch {
            BadgeNotificationListener.counts.collect { badges = it }
        }
    }

    fun refresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            val loaded = withContext(Dispatchers.IO) { repository.loadApps() }
            loadedApps = orderStore.applyOrder(loaded)
            reapplyFilters()
            folders = orderStore.loadFolders(loaded)
            widgets = widgetStore.prune(getApplication())
        }
    }

    private fun reapplyFilters() {
        allApps = loadedApps
        apps = loadedApps.filterNot { it.stableKey() in settings.hiddenApps }
        dockApps = if (orderStore.hasDockOrder()) {
            orderStore.applyDockOrder(loadedApps).filterNot { it.stableKey() in settings.hiddenApps }
        } else {
            apps.take(DOCK_CAPACITY)
        }
    }

    fun movePage(key: String, direction: Int) {
        val list = apps.toMutableList()
        val index = list.indexOfFirst { it.stableKey() == key }
        if (index < 0) return
        val perPage = appsPerPage
        val page = index / perPage
        val targetPage = page + direction
        if (targetPage < 0) return
        val item = list.removeAt(index)
        val newIndex = if (direction > 0) {
            ((targetPage + 1) * perPage - 1).coerceAtMost(list.size - 1)
        } else {
            (targetPage * perPage).coerceAtMost(list.size - 1)
        }
        list.add(newIndex.coerceAtLeast(0), item)
        apps = list
        orderStore.saveOrder(list + loadedApps.filter { it.stableKey() in settings.hiddenApps })
    }

    fun addToDock(app: AppInfo) {
        val key = app.stableKey()
        if (dockApps.any { it.stableKey() == key } || dockApps.size >= DOCK_CAPACITY) return
        dockApps = dockApps + app
        orderStore.saveDockOrder(dockApps)
    }

    fun removeFromDock(key: String) {
        dockApps = dockApps.filterNot { it.stableKey() == key }
        orderStore.saveDockOrder(dockApps)
    }

    fun createFolder(first: AppInfo, second: AppInfo, name: String = "Folder") {
        val keys = listOf(first.stableKey(), second.stableKey()).distinct()
        if (keys.size < 2) return
        val next = folders.filterNot { folder -> folder.appKeys.any { it in keys } } +
            LauncherFolder(UUID.randomUUID().toString(), name.ifBlank { "Folder" }, keys)
        folders = next
        orderStore.saveFolders(next)
    }

    fun addAppToFolder(app: AppInfo, folderId: String) {
        val key = app.stableKey()
        val next = folders.map { folder ->
            if (folder.id == folderId) folder.copy(appKeys = (folder.appKeys + key).distinct())
            else folder.copy(appKeys = folder.appKeys.filterNot { it == key })
        }
        folders = next
        orderStore.saveFolders(next)
    }

    fun removeAppFromFolder(app: AppInfo, folderId: String) {
        val next = folders.map { folder ->
            if (folder.id == folderId) folder.copy(appKeys = folder.appKeys.filterNot { it == app.stableKey() })
            else folder
        }.filter { it.appKeys.isNotEmpty() }
        folders = next
        orderStore.saveFolders(next)
    }

    fun renameFolder(folderId: String, name: String) {
        val next = folders.map { if (it.id == folderId) it.copy(name = name.ifBlank { "Folder" }) else it }
        folders = next
        orderStore.saveFolders(next)
    }

    fun deleteFolder(folderId: String) {
        val next = folders.filterNot { it.id == folderId }
        folders = next
        orderStore.saveFolders(next)
    }

    fun toggleHiddenApp(key: String) {
        settings.toggleHiddenApp(key)
    }

    fun addWidget(id: Int) {
        if (widgets.any { it.id == id }) return
        widgets = widgets + WidgetEntry(id, WidgetStore.DEFAULT_WIDTH, WidgetStore.DEFAULT_HEIGHT)
        widgetStore.save(widgets)
    }

    fun removeWidget(id: Int) {
        widgets = widgets.filterNot { it.id == id }
        widgetStore.save(widgets)
    }

    fun resizeWidget(id: Int, widthDp: Int, heightDp: Int) {
        widgets = widgets.map {
            if (it.id == id) it.copy(widthDp = widthDp, heightDp = heightDp) else it
        }
    }

    fun saveWidgets() {
        widgetStore.save(widgets)
    }

    fun exportLayout(uri: Uri) {
        val context = getApplication<Application>()
        viewModelScope.launch(Dispatchers.IO) {
            val json = BackupManager.export(context)
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(json.toByteArray(Charsets.UTF_8))
                }
            }
        }
    }

    fun importLayout(uri: Uri) {
        val context = getApplication<Application>()
        viewModelScope.launch(Dispatchers.IO) {
            val text = runCatching {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    stream.readBytes().toString(Charsets.UTF_8)
                }
            }.getOrNull() ?: return@launch
            BackupManager.import(context, text)
            withContext(Dispatchers.Main) { reloadAfterRestore() }
        }
    }

    fun reloadAfterRestore() {
        settings.reload()
        refresh()
    }

    fun launch(app: AppInfo) { repository.launch(getApplication(), app) }

    companion object {
        const val GRID_ROWS = 6
        const val DOCK_CAPACITY = 4
    }
}
