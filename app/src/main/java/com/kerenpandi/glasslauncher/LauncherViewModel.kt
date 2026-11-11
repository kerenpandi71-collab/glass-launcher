package com.kerenpandi.glasslauncher

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.kerenpandi.glasslauncher.data.AppInfo
import com.kerenpandi.glasslauncher.data.AppRepository
import com.kerenpandi.glasslauncher.data.LauncherFolder
import com.kerenpandi.glasslauncher.data.OrderStore
import com.kerenpandi.glasslauncher.data.stableKey
import java.util.UUID

class LauncherViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = AppRepository(app)
    private val orderStore = OrderStore(app)

    var apps by mutableStateOf<List<AppInfo>>(emptyList())
        private set
    var dockApps by mutableStateOf<List<AppInfo>>(emptyList())
        private set
    var folders by mutableStateOf<List<LauncherFolder>>(emptyList())
        private set

    init { refresh() }

    fun refresh() {
        val loaded = repository.loadApps()
        apps = orderStore.applyOrder(loaded)
        dockApps = if (orderStore.hasDockOrder()) orderStore.applyDockOrder(loaded)
            else apps.take(DOCK_CAPACITY)
        folders = orderStore.loadFolders(loaded)
    }

    fun movePage(key: String, direction: Int) {
        val list = apps.toMutableList()
        val index = list.indexOfFirst { it.stableKey() == key }
        if (index < 0) return
        val page = index / APPS_PER_PAGE
        val targetPage = page + direction
        if (targetPage < 0) return
        val item = list.removeAt(index)
        val newIndex = if (direction > 0) {
            ((targetPage + 1) * APPS_PER_PAGE - 1).coerceAtMost(list.size - 1)
        } else {
            (targetPage * APPS_PER_PAGE).coerceAtMost(list.size - 1)
        }
        list.add(newIndex.coerceAtLeast(0), item)
        apps = list
        orderStore.saveOrder(list)
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

    fun launch(app: AppInfo) { repository.launch(getApplication(), app) }
    fun launch(app: AppInfo, displayId: Int) { repository.launch(getApplication(), app, displayId) }

    companion object {
        const val APPS_PER_PAGE = 24
        const val DOCK_CAPACITY = 4
    }
}
