package com.kerenpandi.glassberry

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.BatteryManager
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.view.View
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

class BerryScreenView(context: Context) : View(context) {

    var os: BerryOS? = null

    private val bg = Paint().apply { color = Color.BLACK }
    private val barPaint = Paint().apply { color = 0xFF161618.toInt() }
    private val accent = 0xFFDF2029.toInt()
    private val accentPaint = Paint().apply { color = accent }
    private val selPaint = Paint().apply { color = 0xFF2A2A2E.toInt() }
    private val white = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
    }
    private val mono = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        typeface = Typeface.MONOSPACE
    }
    private val dim = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF9A9AA0.toInt() }

    private val clockFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault())

    private val density = resources.displayMetrics.density
    private fun dp(v: Float) = v * density

    private val tick = object : Runnable {
        override fun run() {
            invalidate()
            postDelayed(this, 1000L)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        post(tick)
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(tick)
        super.onDetachedFromWindow()
    }

    private fun batteryLevel(): Int {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val level = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
        return if (level in 1..100) level else 100
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        canvas.drawRect(0f, 0f, w, h, bg)

        val barH = dp(28f)
        canvas.drawRect(0f, 0f, w, barH, barPaint)
        white.textSize = dp(13f)
        canvas.drawText(clockFormat.format(Date()), dp(12f), barH - dp(9f), white)
        val batt = "${batteryLevel()}%"
        canvas.drawText(batt, w - dp(12f) - white.measureText(batt), barH - dp(9f), white)

        val os = os ?: return
        runCatching {
            when (os.screen) {
                BerryOS.Screen.HOME -> drawHome(canvas, os, w, h, barH)
                BerryOS.Screen.NOTES -> drawNotes(canvas, os, w, h, barH)
                BerryOS.Screen.TERMINAL -> drawTerminal(canvas, os, w, h, barH)
                BerryOS.Screen.ABOUT -> drawAbout(canvas, os, w, h, barH)
            }
        }
    }

    private fun drawHome(canvas: Canvas, os: BerryOS, w: Float, h: Float, barH: Float) {
        white.textSize = dp(64f)
        white.typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
        val clock = clockFormat.format(Date())
        val clockW = white.measureText(clock)
        canvas.drawText(clock, (w - clockW) / 2, barH + dp(90f), white)

        white.textSize = dp(14f)
        white.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        dim.textSize = dp(13f)
        val date = dateFormat.format(Date())
        val dateW = white.measureText(date)
        canvas.drawText(date, (w - dateW) / 2, barH + dp(118f), white)

        val listTop = h - dp(210f)
        if (os.filter.isNotEmpty()) {
            canvas.drawText("\"$os.filter\"", dp(12f), listTop - dp(12f), dim)
        }

        val list = os.filtered
        if (list.isEmpty()) {
            val hint = if (os.filter.isEmpty()) "Ketik untuk cari app" else "Tidak ditemukan"
            val hw = dim.measureText(hint)
            canvas.drawText(hint, (w - hw) / 2, listTop + dp(30f), dim)
        } else {
            var y = listTop
            list.forEachIndexed { i, app ->
                val selected = i == os.selectedIndex.coerceIn(0, list.lastIndex)
                if (selected) canvas.drawRect(dp(8f), y, w - dp(8f), y + dp(40f), selPaint)
                if (selected) canvas.drawRect(dp(8f), y, dp(12f), y + dp(40f), accentPaint)
                white.textSize = dp(16f)
                canvas.drawText(app.name, dp(24f), y + dp(26f), white)
                y += dp(42f)
            }
        }

        dim.textSize = dp(11f)
        val foot = "⏎ buka · ⎋ kembali · ⌫ hapus"
        val fw = dim.measureText(foot)
        canvas.drawText(foot, (w - fw) / 2, h - dp(8f), dim)
    }

    private fun drawNotes(canvas: Canvas, os: BerryOS, w: Float, h: Float, barH: Float) {
        drawAppHeader(canvas, "Notes", w, barH)
        white.textSize = dp(15f)
        val text = os.notes.ifEmpty { "Ketik sesuatu…\n(⎋ = kembali ke home)" }
        val paint = TextPaint(white)
        val layout = StaticLayout.Builder
            .obtain(text, 0, text.length, paint, (w - dp(24f)).toInt())
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(dp(4f), 1f)
            .build()
        canvas.save()
        canvas.translate(dp(12f), barH + dp(14f))
        layout.draw(canvas)
        canvas.restore()
    }

    private fun drawTerminal(canvas: Canvas, os: BerryOS, w: Float, h: Float, barH: Float) {
        drawAppHeader(canvas, "Terminal", w, barH)
        mono.textSize = dp(12f)
        val lineH = mono.fontSpacing
        val prompt = "berry@~ $ ${os.terminalInput}▌"
        val allLines = os.terminalLines + prompt
        val maxLines = max(1, ((h - barH - dp(30f)) / lineH).toInt())
        val visible = allLines.takeLast(maxLines)
        var y = barH + dp(18f) + lineH - dp(3f)
        visible.forEach { line ->
            if (line.length > 60) {
                val layout = StaticLayout.Builder
                    .obtain(line, 0, line.length, TextPaint(mono), (w - dp(20f)).toInt())
                    .build()
                canvas.save()
                canvas.translate(dp(10f), y - lineH + dp(2f))
                layout.draw(canvas)
                canvas.restore()
                y += layout.height
            } else {
                canvas.drawText(line, dp(10f), y, mono)
                y += lineH
            }
        }
    }

    private fun drawAbout(canvas: Canvas, os: BerryOS, w: Float, h: Float, barH: Float) {
        drawAppHeader(canvas, "About", w, barH)
        white.textSize = dp(22f)
        canvas.drawText("GlassBerry", dp(12f), barH + dp(46f), white)
        dim.textSize = dp(13f)
        val lines = listOf(
            "Virtual phone portrait",
            "dengan keyboard QWERTY",
            "ala BlackBerry.",
            "",
            "Layar virtual berjalan di",
            "VirtualDisplay milik app ini.",
            "Semua input lewat keyboard.",
            "",
            "Glass Launcher · v1.0"
        )
        var y = barH + dp(78f)
        lines.forEach {
            canvas.drawText(it, dp(12f), y, dim)
            y += dp(20f)
        }
    }

    private fun drawAppHeader(canvas: Canvas, title: String, w: Float, barH: Float) {
        val headH = dp(36f)
        canvas.drawRect(0f, barH, w, barH + headH, barPaint)
        canvas.drawRect(0f, barH + headH - dp(2f), w, barH + headH, accentPaint)
        white.textSize = dp(17f)
        canvas.drawText(title, dp(12f), barH + headH - dp(11f), white)
    }
}
