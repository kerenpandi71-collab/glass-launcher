package com.kerenpandi.glasslauncher.ui.widgets

import android.appwidget.AppWidgetProviderInfo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun WidgetPickerDialog(
    providers: List<AppWidgetProviderInfo>,
    onSelect: (AppWidgetProviderInfo) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pilih widget") },
        text = {
            if (providers.isEmpty()) {
                Text("Tidak ada widget terpasang di perangkat ini.")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(providers, key = { it.provider.flattenToString() }) { info ->
                        TextButton(
                            onClick = { onSelect(info) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                verticalArrangement = Arrangement.spacedBy(1.dp)
                            ) {
                                Text(
                                    widgetLabel(info),
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                                Text(
                                    info.provider.packageName,
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

fun widgetLabel(info: AppWidgetProviderInfo): String = when {
    info.label.isNotBlank() -> info.label
    else -> info.provider.className.substringAfterLast('.')
}
