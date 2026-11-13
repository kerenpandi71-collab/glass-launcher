package com.kerenpandi.glassberry

import android.app.Presentation
import android.content.Context
import android.os.Bundle
import android.view.Display

class BerryPresentation(context: Context, display: Display) : Presentation(context, display) {

    val berryOS = BerryOS(context)
    lateinit var screenView: BerryScreenView
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        screenView = BerryScreenView(context)
        screenView.os = berryOS
        setContentView(screenView)
        berryOS.showCrashLog()
    }
}
