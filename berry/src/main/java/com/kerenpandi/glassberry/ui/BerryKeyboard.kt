package com.kerenpandi.glassberry.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kerenpandi.glassberry.BerryController

private const val SHIFT_OFF = 0
private const val SHIFT_ONCE = 1
private const val SHIFT_LOCK = 2

@Composable
fun BerryKeyboard(controller: BerryController) {
    var shift by remember { mutableIntStateOf(SHIFT_OFF) }
    var symbols by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    fun tap(action: () -> Unit) {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        action()
    }

    fun typeChar(c: Char) {
        val out = when (shift) {
            SHIFT_LOCK -> c.uppercaseChar()
            SHIFT_ONCE -> c.uppercaseChar().also { shift = SHIFT_OFF }
            else -> c
        }
        controller.sendChar(out)
    }

    Column(
        Modifier
            .fillMaxWidth()
            .background(Berry.Bezel)
            .padding(horizontal = 6.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (symbols) {
            KeyRow(listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")) { typeChar(it[0]) }
            KeyRow(listOf("@", "#", "_", "&", "-", "+", "(", ")")) { typeChar(it[0]) }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Key("ABC", Modifier.weight(1.5f), special = true) { symbols = false }
                KeyRow(listOf("=", "\\", "*", "\"", "'", "!", ";", "?"), Modifier.weight(8f)) { typeChar(it[0]) }
                Key("⌫", Modifier.weight(1.5f), special = true) { tap { controller.sendBackspace() } }
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Key("/", Modifier.weight(1f)) { typeChar('/') }
                Key(",", Modifier.weight(1f)) { typeChar(',') }
                Key("SPASI", Modifier.weight(6f)) { tap { typeChar(' ') } }
                Key(".", Modifier.weight(1f)) { typeChar('.') }
                Key("⏎", Modifier.weight(1.5f), accent = true) { tap { controller.sendEnter() } }
            }
        } else {
            KeyRow(listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p")) { typeChar(it[0]) }
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                listOf("a", "s", "d", "f", "g", "h", "j", "k", "l").forEach { letter ->
                    Key(letter, Modifier.weight(1f)) { typeChar(letter[0]) }
                }
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Key(
                    when (shift) {
                        SHIFT_LOCK -> "⇪"
                        SHIFT_ONCE -> "⇧"
                        else -> "⇧"
                    },
                    Modifier.weight(1.5f),
                    special = true,
                    active = shift != SHIFT_OFF,
                ) {
                    shift = when (shift) {
                        SHIFT_OFF -> SHIFT_ONCE
                        SHIFT_ONCE -> SHIFT_LOCK
                        else -> SHIFT_OFF
                    }
                }
                Row(Modifier.weight(7f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("z", "x", "c", "v", "b", "n", "m").forEach { letter ->
                        Key(letter, Modifier.weight(1f)) { typeChar(letter[0]) }
                    }
                }
                Key("⌫", Modifier.weight(1.5f), special = true) { tap { controller.sendBackspace() } }
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Key("123", Modifier.weight(1.5f), special = true) { symbols = true }
                Key(",", Modifier.weight(1f)) { typeChar(',') }
                Key("SPASI", Modifier.weight(5f)) { tap { typeChar(' ') } }
                Key(".", Modifier.weight(1f)) { typeChar('.') }
                Key("⏎", Modifier.weight(1.5f), accent = true) { tap { controller.sendEnter() } }
            }
        }
    }
}

@Composable
private fun KeyRow(
    letters: List<String>,
    modifier: Modifier = Modifier,
    onPress: (String) -> Unit,
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        letters.forEach { letter ->
            Key(letter, Modifier.weight(1f)) { onPress(letter) }
        }
    }
}

@Composable
private fun Key(
    label: String,
    modifier: Modifier = Modifier,
    special: Boolean = false,
    active: Boolean = false,
    onPress: () -> Unit,
) {
    val bg = when {
        active -> Berry.Accent
        special -> Berry.KeySpecial
        else -> Berry.Key
    }
    Box(
        modifier
            .height(46.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .pointerInput(label) {
                detectTapGestures(onPress = {
                    tryAwaitRelease()
                    onPress()
                })
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
    }
}
