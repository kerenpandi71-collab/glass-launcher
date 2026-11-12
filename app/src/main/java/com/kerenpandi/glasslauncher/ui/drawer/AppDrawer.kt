package com.kerenpandi.glasslauncher.ui.drawer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kerenpandi.glasslauncher.data.AppInfo
import com.kerenpandi.glasslauncher.data.stableKey
import com.kerenpandi.glasslauncher.ui.common.AppIcon
import com.kerenpandi.glasslauncher.ui.common.LocalLauncherSettings
import com.kerenpandi.glasslauncher.ui.glass.GlassPanel

@Composable
fun AppDrawer(
    apps: List<AppInfo>,
    onLaunch: (AppInfo) -> Unit,
    onDismiss: () -> Unit
) {
    val settings = LocalLauncherSettings.current
    var query by remember { mutableStateOf("") }
    val filtered = remember(apps, query) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) apps
        else apps.filter { it.label.contains(trimmed, ignoreCase = true) }
    }

    Box(
        Modifier.fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            )
    ) {
        GlassPanel(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .imePadding()
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    placeholder = { Text("Cari aplikasi…") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                if (filtered.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            if (query.isBlank()) "Tidak ada aplikasi"
                            else "Tidak ditemukan untuk \"$query\"",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(settings?.gridColumns ?: 4),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filtered, key = { it.stableKey() }) { app ->
                            AppIcon(app = app, onClick = { onLaunch(app) })
                        }
                    }
                }
                Spacer(Modifier.navigationBarsPadding())
            }
        }
    }
}
