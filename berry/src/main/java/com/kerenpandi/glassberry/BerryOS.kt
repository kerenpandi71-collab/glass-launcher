package com.kerenpandi.glassberry

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build

class BerryOS(context: Context) {

    enum class Screen { HOME, NOTES, TERMINAL, APPS, ABOUT }

    data class App(val name: String, val target: Screen)
    data class RealApp(val label: String, val component: String)

    private val prefs: SharedPreferences =
        context.getSharedPreferences("glassberry_notes", Context.MODE_PRIVATE)
    private val appContext: Context = context.applicationContext

    var screen = Screen.HOME
    var filter = ""
    var selectedIndex = 0
    var notes: String = prefs.getString("notes", "") ?: ""
    val terminalLines = mutableListOf<String>()
    var terminalInput = ""
    var status = ""
    var displayId = 0
    var invalidate: (() -> Unit)? = null

    private var realApps: List<RealApp> = emptyList()

    val apps = listOf(
        App("Notes", Screen.NOTES),
        App("Terminal", Screen.TERMINAL),
        App("Apps", Screen.APPS),
        App("About", Screen.ABOUT)
    )

    val filtered: List<App>
        get() = if (filter.isEmpty()) apps else apps.filter { it.name.startsWith(filter, ignoreCase = true) }

    val filteredReal: List<RealApp>
        get() = if (filter.isEmpty()) realApps else realApps.filter { it.label.startsWith(filter, ignoreCase = true) }

    init {
        showCrashLog()
    }

    private fun saveNotes() {
        prefs.edit().putString("notes", notes).apply()
    }

    private fun ensureRealApps(context: Context) {
        if (realApps.isNotEmpty()) return
        realApps = runCatching {
            val pm = context.packageManager
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            pm.queryIntentActivities(intent, 0)
                .asSequence()
                .mapNotNull { it.activityInfo }
                .filter { it.packageName != context.packageName }
                .distinctBy { it.packageName }
                .map { info ->
                    RealApp(
                        label = info.loadLabel(pm).toString(),
                        component = "${info.packageName}/${info.name}"
                    )
                }
                .sortedBy { it.label.lowercase() }
                .toList()
        }.getOrDefault(emptyList())
    }

    fun onChar(c: Char) {
        when (screen) {
            Screen.HOME -> {
                filter += c
                selectedIndex = 0
            }
            Screen.NOTES -> notes += c
            Screen.TERMINAL -> terminalInput += c
            Screen.APPS -> {
                filter += c
                selectedIndex = 0
            }
            Screen.ABOUT -> Unit
        }
    }

    fun onBackspace() {
        when (screen) {
            Screen.HOME -> if (filter.isNotEmpty()) filter = filter.dropLast(1)
            Screen.NOTES -> if (notes.isNotEmpty()) notes = notes.dropLast(1)
            Screen.TERMINAL -> if (terminalInput.isNotEmpty()) terminalInput = terminalInput.dropLast(1)
            Screen.APPS -> if (filter.isNotEmpty()) filter = filter.dropLast(1)
            Screen.ABOUT -> Unit
        }
    }

    fun onEnter() {
        when (screen) {
            Screen.HOME -> {
                val list = filtered
                if (list.isNotEmpty()) {
                    val app = list[selectedIndex.coerceIn(0, list.lastIndex)]
                    open(app.target)
                } else {
                    filter = ""
                }
            }
            Screen.NOTES -> notes += "\n"
            Screen.TERMINAL -> runCommand()
            Screen.APPS -> launchSelected()
            Screen.ABOUT -> Unit
        }
    }

    fun onEscape() {
        when (screen) {
            Screen.HOME -> filter = ""
            else -> open(Screen.HOME)
        }
    }

    fun open(target: Screen) {
        screen = target
        filter = ""
        selectedIndex = 0
        if (target == Screen.TERMINAL && terminalLines.isEmpty()) {
            terminalLines += "GlassBerry OS 1.1"
            terminalLines += "Ketik 'help' untuk daftar perintah."
            terminalLines += ""
        }
        if (target == Screen.APPS) ensureRealApps(appContext)
    }

    private fun launchSelected() {
        val list = filteredReal
        if (list.isEmpty()) {
            status = "App tidak ditemukan"
            invalidate?.invoke()
            return
        }
        val app = list[selectedIndex.coerceIn(0, list.lastIndex)]
        launchReal(app)
    }

