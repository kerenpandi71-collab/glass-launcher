package com.kerenpandi.glasslauncher.ui.home

import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.util.concurrent.TimeUnit

private data class RecentEntry(val packageName: String, val label: String)

@Composable
fun RecentAppsDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var entries by remember { mutableStateOf(loadRecentApps(context)) }
    val hasAccess = remember { hasUsageAccess(context) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
            shape = RoundedCornerShape(28.dp),
            color = Color(0xE91B2638)
        ) {
            Column(Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Recent", color = Color.White, fontSize = 21.sp, modifier = Modifier.weight(1f))
                    TextButton(onClick = { entries = emptyList() }) { Text("Bersihkan daftar") }
                    TextButton(onClick = onDismiss) { Text("Tutup") }
                }
                if (!hasAccess) {
                    Text("Izinkan akses penggunaan agar Glass Launcher dapat menampilkan aplikasi terbaru.",
                        color = Color.White.copy(alpha = .8f), fontSize = 13.sp)
                    Button(onClick = {
                        context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    }) { Text("Buka izin Usage Access") }
                } else if (entries.isEmpty()) {
                    Text("Belum ada aplikasi terbaru yang terdeteksi.", color = Color.White.copy(alpha = .7f))
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(vertical = 14.dp)
                    ) {
                        items(entries, key = { it.packageName }) { entry ->
                            Column(
                                Modifier.width(142.dp).height(190.dp)
                                    .background(Color.White.copy(alpha = .10f), RoundedCornerShape(20.dp))
                                    .clickable {
                                        context.packageManager.getLaunchIntentForPackage(entry.packageName)?.let {
                                            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            context.startActivity(it)
                                            onDismiss()
                                        }
                                    }.padding(12.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(entry.label, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text("Ketuk untuk buka", color = Color.White.copy(alpha = .6f), fontSize = 11.sp)
                            }
                        }
                    }
                    Text("Kartu menampilkan daftar aplikasi terbaru, bukan snapshot task asli Android.",
                        color = Color.White.copy(alpha = .55f), fontSize = 11.sp)
                }
            }
        }
    }
}

private fun hasUsageAccess(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
    val mode = appOps.checkOpNoThrow(
        "android:get_usage_stats",
        android.os.Process.myUid(),
        context.packageName
    )
    return mode == android.app.AppOpsManager.MODE_ALLOWED
}

private fun loadRecentApps(context: Context): List<RecentEntry> {
    if (!hasUsageAccess(context)) return emptyList()
    val manager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    val end = System.currentTimeMillis()
    val stats: List<UsageStats> = manager.queryUsageStats(
        UsageStatsManager.INTERVAL_DAILY,
        end - TimeUnit.DAYS.toMillis(2),
        end
    ) ?: emptyList()
    return stats.asSequence()
        .filter { it.packageName != context.packageName && it.lastTimeUsed > 0L }
        .groupBy { it.packageName }
        .mapNotNull { (pkg, rows) ->
            val launch = context.packageManager.getLaunchIntentForPackage(pkg) ?: return@mapNotNull null
            val label = runCatching {
                context.packageManager.getApplicationLabel(context.packageManager.getApplicationInfo(pkg, 0)).toString()
            }.getOrNull() ?: return@mapNotNull null
            RecentEntry(pkg, label)
        }
        .sortedByDescending { entry -> stats.filter { it.packageName == entry.packageName }.maxOfOrNull { it.lastTimeUsed } ?: 0L }
        .take(12)
        .toList()
}
