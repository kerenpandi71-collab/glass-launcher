package com.kerenpandi.glasslauncher.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kerenpandi.glasslauncher.data.AppInfo
import com.kerenpandi.glasslauncher.data.stableKey
import com.kerenpandi.glasslauncher.ui.common.AppIcon

@Composable
fun HiddenAppsDialog(
    apps: List<AppInfo>,
    hiddenKeys: Set<String>,
    onToggle: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sembunyikan aplikasi") },
        text = {
            Column {
                Text("Aplikasi tercentang disembunyikan dari home dan pencarian.")
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 380.dp).padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(apps, key = { it.stableKey() }) { app ->
                        val key = app.stableKey()
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AppIcon(
                                app = app,
                                iconSize = 34.dp,
                                showLabel = false,
                                onClick = { onToggle(key) }
                            )
                            Text(app.label, modifier = Modifier.weight(1f))
                            Checkbox(
                                checked = key in hiddenKeys,
                                onCheckedChange = { onToggle(key) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Selesai") }
        }
    )
}
