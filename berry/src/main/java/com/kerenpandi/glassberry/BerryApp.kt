package com.kerenpandi.glassberry

import android.app.Application

class BerryApp : Application() {

    lateinit var controller: BerryController
        private set

    override fun onCreate() {
        super.onCreate()
        controller = BerryController(this)
        controller.init()
    }
}
