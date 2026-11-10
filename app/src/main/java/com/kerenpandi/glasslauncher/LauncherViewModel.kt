package com.kerenpandi.glasslauncher

import android.app.ActivityOptions
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

    var widgetIds by mutableStateOf<List<Int>>(emptyList())
        private set

    init {
        refresh()
        widgetIds = repository.loadWidgetIds()
    }

    fun refresh() {
        apps = repository.loadApps()
    }

    fun launch(app: AppInfo) {
        getApplication<Application>().startActivity(repository.launchIntent(app))
    }

    fun launch(app: AppInfo, displayId: Int) {
        val options = ActivityOptions.makeBasic().setLaunchDisplayId(displayId)
        getApplication<Application>().startActivity(repository.launchIntent(app), options.toBundle())
    }

    fun addWidgetId(id: Int) {
        widgetIds = widgetIds + id
        repository.saveWidgetIds(widgetIds)
    }

    fun removeWidgetId(id: Int) {
        widgetIds = widgetIds - id
        repository.deleteAppWidgetId(id)
        repository.saveWidgetIds(widgetIds)
    }
}
