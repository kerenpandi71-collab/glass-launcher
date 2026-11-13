package com.kerenpandi.glassberry

import android.content.Context
import android.content.SharedPreferences

class BerryOS(context: Context) {

    enum class Screen { HOME, NOTES, TERMINAL, ABOUT }

    data class App(val name: String, val target: Screen)

    private val prefs: SharedPreferences =
        context.getSharedPreferences("glassberry_notes", Context.MODE_PRIVATE)

    var screen = Screen.HOME
    var filter = ""
    var selectedIndex = 0
    var notes: String = prefs.getString("notes", "") ?: ""
    val terminalLines = mutableListOf<String>()
    var terminalInput = ""

    val apps = listOf(
        App("Notes", Screen.NOTES),
        App("Terminal", Screen.TERMINAL),
        App("About", Screen.ABOUT)
    )

    val filtered: List<App>
        get() = if (filter.isEmpty()) apps else apps.filter { it.name.startsWith(filter, ignoreCase = true) }

    private fun saveNotes() {
        prefs.edit().putString("notes", notes).apply()
    }

    fun onChar(c: Char) {
        when (screen) {
            Screen.HOME -> {
                filter += c
                selectedIndex = 0
            }
            Screen.NOTES -> notes += c
            Screen.TERMINAL -> terminalInput += c
            Screen.ABOUT -> Unit
        }
    }

    fun onBackspace() {
        when (screen) {
            Screen.HOME -> if (filter.isNotEmpty()) filter = filter.dropLast(1)
            Screen.NOTES -> if (notes.isNotEmpty()) notes = notes.dropLast(1)
            Screen.TERMINAL -> if (terminalInput.isNotEmpty()) terminalInput = terminalInput.dropLast(1)
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
            terminalLines += "GlassBerry OS 1.0"
            terminalLines += "Ketik 'help' untuk daftar perintah."
            terminalLines += ""
        }
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
                terminalLines += "  help     daftar perintah"
                terminalLines += "  date     tanggal & jam"
                terminalLines += "  echo X   tampilkan X"
                terminalLines += "  battery  level baterai"
                terminalLines += "  apps     daftar aplikasi"
                terminalLines += "  open X   buka app (notes/terminal/about)"
                terminalLines += "  clear    bersihkan layar"
                terminalLines += "  about    info"
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
                "about" -> open(Screen.ABOUT)
                else -> terminalLines += "open: app tidak dikenal"
            }
            "clear" -> terminalLines.clear()
            "about" -> {
                terminalLines += "GlassBerry OS 1.0"
                terminalLines += "Virtual phone dengan keyboard QWERTY"
                terminalLines += "ala BlackBerry. Dibuat dengan ide @kerenpandi71-collab"
            }
            else -> terminalLines += "perintah tidak dikenal: ${parts[0]}"
        }
        terminalLines += ""
    }
}
