package com.kerenpandi.glasslauncher

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.kerenpandi.glasslauncher.data.AppInfo
import com.kerenpandi.glasslauncher.data.AppRepository

class LauncherViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = AppRepository(app)

    var apps by mutableStateOf<List<AppInfo>>(emptyList())
        private set

    init {
        refresh()
    }

    fun refresh() {
        apps = repository.loadApps()
    }

    fun launch(app: AppInfo) {
        repository.launch(getApplication(), app)
    }

    fun launch(app: AppInfo, displayId: Int) {
        repository.launch(getApplication(), app, displayId)
    }
}
