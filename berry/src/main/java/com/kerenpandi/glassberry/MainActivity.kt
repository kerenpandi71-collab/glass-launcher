package com.kerenpandi.glassberry

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.PowerManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.kerenpandi.glassberry.ui.BerryScreen
import com.kerenpandi.glassberry.ui.BerryTheme

class MainActivity : ComponentActivity() {

    private val controller: BerryController
        get() = (application as BerryApp).controller

    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        hideSystemBars()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!controller.handleBack()) moveTaskToBack(true)
            }
        })

        setContent {
            BerryTheme {
                BerryScreen(controller)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        acquireWakeLock()
    }

    override fun onResume() {
        super.onResume()
        hideSystemBars()
        acquireWakeLock()
        controller.onHostResumed()
    }

    @Suppress("DEPRECATION")
    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        runCatching {
            wakeLock = getSystemService(PowerManager::class.java)
                .newWakeLock(
                    PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ON_AFTER_RELEASE,
                    "GlassBerry::Screen",
                )
                .apply {
                    setReferenceCounted(false)
                    acquire(WAKELOCK_TIMEOUT_MS)
                }
        }
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        runCatching {
            @Suppress("DEPRECATION")
            wakeLock?.acquire(WAKELOCK_TIMEOUT_MS)
        }
    }

    override fun onStop() {
        wakeLock?.let { runCatching { it.release() } }
        wakeLock = null
        controller.shutdown()
        super.onStop()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    private fun hideSystemBars() {
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private companion object {
        const val WAKELOCK_TIMEOUT_MS = 30 * 60 * 1000L
    }
}
