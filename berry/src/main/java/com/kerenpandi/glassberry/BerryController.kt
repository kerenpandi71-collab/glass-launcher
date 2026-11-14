package com.kerenpandi.glassberry

import android.app.Application
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.view.InputDevice
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.kerenpandi.glassberry.service.BerryUserService
import rikka.shizuku.Shizuku
import java.util.concurrent.Executors

enum class ShizukuState { NOT_INSTALLED, NOT_RUNNING, NO_PERMISSION, CONNECTING, READY }

data class AppEntry(
    val label: String,
    val packageName: String,
    val component: String,
    val icon: ImageBitmap? = null,
)

/** Owns the Shizuku helper connection, the virtual display and all input sent to it. */
class BerryController(private val app: Application) {

    var shizukuState by mutableStateOf(ShizukuState.NOT_RUNNING)
        private set
    var displayId by mutableIntStateOf(-1)
        private set
    var currentApp by mutableStateOf<AppEntry?>(null)
        private set
    var pickerOpen by mutableStateOf(true)
    var message by mutableStateOf<String?>(null)
    var search by mutableStateOf("")
    var busy by mutableStateOf(false)
        private set
    var apps by mutableStateOf<List<AppEntry>>(emptyList())
        private set
    var showDisplaySettings by mutableStateOf(false)
    var dpiScale by mutableStateOf(DEFAULT_DPI_SCALE)
        private set
    var resScale by mutableStateOf(1f)
        private set
    var keyboardHidden by mutableStateOf(false)
        private set
    var touchpadHidden by mutableStateOf(false)
        private set
    var androidKeyboardEnabled by mutableStateOf(true)
        private set
    var landscapeMode by mutableStateOf(false)
        private set
    var batteryPct by mutableIntStateOf(-1)
        private set
    var fps by mutableIntStateOf(0)

    val deviceDensityDpi: Int
        get() = app.resources.displayMetrics.densityDpi

    val surfaceSize: Pair<Int, Int>
        get() = surfaceW to surfaceH

    val displaySize: Pair<Int, Int>
        get() = displayW to displayH

    val filteredApps: List<AppEntry>
        get() = if (search.isBlank()) apps
        else apps.filter {
            it.label.contains(search, ignoreCase = true) ||
                it.packageName.contains(search, ignoreCase = true)
        }

