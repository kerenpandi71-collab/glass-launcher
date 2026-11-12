package com.kerenpandi.glasslauncher.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kerenpandi.glasslauncher.data.AppInfo
import com.kerenpandi.glasslauncher.data.stableKey
import com.kerenpandi.glasslauncher.ui.common.AppIcon

@Composable
fun SearchOverlay(
    apps: List<AppInfo>,
    onLaunch: (AppInfo) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val results = remember(apps, query) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) emptyList()
        else apps.filter { it.label.contains(trimmed, ignoreCase = true) }
    }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Box(
        Modifier.fillMaxSize()
            .background(Color(0xE60D1420))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            )
    ) {
        Column(
            Modifier.fillMaxSize()
                .statusBarsPadding()
                .imePadding()
                .padding(20.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { }
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                placeholder = { Text("Cari aplikasi…") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
            )
            Spacer(Modifier.height(14.dp))
            when {
                query.isBlank() -> Text(
                    "Ketik untuk mencari aplikasi",
                    color = Color.White.copy(alpha = 0.6f)
                )
                results.isEmpty() -> Text(
                    "Tidak ditemukan untuk \"${query.trim()}\"",
                    color = Color.White.copy(alpha = 0.6f)
                )
                else -> LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(results, key = { it.stableKey() }) { app ->
                        Row(
                            Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { onLaunch(app) }
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AppIcon(
                                app = app,
                                iconSize = 38.dp,
                                showLabel = false,
                                onClick = {
                                    onLaunch(app)
                                }
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                app.label,
                                color = Color.White,
                                fontSize = 15.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                app.packageName,
                                color = Color.White.copy(alpha = 0.4f),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
