package com.kerenpandi.glassberry

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View

class BerryKeyboardView(context: Context) : View(context) {

    interface Listener {
        fun onChar(c: Char)
        fun onBackspace()
        fun onEnter()
        fun onEscape()
    }

    var listener: Listener? = null

    private enum class Kind { CHAR, SHIFT, BACKSPACE, ENTER, ESCAPE, SYM }

    private data class Key(
        val label: String,
        val kind: Kind,
        val weight: Float = 1f,
        val ch: Char? = null
    )

    private val letterRows: List<List<Key>> = listOf(
        "qwertyuiop".map { Key(it.toString(), Kind.CHAR, ch = it) },
        "asdfghjkl".map { Key(it.toString(), Kind.CHAR, ch = it) },
        listOf(Key("⇧", Kind.SHIFT, 1.5f)) +
            "zxcvbnm".map { Key(it.toString(), Kind.CHAR, ch = it) } +
            listOf(Key("⌫", Kind.BACKSPACE, 1.5f)),
        listOf(
            Key("⎋", Kind.ESCAPE, 1.3f),
            Key("123", Kind.SYM, 1.3f),
            Key(",", Kind.CHAR, ch = ','),
            Key("", Kind.CHAR, 4.2f, ch = ' '),
            Key(".", Kind.CHAR, ch = '.'),
            Key("⏎", Kind.ENTER, 1.6f)
        )
    )

    private val symbolRows: List<List<Key>> = listOf(
        "1234567890".map { Key(it.toString(), Kind.CHAR, ch = it[0]) },
        "@#_&-+()/".map { Key(it.toString(), Kind.CHAR, ch = it[0]) },
        listOf(Key("=\\<", Kind.SYM, 1.5f)) +
            "*\"':;!?".map { Key(it.toString(), Kind.CHAR, ch = it[0]) } +
            listOf(Key("⌫", Kind.BACKSPACE, 1.5f)),
        listOf(
            Key("⎋", Kind.ESCAPE, 1.3f),
            Key("abc", Kind.SYM, 1.3f),
            Key(",", Kind.CHAR, ch = ','),
            Key("", Kind.CHAR, 4.2f, ch = ' '),
            Key(".", Kind.CHAR, ch = '.'),
            Key("⏎", Kind.ENTER, 1.6f)
        )
    )

    private var sym = false
    private var shift = 0

    private val keyPaint = Paint().apply { color = 0xFF2A2A2E.toInt() }
    private val specialPaint = Paint().apply { color = 0xFF3A3A40.toInt() }
    private val pressedPaint = Paint().apply { color = 0xFF55555C.toInt() }
    private val enterPaint = Paint().apply { color = 0xFFDF2029.toInt() }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
    }
    private val bgPaint = Paint().apply { color = 0xFF101012.toInt() }

    private val density = resources.displayMetrics.density
    private fun dp(v: Float) = v * density

    private var keyRects: List<Pair<RectF, Key>> = emptyList()
    private var pressedIndex = -1

    private fun activeRows(): List<List<Key>> = if (sym) symbolRows else letterRows

    private fun labelFor(key: Key): String = when {
        key.kind != Kind.CHAR -> key.label
        shift >= 2 -> key.ch?.uppercaseChar()?.toString() ?: key.label
        shift == 1 -> key.ch?.uppercaseChar()?.toString() ?: key.label
        sym -> key.label
        else -> key.label
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldw2: Int) {
        computeLayout(w.toFloat(), h.toFloat())
    }

    private fun computeLayout(w: Float, h: Float) {
        val rects = mutableListOf<Pair<RectF, Key>>()
        val rows = activeRows()
        val pad = dp(3f)
        val rowH = (h - pad * (rows.size + 1)) / rows.size
        rows.forEachIndexed { r, row ->
            val totalWeight = row.sumOf { it.weight.toDouble() }.toFloat()
            val usable = w - pad * (row.size + 1) - dp(4f)
            var x = pad + dp(2f)
            val y = pad + r * (rowH + pad)
            row.forEach { key ->
                val kw = usable * (key.weight / totalWeight)
                rects += RectF(x, y, x + kw, y + rowH) to key
                x += kw + pad
            }
        }
        keyRects = rects
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)
        keyRects.forEachIndexed { i, (rect, key) ->
            val paint = when {
                i == pressedIndex -> pressedPaint
                key.kind == Kind.ENTER -> enterPaint
                key.kind != Kind.CHAR -> specialPaint
                else -> keyPaint
            }
            canvas.drawRoundRect(rect, dp(6f), dp(6f), paint)
            val label = labelFor(key)
            if (label.isNotEmpty()) {
                labelPaint.textSize = if (key.kind == Kind.CHAR) dp(17f) else dp(13f)
                if (key.kind == Kind.CHAR && shift >= 1 && !sym) {
                    labelPaint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                } else {
                    labelPaint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
                }
                val tw = labelPaint.measureText(label)
                val cy = rect.centerY() - (labelPaint.ascent() + labelPaint.descent()) / 2
                canvas.drawText(label, rect.centerX() - tw / 2, cy, labelPaint)
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val idx = keyAt(event.x, event.y)
                if (idx != pressedIndex) {
                    pressedIndex = idx
                    performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    invalidate()
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                val idx = keyAt(event.x, event.y)
                pressedIndex = -1
                invalidate()
                if (idx >= 0) fire(keyRects[idx].second)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val idx = keyAt(event.x, event.y)
                if (idx != pressedIndex) {
                    pressedIndex = idx
                    invalidate()
                }
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                pressedIndex = -1
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun keyAt(x: Float, y: Float): Int {
        keyRects.forEachIndexed { i, (rect, _) ->
            if (rect.contains(x, y)) return i
        }
        return -1
    }

    private fun fire(key: Key) {
        val l = listener ?: return
        when (key.kind) {
            Kind.CHAR -> {
                var c = key.ch ?: return
                if (!sym && shift >= 1) c = c.uppercaseChar()
                l.onChar(c)
                if (shift == 1) {
                    shift = 0
                    computeLayout(width.toFloat(), height.toFloat())
                    invalidate()
                }
            }
            Kind.BACKSPACE -> l.onBackspace()
            Kind.ENTER -> l.onEnter()
            Kind.ESCAPE -> l.onEscape()
            Kind.SYM -> {
                sym = !sym
                computeLayout(width.toFloat(), height.toFloat())
                invalidate()
            }
            Kind.SHIFT -> {
                shift = when (shift) {
                    0 -> 1
                    1 -> 2
                    else -> 0
                }
                invalidate()
            }
        }
    }
}