    @Volatile
    private var service: IBerryService? = null
    private var binding = false
    @Volatile
    private var hostActive = true
    @Volatile
    private var shuttingDown = false
    private var pendingEnsure = false
    private val worker = Executors.newSingleThreadExecutor()
    private val inputWorker = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())

    private var surface: Surface? = null
    private var surfaceW = 0
    private var surfaceH = 0
    private var displayW = 0
    private var displayH = 0

    private val keyDownTimes = HashMap<Int, Long>()
    var cursorX by mutableStateOf(0f)
        private set
    var cursorY by mutableStateOf(0f)
        private set
    private var padDown = false
    private var padDownTime = 0L
    private val hoverLock = Any()
    private var hoverQueued = false
    private var hoverDirty = false
    private var pendingHoverX = 0f
    private var pendingHoverY = 0f
    private data class ActiveTouch(val id: Int, var x: Float, var y: Float)
    private val touchLock = Any()
    private val activeTouches = LinkedHashMap<String, ActiveTouch>()
    private var touchDownTime = 0L

    private val userServiceArgs by lazy {
        Shizuku.UserServiceArgs(ComponentName(app.packageName, BerryUserService::class.java.name))
            .daemon(false)
            .processNameSuffix("berry")
            .debuggable(BuildConfig.DEBUG)
            .version(BuildConfig.VERSION_CODE)
    }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            if (binder == null || !binder.pingBinder()) return
            service = IBerryService.Stub.asInterface(binder)
            main.post {
                binding = false
                shizukuState = ShizukuState.READY
                message = null
                ensureDisplay()
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
            main.post {
                binding = false
                displayId = -1
                currentApp = null
                pickerOpen = true
                refresh()
            }
        }
    }

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            if (level >= 0 && scale > 0) batteryPct = level * 100 / scale
        }
    }

    fun init() {
        runCatching {
            val sticky = app.registerReceiver(
                batteryReceiver,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            )
            sticky?.let { batteryReceiver.onReceive(app, it) }
        }
        Shizuku.addBinderReceivedListenerSticky { main.post { refresh() } }
        Shizuku.addBinderDeadListener {
            main.post {
                service = null
                binding = false
                displayId = -1
                currentApp = null
                pickerOpen = true
                refresh()
            }
        }
        Shizuku.addRequestPermissionResultListener { _, _ -> main.post { refresh() } }
        loadApps()
        refresh()
    }

    fun onHostResumed() {
        hostActive = true
        shuttingDown = false
        refresh()
    }

    fun refresh() {
        if (!hostActive || shuttingDown) return
        if (service != null) {
            shizukuState = ShizukuState.READY
            return
        }
        shizukuState = try {
            when {
                !Shizuku.pingBinder() ->
                    if (isShizukuInstalled()) ShizukuState.NOT_RUNNING else ShizukuState.NOT_INSTALLED
                Shizuku.isPreV11() -> {
                    message = "Versi Shizuku terlalu lama. Perbarui Shizuku dulu."
                    ShizukuState.NOT_RUNNING
                }
                Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED ->
                    ShizukuState.NO_PERMISSION
                else -> {
                    bind()
                    ShizukuState.CONNECTING
                }
            }
        } catch (t: Throwable) {
            message = "Shizuku error: ${t.message}"
            ShizukuState.NOT_RUNNING
        }
    }

    private fun bind() {
        if (binding) return
        binding = true
        try {
            Shizuku.bindUserService(userServiceArgs, connection)
        } catch (t: Throwable) {
            binding = false
            message = "Gagal menyalakan layanan Shizuku: ${t.message}"
        }
    }

    fun requestPermission() {
        try {
            if (Shizuku.shouldShowRequestPermissionRationale()) {
                message = "Izin pernah ditolak. Buka Shizuku → Aplikasi yang diizinkan → nyalakan GlassBerry."
            } else {
                Shizuku.requestPermission(REQUEST_CODE)
            }
        } catch (t: Throwable) {
            message = "Shizuku error: ${t.message}"
        }
    }

    fun openShizuku(ctx: Context) {
        val launch = app.packageManager.getLaunchIntentForPackage(SHIZUKU_PACKAGE)
        val intent = launch ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://shizuku.rikka.app/download/"))
        runCatching { ctx.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }

    private fun isShizukuInstalled(): Boolean = try {
        app.packageManager.getPackageInfo(SHIZUKU_PACKAGE, PackageManager.PackageInfoFlags.of(0))
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }

    private fun loadApps() {
        worker.execute {
            val list = runCatching {
                val pm = app.packageManager
                val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0L))
                    .filter { it.activityInfo.packageName != app.packageName }
                    .map {
                        val icon = runCatching {
                            it.loadIcon(pm).toBitmap(96, 96).asImageBitmap()
                        }.getOrNull()
                        AppEntry(
                            label = it.loadLabel(pm).toString(),
                            packageName = it.activityInfo.packageName,
                            component = "${it.activityInfo.packageName}/${it.activityInfo.name}",
                            icon = icon,
                        )
                    }
                    .distinctBy { it.component }
                    .sortedBy { it.label.lowercase() }
            }.getOrDefault(emptyList())
            main.post { apps = list }
        }
    }

    // ---- surface & display --------------------------------------------------

    fun onSurfaceReady(s: Surface, width: Int, height: Int) {
        surface = s
        surfaceW = width
        surfaceH = height
        ensureDisplay()
    }

    fun onSurfaceGone() {
        releaseAllTouchPointers()
        surface = null
        surfaceW = 0
        surfaceH = 0
    }

    fun retryDisplay() {
        message = null
        if (service == null) refresh() else ensureDisplay()
    }

    private fun ensureDisplay() {
        if (!hostActive || shuttingDown) return
        val svc = service ?: return
        val s = surface ?: return
        if (surfaceW <= 0 || surfaceH <= 0) return
        if (busy) {
            pendingEnsure = true
            return
        }
        val w = surfaceW and 1.inv()
        val h = surfaceH and 1.inv()
        val dpi = (app.resources.displayMetrics.densityDpi * dpiScale).toInt().coerceIn(120, 480)
        val existing = displayId
        busy = true
        worker.execute {
            val result = runCatching {
                if (existing >= 0) {
                    svc.setSurface(Bundle().apply { putParcelable("surface", s) })
                    if (w != displayW || h != displayH) svc.resizeDisplay(w, h, dpi)
                    Bundle().apply { putInt("displayId", existing) }
                } else {
                    svc.createDisplay(Bundle().apply {
                        putParcelable("surface", s)
                        putInt("width", w)
                        putInt("height", h)
                        putInt("dpi", dpi)
                    })
                }
            }
            main.post {
                busy = false
                result.onSuccess { b ->
                    val id = b.getInt("displayId", -1)
                    if (id >= 0) {
                        if (displayId < 0) setRecentsGesture(false)
                        displayId = id
                        displayW = w
                        displayH = h
                    } else {
                        message = "Gagal membuat layar virtual: ${b.getString("error")}"
                    }
                }.onFailure {
                    message = "Gagal membuat layar virtual: ${it.message}"
                }
                if (pendingEnsure) {
                    pendingEnsure = false
                    ensureDisplay()
                }
            }
        }
    }

    fun shutdown() {
        if (shuttingDown) return
        hostActive = false
        shuttingDown = true
        pendingEnsure = false
        releaseAllTouchPointers()

        val svc = service
        val wasBinding = binding
        binding = false
        displayId = -1
        displayW = 0
        displayH = 0
        currentApp = null
        pickerOpen = true
        busy = false
        keyDownTimes.clear()

        runCatching {
            worker.execute {
                runCatching {
                    inputWorker.execute {
                        try {
                            if (svc != null) {
                                if (imeSuppressed) {
                                    disabledImes.toList().forEach { id -> runCatching { svc.runShell("ime enable $id") } }
                                    disabledImes.clear()
                                    imeSuppressed = false
                                }
                                runCatching { svc.runShell("settings put secure swipe_up_to_switch_apps_enabled 1") }
                                runCatching { svc.releaseDisplay() }
                            }
                            if (wasBinding || svc != null) {
                                runCatching { Shizuku.unbindUserService(userServiceArgs, connection, true) }
                            }
                        } finally {
                            service = null
                            main.post {
                                if (hostActive) {
                                    shuttingDown = false
                                    refresh()
                                } else {
                                    shizukuState = ShizukuState.CONNECTING
                                }
                            }
                        }
                    }
                }.onFailure {
                    if (svc != null) runCatching { svc.releaseDisplay() }
                    if (wasBinding || svc != null) runCatching {
                        Shizuku.unbindUserService(userServiceArgs, connection, true)
                    }
                    service = null
                }
            }
        }.onFailure {
            if (svc != null) runCatching { svc.releaseDisplay() }
            if (wasBinding || svc != null) runCatching {
                Shizuku.unbindUserService(userServiceArgs, connection, true)
            }
            service = null
        }
    }

    // ---- apps ---------------------------------------------------------------

    fun openPicker() {
        search = ""
        pickerOpen = true
        if (displayW > 0 && displayH > 0 && cursorX <= 0f && cursorY <= 0f) {
            cursorX = displayW / 2f
            cursorY = displayH / 2f
        }
    }

    fun closePicker() {
        if (currentApp != null) pickerOpen = false
    }

    fun launch(entry: AppEntry) {
        val svc = service
        val id = displayId
        if (svc == null || id < 0) {
            message = "Layar virtual belum siap. Tunggu sebentar lalu coba lagi."
            return
        }
        pickerOpen = false
        currentApp = entry
        message = null
        worker.execute {
            val out = runCatching { svc.launchApp(entry.component, id) }
                .getOrElse { "Error: ${it.message}" }
            main.post {
                when {
                    out.contains("brought to the front", true) || out.contains("current task", true) ->
                        message = "${entry.label} sudah terbuka di layar utama. Tutup dulu dari menu aplikasi terbaru, lalu buka lagi di sini."
                    out.contains("Error", true) || out.contains("Exception", true) ->
                        message = "Gagal membuka ${entry.label}: ${out.trim().take(200)}"
                }
            }
        }
    }

    // ---- trackpad ------------------------------------------------------------

    fun pickerCursorMove(dx: Float, dy: Float) {
        if (displayW <= 0 || displayH <= 0) return
        if (cursorX <= 0f && cursorY <= 0f) {
            cursorX = displayW / 2f
            cursorY = displayH / 2f
        }
        cursorX = (cursorX + dx * PAD_SENSITIVITY).coerceIn(0f, displayW.toFloat())
        cursorY = (cursorY + dy * PAD_SENSITIVITY).coerceIn(0f, displayH.toFloat())
    }

    fun trackpadHover(dx: Float, dy: Float) {
        val svc = service ?: return
        val id = displayId
        if (id < 0 || displayW <= 0 || displayH <= 0) return
        if (cursorX <= 0f && cursorY <= 0f) {
            cursorX = displayW / 2f
            cursorY = displayH / 2f
        }
        cursorX = (cursorX + dx * PAD_SENSITIVITY).coerceIn(0f, displayW.toFloat())
        cursorY = (cursorY + dy * PAD_SENSITIVITY).coerceIn(0f, displayH.toFloat())
        synchronized(hoverLock) {
            pendingHoverX = cursorX
            pendingHoverY = cursorY
            hoverDirty = true
            if (hoverQueued) return
            hoverQueued = true
        }
        inputWorker.execute {
            try {
                while (true) {
                    val point = synchronized(hoverLock) {
                        if (!hoverDirty) null
                        else {
                            hoverDirty = false
                            pendingHoverX to pendingHoverY
                        }
                    } ?: break
                    injectHoverMotion(svc, id, point.first, point.second)
                }
            } finally {
                synchronized(hoverLock) { hoverQueued = false }
            }
        }
    }

    fun trackpadDragStart() {
        val svc = service ?: return
        val id = displayId
        if (id < 0 || padDown || displayW <= 0 || displayH <= 0) return
        if (cursorX <= 0f && cursorY <= 0f) {
            cursorX = displayW / 2f
            cursorY = displayH / 2f
        }
        padDown = true
        padDownTime = SystemClock.uptimeMillis()
        injectTouchMotion(svc, id, MotionEvent.ACTION_DOWN, cursorX, cursorY, padDownTime, padDownTime)
    }

    fun trackpadDragMove(dx: Float, dy: Float) {
        val svc = service ?: return
        val id = displayId
        if (id < 0 || !padDown || displayW <= 0 || displayH <= 0) return
        cursorX = (cursorX + dx * PAD_SENSITIVITY).coerceIn(0f, displayW.toFloat())
        cursorY = (cursorY + dy * PAD_SENSITIVITY).coerceIn(0f, displayH.toFloat())
        injectTouchMotion(svc, id, MotionEvent.ACTION_MOVE, cursorX, cursorY, padDownTime, SystemClock.uptimeMillis())
    }

    fun trackpadDragEnd() {
        val svc = service ?: return
        val id = displayId
        if (id < 0 || !padDown) return
        padDown = false
        injectTouchMotion(svc, id, MotionEvent.ACTION_UP, cursorX, cursorY, padDownTime, SystemClock.uptimeMillis())
    }

    fun trackpadScroll(dx: Float, dy: Float) {
        val svc = service ?: return
        val id = displayId
        if (id < 0 || displayW <= 0 || displayH <= 0) return
        if (cursorX <= 0f && cursorY <= 0f) {
            cursorX = displayW / 2f
            cursorY = displayH / 2f
        }
        val now = SystemClock.uptimeMillis()
        val properties = arrayOf(
            MotionEvent.PointerProperties().apply {
                this.id = 0
                toolType = MotionEvent.TOOL_TYPE_MOUSE
            },
        )
        val coordinates = arrayOf(
            MotionEvent.PointerCoords().apply {
                x = cursorX
                y = cursorY
                setAxisValue(MotionEvent.AXIS_VSCROLL, -dy / 48f)
                setAxisValue(MotionEvent.AXIS_HSCROLL, -dx / 48f)
                pressure = 1f
                size = 1f
            },
        )
        val event = MotionEvent.obtain(
            now, now, MotionEvent.ACTION_SCROLL, 1, properties, coordinates,
            0, 0, 1f, 1f, 0, 0, InputDevice.SOURCE_MOUSE, 0,
        )
        try {
            inject(svc, event, id)
        } finally {
            event.recycle()
        }
    }

    private fun injectHoverMotion(svc: IBerryService, id: Int, x: Float, y: Float) {
        val now = SystemClock.uptimeMillis()
        val properties = arrayOf(
            MotionEvent.PointerProperties().apply {
                this.id = 0
                toolType = MotionEvent.TOOL_TYPE_MOUSE
            },
        )
        val coordinates = arrayOf(
            MotionEvent.PointerCoords().apply {
                this.x = x
                this.y = y
                pressure = 1f
                size = 1f
            },
        )
        val event = MotionEvent.obtain(
            now, now, MotionEvent.ACTION_HOVER_MOVE, 1, properties, coordinates,
            0, 0, 1f, 1f, 0, 0, InputDevice.SOURCE_MOUSE, 0,
        )
        try {
            inject(svc, event, id)
        } finally {
            event.recycle()
        }
    }

    fun trackpadTap() {
        val svc = service ?: return
        val id = displayId
        if (id < 0 || displayW <= 0 || displayH <= 0) return
        if (cursorX <= 0f && cursorY <= 0f) {
            cursorX = displayW / 2f
            cursorY = displayH / 2f
        }
        val downTime = SystemClock.uptimeMillis()
        injectTouchMotion(svc, id, MotionEvent.ACTION_DOWN, cursorX, cursorY, downTime, downTime)
        injectTouchMotion(svc, id, MotionEvent.ACTION_UP, cursorX, cursorY, downTime, downTime + 60)
    }

    private fun injectTouchMotion(
        svc: IBerryService,
        id: Int,
        action: Int,
        x: Float,
        y: Float,
        downTime: Long,
        eventTime: Long,
    ) {
        val properties = arrayOf(
            MotionEvent.PointerProperties().apply {
                this.id = 0
                toolType = MotionEvent.TOOL_TYPE_FINGER
            },
        )
        val coordinates = arrayOf(
            MotionEvent.PointerCoords().apply {
                this.x = x
                this.y = y
                pressure = 1f
                size = 1f
            },
        )
        val event = MotionEvent.obtain(
            downTime, eventTime, action, 1, properties, coordinates,
            0, 0, 1f, 1f, 0, 0, InputDevice.SOURCE_TOUCHSCREEN, 0,
        )
        try {
            inject(svc, event, id)
        } finally {
            event.recycle()
        }
    }

    // ---- display settings ----------------------------------------------------

    fun applyDisplayConfig(
        newDpiScale: Float,
        newResScale: Float,
        hideKeyboard: Boolean,
        hideTouchpad: Boolean,
        androidKbEnabled: Boolean,
        landscape: Boolean,
    ) {
        keyboardHidden = hideKeyboard
        touchpadHidden = hideTouchpad
        androidKeyboardEnabled = androidKbEnabled
        landscapeMode = landscape
        setImeSuppressed(!androidKbEnabled)
        val resChanged = kotlin.math.abs(newResScale - resScale) > 0.001f
        dpiScale = newDpiScale.coerceIn(0.4f, 1.2f)
        resScale = newResScale.coerceIn(0.5f, 1f)
        val svc = service ?: return
        val id = displayId
        if (id < 0 || displayW <= 0 || displayH <= 0) return
        if (resChanged) {
            message = "Resolusi diubah ke ${(surfaceW * resScale).toInt()}x${(surfaceH * resScale).toInt()}"
            return
        }
        val dpi = (app.resources.displayMetrics.densityDpi * dpiScale).toInt().coerceIn(120, 480)
        worker.execute {
            val result = runCatching { svc.resizeDisplay(displayW, displayH, dpi) }
            main.post {
                result.onSuccess { b ->
                    val err = b?.getString("error")
                    if (err != null) {
                        message = "Gagal mengubah tampilan: $err"
                    } else {
                        message = "Tampilan: ${displayW}x${displayH} @ ${dpi}dpi"
                    }
                }.onFailure {
                    message = "Gagal mengubah tampilan: ${it.message}"
                }
            }
        }
    }

    private fun setRecentsGesture(enabled: Boolean) {
        val svc = service ?: return
        worker.execute {
            runCatching {
                if (enabled) {
                    svc.runShell("cmd overlay enable com.android.internal.systemui.navbar.gesture")
                    svc.runShell("cmd overlay disable com.android.internal.systemui.navbar.threebutton")
                    svc.runShell("settings put secure swipe_up_to_switch_apps_enabled 1")
                    svc.runShell("settings put secure navigation_gesture_on 1")
                } else {
                    svc.runShell("cmd overlay disable com.android.internal.systemui.navbar.gesture")
                    svc.runShell("cmd overlay enable com.android.internal.systemui.navbar.threebutton")
                    svc.runShell("settings put secure swipe_up_to_switch_apps_enabled 0")
                    svc.runShell("settings put secure navigation_gesture_on 0")
                }
            }
        }
    }

    // ---- android IME suppression ----------------------------------------------

    private var imeSuppressed = false
    private val disabledImes = mutableListOf<String>()

    private fun setImeSuppressed(blocked: Boolean) {
        val effective = blocked || !androidKeyboardEnabled
        if (effective == imeSuppressed) return
        imeSuppressed = effective
        val svc = service ?: return
        worker.execute {
            runCatching {
                if (effective) {
                    val ids = svc.runShell("ime list -s")
                        .lines()
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                    disabledImes.clear()
                    disabledImes.addAll(ids)
                    ids.forEach { svc.runShell("ime disable $it") }
                } else {
                    disabledImes.toList().forEach { svc.runShell("ime enable $it") }
                    disabledImes.clear()
                }
            }
        }
    }

    // ---- QWERTY keyboard input ----------------------------------------------

    fun sendChar(c: Char) {
        setImeSuppressed(true)
        if (pickerOpen) {
            search += c
            return
        }
        sendText(c.toString())
    }

    fun sendBackspace() {
        setImeSuppressed(true)
        if (pickerOpen) {
            search = search.dropLast(1)
            return
        }
        sendKey(KeyEvent.KEYCODE_DEL, true)
        sendKey(KeyEvent.KEYCODE_DEL, false)
    }

    fun sendEnter() {
        setImeSuppressed(true)
        if (pickerOpen) {
            filteredApps.firstOrNull()?.let { launch(it) }
            return
        }
        sendKey(KeyEvent.KEYCODE_ENTER, true)
        sendKey(KeyEvent.KEYCODE_ENTER, false)
    }

    fun sendMenu() {
        openPicker()
    }

    fun sendHomeKey() {
        sendKey(KeyEvent.KEYCODE_HOME, true)
        sendKey(KeyEvent.KEYCODE_HOME, false)
    }

    fun goHome() {
        openPicker()
    }

    fun sendBackKey() {
        sendKey(KeyEvent.KEYCODE_BACK, true)
        sendKey(KeyEvent.KEYCODE_BACK, false)
    }

    fun sendKey(keyCode: Int, down: Boolean) {
        if (down) setImeSuppressed(true)
        val svc = service ?: return
        val id = displayId
        if (id < 0) return
        val now = SystemClock.uptimeMillis()
        val downTime = if (down) now.also { keyDownTimes[keyCode] = it } else keyDownTimes.remove(keyCode) ?: now
        val ev = KeyEvent(
            downTime, now,
            if (down) KeyEvent.ACTION_DOWN else KeyEvent.ACTION_UP,
            keyCode, 0, 0, KeyCharacterMap.VIRTUAL_KEYBOARD, 0, 0,
            InputDevice.SOURCE_KEYBOARD
        )
        inject(svc, ev, id)
    }

    fun sendText(text: String): Boolean {
        if (text.isEmpty()) return true
        setImeSuppressed(true)
        val svc = service ?: return false
        val id = displayId
        if (id < 0) return false
        val events = runCatching {
            KeyCharacterMap.load(KeyCharacterMap.VIRTUAL_KEYBOARD).getEvents(text.toCharArray())
        }.getOrNull()
        if (events == null) {
            message = "Karakter ini tidak didukung keyboard virtual. Coba huruf, angka, atau tanda baca umum."
            return false
        }
        events.forEach { inject(svc, it, id) }
        return true
    }

    // ---- direct touch -------------------------------------------------------

    fun forwardTouch(e: MotionEvent, viewW: Int, viewH: Int): Boolean {
        val svc = service ?: return true
        val id = displayId
        if (id < 0 || pickerOpen) return true
        if (e.actionMasked == MotionEvent.ACTION_DOWN) setImeSuppressed(false)
        if (displayW <= 0 || displayH <= 0 || viewW <= 0 || viewH <= 0) return true
        if (e.actionMasked == MotionEvent.ACTION_SCROLL) {
            inject(svc, e, id)
            return true
        }
        synchronized(touchLock) {
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    releaseDirectTouchesLocked()
                    addTouchLocked(directTouchKey(e.getPointerId(0)),
                        e.getX(0) * displayW / viewW, e.getY(0) * displayH / viewH)
                }
                MotionEvent.ACTION_POINTER_DOWN -> {
                    updateDirectTouchesLocked(e, viewW, viewH)
                    val i = e.actionIndex
                    addTouchLocked(directTouchKey(e.getPointerId(i)),
                        e.getX(i) * displayW / viewW, e.getY(i) * displayH / viewH)
                }
                MotionEvent.ACTION_MOVE -> {
                    updateDirectTouchesLocked(e, viewW, viewH)
                    if (activeTouches.isNotEmpty()) dispatchTouchLocked(MotionEvent.ACTION_MOVE)
                }
                MotionEvent.ACTION_POINTER_UP -> {
                    updateDirectTouchesLocked(e, viewW, viewH)
                    removeTouchLocked(directTouchKey(e.getPointerId(e.actionIndex)))
                }
                MotionEvent.ACTION_UP -> removeTouchLocked(directTouchKey(e.getPointerId(0)))
                MotionEvent.ACTION_CANCEL -> releaseDirectTouchesLocked()
            }
        }
        return true
    }

    private fun directTouchKey(pointerId: Int) = "direct:$pointerId"

    private fun updateDirectTouchesLocked(e: MotionEvent, viewW: Int, viewH: Int) {
        for (i in 0 until e.pointerCount) {
            activeTouches[directTouchKey(e.getPointerId(i))]?.let {
                it.x = e.getX(i) * displayW / viewW
                it.y = e.getY(i) * displayH / viewH
            }
        }
    }

    private fun addTouchLocked(key: String, x: Float, y: Float) {
        if (activeTouches.containsKey(key) || activeTouches.size >= 10) return
        val usedIds = activeTouches.values.map { it.id }.toSet()
        val pointerId = (0..31).firstOrNull { it !in usedIds } ?: return
        val wasEmpty = activeTouches.isEmpty()
        activeTouches[key] = ActiveTouch(pointerId, x, y)
        val now = SystemClock.uptimeMillis()
        if (wasEmpty) touchDownTime = now
        val index = activeTouches.size - 1
        dispatchTouchLocked(if (wasEmpty) MotionEvent.ACTION_DOWN else
            MotionEvent.ACTION_POINTER_DOWN or (index shl MotionEvent.ACTION_POINTER_INDEX_SHIFT))
    }

    private fun removeTouchLocked(key: String) {
        val index = activeTouches.keys.indexOf(key)
        if (index < 0) return
        val action = if (activeTouches.size == 1) MotionEvent.ACTION_UP else
            MotionEvent.ACTION_POINTER_UP or (index shl MotionEvent.ACTION_POINTER_INDEX_SHIFT)
        dispatchTouchLocked(action)
        activeTouches.remove(key)
        if (activeTouches.isEmpty()) touchDownTime = 0L
    }

    private fun releaseDirectTouchesLocked() {
        activeTouches.keys.filter { it.startsWith("direct:") }.toList().forEach(::removeTouchLocked)
    }

    private fun releaseAllTouchPointers() {
        synchronized(touchLock) {
            activeTouches.keys.toList().forEach(::removeTouchLocked)
        }
    }

    private fun dispatchTouchLocked(action: Int) {
        val svc = service ?: return
        val id = displayId
        val entries = activeTouches.entries.toList()
        if (id < 0 || entries.isEmpty()) return
        val now = SystemClock.uptimeMillis()
        val properties = Array(entries.size) { i ->
            MotionEvent.PointerProperties().apply {
                this.id = entries[i].value.id
                toolType = MotionEvent.TOOL_TYPE_FINGER
            }
        }
        val coordinates = Array(entries.size) { i ->
            MotionEvent.PointerCoords().apply {
                x = entries[i].value.x
                y = entries[i].value.y
                pressure = 1f
                size = 1f
            }
        }
        val event = MotionEvent.obtain(
            touchDownTime.takeIf { it > 0L } ?: now, now, action, entries.size,
            properties, coordinates, 0, 0, 1f, 1f, 0, 0,
            InputDevice.SOURCE_TOUCHSCREEN, 0,
        )
        try { inject(svc, event, id) } finally { event.recycle() }
    }

    private fun inject(svc: IBerryService, ev: android.view.InputEvent, id: Int) {
        val queuedEvent: android.view.InputEvent = when (ev) {
            is MotionEvent -> MotionEvent.obtain(ev)
            is KeyEvent -> KeyEvent(ev)
            else -> return
        }
        runCatching {
            inputWorker.execute {
                try {
                    svc.injectEvent(Bundle().apply { putParcelable("event", queuedEvent) }, id)
                } catch (_: Throwable) {
                } finally {
                    (queuedEvent as? MotionEvent)?.recycle()
                }
            }
        }.onFailure { (queuedEvent as? MotionEvent)?.recycle() }
    }

    /** Returns true when the back press was used by the berry. */
    fun handleBack(): Boolean {
        if (pickerOpen && currentApp != null) {
            pickerOpen = false
            return true
        }
        if (!pickerOpen && displayId >= 0) {
            sendBackKey()
            return true
        }
        return false
    }

    companion object {
        const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
        private const val REQUEST_CODE = 7
        private const val DEFAULT_DPI_SCALE = 0.75f
        private const val PAD_SENSITIVITY = 1.6f
    }
}
