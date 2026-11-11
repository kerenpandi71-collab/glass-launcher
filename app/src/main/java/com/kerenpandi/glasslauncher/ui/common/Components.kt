package com.kerenpandi.glasslauncher.ui.common

import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.app.ActivityManager
import android.os.Process
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import android.util.LruCache
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.kerenpandi.glasslauncher.ShizukuActions
import com.kerenpandi.glasslauncher.data.AppInfo
import com.kerenpandi.glasslauncher.data.stableKey
import com.kerenpandi.glasslauncher.ui.glass.glassSurface
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

data class AppMenuItem(val label: String, val action: () -> Unit)

private val appIconCache = object : LruCache<String, ImageBitmap>(2 * 1024 * 1024) {
    override fun sizeOf(key: String, value: ImageBitmap): Int =
        value.width * value.height * 4
}

fun cachedAppIcon(app: AppInfo, sizePx: Int): ImageBitmap {
    val key = "${app.stableKey()}:$sizePx"
    return appIconCache.get(key) ?: app.icon.toBitmap(sizePx, sizePx)
        .asImageBitmap()
        .also { appIconCache.put(key, it) }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppIcon(
    app: AppInfo,
    iconSize: Dp = 52.dp,
    showLabel: Boolean = true,
    onClick: () -> Unit,
    extraMenuItems: List<AppMenuItem> = emptyList(),
    modifier: Modifier = Modifier
) {
    var menuOpen by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val bitmap = remember(app.stableKey()) { cachedAppIcon(app, 96) }
    Box(modifier = modifier) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .clip(CircleShape)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { menuOpen = true }
                )
                .padding(8.dp)
        ) {
            Image(
                bitmap = bitmap,
                contentDescription = app.label,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(iconSize)
            )
            if (showLabel) {
                Text(
                    text = app.label,
                    color = Color.White,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .fillMaxWidth()
                )
            }
        }
        DropdownMenu(
            expanded = menuOpen,
            onDismissRequest = { menuOpen = false },
            containerColor = Color(0xFF141C2E)
        ) {
            extraMenuItems.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item.label, color = Color.White) },
                    onClick = {
                        menuOpen = false
                        item.action()
                    }
                )
            }
            DropdownMenuItem(
                text = { Text("Hentikan aplikasi", color = Color.White) },
                onClick = {
                    menuOpen = false
                    ShizukuActions.stopApp(context, app.packageName)
                }
            )
            DropdownMenuItem(
                text = { Text("Info aplikasi", color = Color.White) },
                onClick = {
                    menuOpen = false
                    openAppInfo(context, app)
                }
            )
            DropdownMenuItem(
                text = { Text("Uninstall", color = Color.White) },
                onClick = {
                    menuOpen = false
                    uninstallApp(context, app)
                }
            )
        }
    }
}

private fun openAppInfo(context: Context, app: AppInfo) {
    if (openProfileAppDetails(context, app)) return

    try {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(Uri.fromParts("package", app.packageName, null))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    } catch (e: Exception) {
        Toast.makeText(context, "Tidak bisa membuka info aplikasi", Toast.LENGTH_SHORT).show()
    }
}

/**
 * Opens Settings for the exact Android profile represented by this launcher icon.
 * This matters for Dual Apps, work profiles, and other secondary user profiles.
 */
private fun openProfileAppDetails(context: Context, app: AppInfo): Boolean {
    return try {
        val launcherApps = context.getSystemService(LauncherApps::class.java)
            ?: return false
        launcherApps.startAppDetailsActivity(
            app.componentName,
            app.userHandle,
            null,
            null
        )
        true
    } catch (e: Exception) {
        false
    }
}

private fun uninstallApp(context: Context, app: AppInfo) {
    // A cloned/work-profile app must be managed from that profile's own app details.
    if (app.userHandle != Process.myUserHandle()) {
        if (openProfileAppDetails(context, app)) {
            Toast.makeText(
                context,
                "Pilih Copot pemasangan di Info aplikasi profil tersebut",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        Toast.makeText(context, "Tidak bisa membuka info aplikasi profil ini", Toast.LENGTH_SHORT).show()
        return
    }

    val packageUri = Uri.fromParts("package", app.packageName, null)
    val attempts = listOf(
        Intent(Intent.ACTION_UNINSTALL_PACKAGE, packageUri)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        Intent(Intent.ACTION_DELETE, packageUri)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )

    for (intent in attempts) {
        try {
            context.startActivity(intent)
            return
        } catch (e: Exception) {
            // Continue to the next system-supported uninstall entry point.
        }
    }

    // Last-resort fallback: expose the system app details page instead of no-op.
    if (openProfileAppDetails(context, app)) return

    Toast.makeText(context, "Tidak bisa membuka menu uninstall aplikasi", Toast.LENGTH_SHORT).show()
}

@Composable
fun ClockHeader(
    modifier: Modifier = Modifier,
    timeFontSize: TextUnit = 54.sp,
    dateFontSize: TextUnit = 14.sp
) {
    var now by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(10_000)
            now = LocalTime.now()
        }
    }
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = now.format(DateTimeFormatter.ofPattern("HH:mm")),
            color = Color.White,
            fontSize = timeFontSize
        )
        Text(
            text = LocalDate.now()
                .format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault())),
            color = Color.White.copy(alpha = 0.85f),
            fontSize = dateFontSize,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .glassSurface(shape = CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(22.dp)
        )
    }
}
