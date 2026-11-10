package com.kerenpandi.glasslauncher

import android.app.Application
import com.kerenpandi.glasslauncher.data.WidgetRepository

class GlassLauncherApp : Application() {

    lateinit var widgetRepository: WidgetRepository
        private set

    override fun onCreate() {
        super.onCreate()
        widgetRepository = WidgetRepository(this)
    }
}
