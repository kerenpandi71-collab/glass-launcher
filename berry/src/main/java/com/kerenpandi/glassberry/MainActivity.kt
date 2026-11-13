package com.kerenpandi.glassberry

import android.app.Activity
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Bundle
import android.view.MotionEvent
import android.view.Display
import android.view.KeyEvent
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.WindowInsets
import android.widget.FrameLayout
import android.widget.LinearLayout

class MainActivity : Activity() {

    private lateinit var screenContainer: FrameLayout
    private lateinit var keyboard: BerryKeyboardView

    private var virtualDisplay: android.hardware.display.VirtualDisplay? = null
    private var presentation: BerryPresentation? = null
    private var pendingPresentation: BerryPresentation? = null
    private var fallbackView: BerryScreenView? = null
    private var resumed = false

    private val dispatcher = object : BerryKeyboardView.Listener {
        private fun refresh() {
            presentation?.screenView?.invalidate()
            fallbackView?.invalidate()
        }

        override fun onChar(c: Char) {
            presentation?.berryOS?.onChar(c)
            fallbackView?.os?.onChar(c)
            refresh()
        }

        override fun onBackspace() {
            presentation?.berryOS?.onBackspace()
            fallbackView?.os?.onBackspace()
            refresh()
        }

        override fun onEnter() {
            presentation?.berryOS?.onEnter()
            fallbackView?.os?.onEnter()
            refresh()
        }

        override fun onEscape() {
            presentation?.berryOS?.onEscape()
            fallbackView?.os?.onEscape()
            refresh()
        }
    }

    private val surfaceCallback = object : SurfaceHolder.Callback {
        override fun surfaceCreated(holder: SurfaceHolder) {
        }

        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            stopVirtualDisplay()
            holder.surface?.let { startVirtualDisplay(it, width, height) }
        }

        override fun surfaceDestroyed(holder: SurfaceHolder) {
            stopVirtualDisplay()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installCrashCatcher()
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        hideSystemBars()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(android.graphics.Color.BLACK)
        }

        screenContainer = FrameLayout(this)
        root.addView(
            screenContainer,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
            )
        )

        val surface = SurfaceView(this)
        surface.holder.addCallback(surfaceCallback)
        surface.setOnTouchListener { _, event ->
            handleTouchPassthrough(event)
            true
        }
        screenContainer.addView(
            surface,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        keyboard = BerryKeyboardView(this)
        keyboard.listener = dispatcher
        val kbHeight = (resources.displayMetrics.density * 236f).toInt()
        root.addView(
            keyboard,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, kbHeight
            )
        )

        setContentView(root)
    }

    private fun installCrashCatcher() {
        val prefs = getSharedPreferences("glassberry_crash", MODE_PRIVATE)
        CrashLog.last = prefs.getString("trace", null)
        val previous = CrashLog.last
        if (!previous.isNullOrEmpty()) {
            prefs.edit().remove("trace").apply()
        }
        val upstream = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching {
                prefs.edit().putString("trace", android.util.Log.getStackTraceString(error)).apply()
            }
            upstream?.uncaughtException(thread, error)
        }
    }

    private var touchDownX = 0f
    private var touchDownY = 0f

    private fun handleTouchPassthrough(event: MotionEvent) {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            touchDownX = event.x
            touchDownY = event.y
            return
        }
        if (event.actionMasked != MotionEvent.ACTION_UP) return
        val os = presentation?.berryOS ?: fallbackView?.os ?: return
        val displayId = os.displayId
        if (displayId <= 0 || Build.VERSION.SDK_INT < 29) return
        if (!BerryShizuku.hasPermission()) return
        val dx = event.x - touchDownX
        val dy = event.y - touchDownY
        val cmd = if (dx * dx + dy * dy > 900f) {
            arrayOf(
                "input", "-d", displayId.toString(), "swipe",
                touchDownX.toInt().toString(), touchDownY.toInt().toString(),
                event.x.toInt().toString(), event.y.toInt().toString(), "250"
            )
        } else {
            arrayOf(
                "input", "-d", displayId.toString(), "tap",
                event.x.toInt().toString(), event.y.toInt().toString()
            )
        }
        BerryShizuku.exec(cmd) { _, _ -> }
    }

    private fun hideSystemBars() {
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.hide(WindowInsets.Type.systemBars())
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    private fun startVirtualDisplay(surface: Surface, width: Int, height: Int) {
        val dm = getSystemService(DISPLAY_SERVICE) as DisplayManager
        val w = width.coerceAtLeast(1)
        val h = height.coerceAtLeast(1)
        val dpi = (w / 2f).toInt().coerceIn(120, 560)
        val vd = runCatching {
            dm.createVirtualDisplay(
                "glassberry", w, h, dpi, surface,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION or
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY
            )
        }.getOrNull() ?: return fallbackToDirectView()
        virtualDisplay = vd

        val display: Display? = dm.getDisplay(vd.display.displayId) ?: vd.display
        val pres = display?.let { d ->
            runCatching { BerryPresentation(this, d) }.getOrNull()
        } ?: run {
            vd.release()
            return fallbackToDirectView()
        }

        pendingPresentation = pres
        pres.berryOS.displayId = vd.display.displayId
        showPendingPresentation()
    }

    private fun showPendingPresentation() {
        val pres = pendingPresentation ?: return
        if (!resumed) return
        pendingPresentation = null
        val shown = runCatching { pres.show() }.isSuccess
        if (shown) {
            presentation = pres
        } else {
            runCatching { pres.dismiss() }
            runCatching { virtualDisplay?.release() }
            virtualDisplay = null
            fallbackToDirectView()
        }
    }

    override fun onResume() {
        super.onResume()
        resumed = true
        showPendingPresentation()
    }

    override fun onPause() {
        resumed = false
        super.onPause()
    }

    private fun stopVirtualDisplay() {
        pendingPresentation = null
        runCatching { presentation?.dismiss() }
        presentation = null
        runCatching { virtualDisplay?.release() }
        virtualDisplay = null
    }

    private fun fallbackToDirectView() {
        if (fallbackView == null) {
            val view = BerryScreenView(this)
            val os = BerryOS(this)
            os.invalidate = { view.invalidate() }
            view.os = os
            os.showCrashLog()
            fallbackView = view
            screenContainer.addView(
                view,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            )
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            in KeyEvent.KEYCODE_A..KeyEvent.KEYCODE_Z -> {
                dispatcher.onChar(('a' + (keyCode - KeyEvent.KEYCODE_A)))
                return true
            }
            KeyEvent.KEYCODE_DEL -> {
                dispatcher.onBackspace()
                return true
            }
            KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_DPAD_CENTER -> {
                dispatcher.onEnter()
                return true
            }
            KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_ESCAPE -> {
                dispatcher.onEscape()
                return true
            }
            KeyEvent.KEYCODE_SPACE -> {
                dispatcher.onChar(' ')
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onDestroy() {
        stopVirtualDisplay()
        super.onDestroy()
    }
}
