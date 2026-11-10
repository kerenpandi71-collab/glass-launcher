package com.kerenpandi.glasslauncher.ui.common

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
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
import com.kerenpandi.glasslauncher.data.AppInfo
import com.kerenpandi.glasslauncher.ui.glass.glassSurface
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppIcon(
    app: AppInfo,
    iconSize: Dp = 52.dp,
    showLabel: Boolean = true,
    onClick: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val bitmap = remember(app) {
        app.icon.toBitmap(96, 96).asImageBitmap()
    }
    Box {
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
    try {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(Uri.fromParts("package", app.packageName, null))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    } catch (e: Exception) {
    }
}

private fun uninstallApp(context: Context, app: AppInfo) {
    try {
        context.startActivity(
            Intent(Intent.ACTION_DELETE, Uri.fromParts("package", app.packageName, null))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    } catch (e: Exception) {
    }
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
