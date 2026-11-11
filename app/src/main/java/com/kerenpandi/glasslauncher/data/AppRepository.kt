package com.kerenpandi.glasslauncher.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.graphics.drawable.Drawable
import android.os.Process
import android.os.UserHandle
import android.os.UserManager

data class AppInfo(
    val label: String,
    val packageName: String,
    val activityName: String,
    val icon: Drawable,
    val userHandle: UserHandle = Process.myUserHandle()
) {
    val componentName: ComponentName
        get() = ComponentName(packageName, activityName)
}

fun AppInfo.stableKey(): String =
    "${userHandle.hashCode()}/${componentName.flattenToString()}"

class AppRepository(private val context: Context) {

    private val launcherApps =
        context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    private val userManager =
        context.getSystemService(Context.USER_SERVICE) as UserManager

    fun loadApps(): List<AppInfo> {
        val apps = mutableListOf<AppInfo>()
        for (profile in userManager.userProfiles) {
            try {
                launcherApps.getActivityList(null, profile).forEach { activity ->
                    val packageName = activity.componentName.packageName
                    if (packageName == context.packageName) return@forEach
                    apps += AppInfo(
                        label = activity.label.toString(),
                        packageName = packageName,
                        activityName = activity.componentName.className,
                        icon = activity.getBadgedIcon(0),
                        userHandle = profile
                    )
                }
            } catch (e: Exception) {
            }
        }
        return apps.sortedBy { it.label.lowercase() }
    }

    fun launch(activityContext: Context, app: AppInfo) {
        try {
            launcherApps.startMainActivity(app.componentName, app.userHandle, null, null)
        } catch (e: Exception) {
            activityContext.startActivity(launchIntent(app))
        }
    }

    fun launchIntent(app: AppInfo): Intent =
        Intent().apply {
            setClassName(app.packageName, app.activityName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

}
