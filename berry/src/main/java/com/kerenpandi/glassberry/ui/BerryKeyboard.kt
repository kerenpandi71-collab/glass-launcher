package com.kerenpandi.glassberry.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kerenpandi.glassberry.BerryController
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val SHIFT_OFF = 0
private const val SHIFT_ONCE = 1
private const val SHIFT_LOCK = 2

@Composable
fun BerryKeyboard(controller: BerryController) {
    var shift by remember { mutableIntStateOf(SHIFT_OFF) }
    var symbols by remember { mutableStateOf(false) }

    fun typeChar(c: Char) {
        val out = when (shift) {
            SHIFT_LOCK -> c.uppercaseChar()
            SHIFT_ONCE -> c.uppercaseChar().also { shift = SHIFT_OFF }
            else -> c
        }
        controller.sendChar(out)
    }

    fun typeAlt(alt: String) {
        controller.sendChar(alt[0])
    }

    Column(
        Modifier
            .fillMaxWidth()
            .background(Berry.Bezel)
            .padding(horizontal = 6.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        if (symbols) {
            KeyRow(listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"), alts = null) { typeAlt(it) }
            KeyRow(listOf("@", "#", "_", "&", "-", "+", "(", ")"), alts = null) { typeAlt(it) }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Key("ABC", Modifier.weight(1.5f), special = true) { symbols = false }
                Row(Modifier.weight(8f), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    listOf("=", "\\", "*", "\"", "'", "!", ";", "?").forEach { symbol ->
                        Key(symbol, Modifier.weight(1f)) { typeAlt(symbol) }
                    }
                }
                Key("⌫", Modifier.weight(1.5f), special = true, repeatable = true) { controller.sendBackspace() }
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Key("/", Modifier.weight(1f)) { typeAlt("/") }
                Key(",", Modifier.weight(1f)) { typeAlt(",") }
                Key("SPASI", Modifier.weight(6f), special = true) { typeChar(' ') }
                Key(".", Modifier.weight(1f)) { typeAlt(".") }
                Key("⏎", Modifier.weight(1.5f), accent = true) { controller.sendEnter() }
            }
        } else {
            KeyRow(
                listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"),
                alts = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"),
                onAlt = { typeAlt(it) },
            ) { typeChar(it[0]) }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 15.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                listOf("a", "s", "d", "f", "g", "h", "j", "k", "l").forEach { letter ->
                    Key(letter, Modifier.weight(1f)) { typeChar(letter[0]) }
                }
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Key("⇧", Modifier.weight(1.5f), special = true, active = shift != SHIFT_OFF) {
                    shift = when (shift) {
                        SHIFT_OFF -> SHIFT_ONCE
                        SHIFT_ONCE -> SHIFT_LOCK
                        else -> SHIFT_OFF
                    }
                }
                Row(Modifier.weight(7f), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    listOf("z", "x", "c", "v", "b", "n", "m").forEach { letter ->
                        Key(letter, Modifier.weight(1f)) { typeChar(letter[0]) }
                    }
                }
                Key("⌫", Modifier.weight(1.5f), special = true, repeatable = true) { controller.sendBackspace() }
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Key("123", Modifier.weight(1.5f), special = true) { symbols = true }
                Key(",", Modifier.weight(1f)) { typeChar(',') }
                Key("SPASI", Modifier.weight(5f), special = true) { typeChar(' ') }
                Key(".", Modifier.weight(1f)) { typeChar('.') }
                Key("⏎", Modifier.weight(1.5f), accent = true) { controller.sendEnter() }
            }
        }
    }
}

@Composable
private fun KeyRow(
    keys: List<String>,
    modifier: Modifier = Modifier,
    alts: List<String>? = null,
    onAlt: ((String) -> Unit)? = null,
    onPress: (String) -> Unit,
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        keys.forEachIndexed { index, label ->
            val alt = alts?.getOrNull(index)
            Key(label, Modifier.weight(1f), alt = alt, onAlt = if (alt != null) {
                { onAlt?.invoke(alt) }
            } else null) {
                onPress(label)
            }
        }
    }
}

@Composable
private fun Key(
    label: String,
    modifier: Modifier = Modifier,
    alt: String? = null,
    special: Boolean = false,
    active: Boolean = false,
    accent: Boolean = false,
    repeatable: Boolean = false,
    onAlt: (() -> Unit)? = null,
    onPress: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var pressed by remember { mutableStateOf(false) }
    var pressJob by remember { mutableStateOf<Job?>(null) }
    val scale by animateFloatAsState(if (pressed) 0.93f else 1f, label = "keyScale")
    val base = when {
        accent || active -> Berry.Accent
        special -> Berry.KeySpecial
        else -> Berry.Key
    }
    val bg = if (pressed) lerp(base, Berry.Ink, 0.22f) else base

    Box(
        modifier
            .height(46.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(9.dp))
            .background(bg)
            .pointerInput(label, repeatable) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        if (repeatable) {
                            onPress()
                            pressJob = scope.launch {
                                delay(350)
                                while (isActive) {
                                    onPress()
                                    delay(60)
                                }
                            }
                        }
                        tryAwaitRelease()
                        pressJob?.cancel()
                        pressJob = null
                        pressed = false
                    },
                    onTap = { if (!repeatable) onPress() },
                    onLongPress = {
                        if (onAlt != null) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onAlt()
                        }
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = Berry.Ink,
            fontSize = 14.sp,
            fontWeight = if (label.length > 1) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
        )
        alt?.let {
            Text(
                it,
                color = Berry.InkDim,
                fontSize = 9.sp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 3.dp, end = 5.dp),
            )
        }
    }
}