    fun launchReal(app: RealApp) {
        if (!BerryShizuku.isBinderAvailable()) {
            status = "Shizuku tidak aktif — install & jalankan Shizuku dulu"
            invalidate?.invoke()
            return
        }
        if (!BerryShizuku.hasPermission()) {
            status = "Meminta izin Shizuku…"
            invalidate?.invoke()
            BerryShizuku.requestPermission { granted ->
                if (granted) {
                    applyBerrySettings()
                    launchReal(app)
                } else {
                    status = "Izin Shizuku ditolak"
                    invalidate?.invoke()
                }
            }
            return
        }
        status = "Membuka ${app.label}…"
        invalidate?.invoke()
        val displayArg = if (displayId > 0 && Build.VERSION.SDK_INT >= 29) {
            arrayOf("--display", displayId.toString())
        } else emptyArray()
        BerryShizuku.exec(
            arrayOf("am", "start") + displayArg + arrayOf("-n", app.component)
        ) { code, output ->
            status = if (code == 0) {
                "${app.label} dibuka di layar virtual — sentuh layar untuk kontrol"
            } else {
                "Gagal buka ${app.label}: ${output.lineSequence().lastOrNull { it.isNotBlank() } ?: "error $code"}"
            }
            invalidate?.invoke()
        }
    }

    fun applyBerrySettings() {
        BerryShizuku.exec(arrayOf("settings", "put", "global", "force_resizable_activities", "1")) { _, _ -> }
    }

    fun restoreBerrySettings() {
        BerryShizuku.exec(arrayOf("settings", "put", "global", "force_resizable_activities", "0")) { _, _ -> }
    }

    fun showCrashLog() {
        val trace = CrashLog.last ?: return
        CrashLog.last = null
        screen = Screen.TERMINAL
        terminalLines += "--- CRASH TERDETEKSI ---"
        trace.lines().take(60).forEach { terminalLines += it }
        terminalLines += "--- AKHIR LOG ---"
        terminalLines += "Screenshot layar ini dan kirim"
        terminalLines += "ke developer. Ketik 'clear' untuk hapus."
        terminalLines += ""
    }

    fun shizukuStatusText(): String = when {
        !BerryShizuku.isBinderAvailable() -> "Shizuku: TIDAK AKTIF"
        !BerryShizuku.hasPermission() -> "Shizuku: belum ada izin"
        else -> "Shizuku: AKTIF"
    }

    private fun runCommand() {
        val cmd = terminalInput.trim()
        terminalInput = ""
        terminalLines += "berry@~ $ $cmd"
        val parts = cmd.split(" ")
        when (parts.firstOrNull()?.lowercase() ?: "") {
            "" -> Unit
            "help" -> {
                terminalLines += "Perintah:"
                terminalLines += "  help      daftar perintah"
                terminalLines += "  date      tanggal & jam"
                terminalLines += "  echo X    tampilkan X"
                terminalLines += "  battery   level baterai"
                terminalLines += "  apps      daftar aplikasi"
                terminalLines += "  open X    buka app (notes/terminal/apps/about)"
                terminalLines += "  shizuku   status izin Shizuku"
                terminalLines += "  berry-on  aktifkan mode resizable (Shizuku)"
                terminalLines += "  berry-off matikan mode resizable (Shizuku)"
                terminalLines += "  clear     bersihkan layar"
                terminalLines += "  about     info"
            }
            "date" -> terminalLines += java.text.SimpleDateFormat(
                "EEE, d MMM yyyy HH:mm:ss", java.util.Locale.getDefault()
            ).format(java.util.Date())
            "echo" -> terminalLines += parts.drop(1).joinToString(" ")
            "battery" -> terminalLines += "Level baterai: lihat indikator atas"
            "apps" -> apps.forEach { terminalLines += "- ${it.name}" }
            "open" -> when (parts.getOrNull(1)?.lowercase()) {
                "notes" -> open(Screen.NOTES)
                "terminal" -> open(Screen.TERMINAL)
                "apps" -> open(Screen.APPS)
                "about" -> open(Screen.ABOUT)
                else -> terminalLines += "open: app tidak dikenal"
            }
            "shizuku" -> terminalLines += shizukuStatusText()
            "berry-on" -> {
                if (!BerryShizuku.hasPermission()) {
                    terminalLines += "Shizuku belum punya izin — buka Apps dulu"
                } else {
                    applyBerrySettings()
                    terminalLines += "force_resizable_activities = 1"
                }
            }
            "berry-off" -> {
                if (!BerryShizuku.hasPermission()) {
                    terminalLines += "Shizuku belum punya izin"
                } else {
                    restoreBerrySettings()
                    terminalLines += "force_resizable_activities = 0"
                }
            }
            "clear" -> terminalLines.clear()
            "about" -> {
                terminalLines += "GlassBerry OS 1.1"
                terminalLines += "Virtual phone dengan keyboard QWERTY"
                terminalLines += "ala BlackBerry + Shizuku virtual display"
                terminalLines += "(terinspirasi Dextop by NarYuki)"
            }
            else -> terminalLines += "perintah tidak dikenal: ${parts[0]}"
        }
        terminalLines += ""
    }
}
