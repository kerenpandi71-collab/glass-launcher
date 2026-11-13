package com.kerenpandi.glasslauncher

import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent

class LockAdminReceiver : DeviceAdminReceiver()

object SystemActions {

    fun isAdminActive(context: Context): Boolean {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
            ?: return false
        return dpm.isAdminActive(ComponentName(context, LockAdminReceiver::class.java))
    }

    fun requestAdmin(context: Context) {
        runCatching {
            context.startActivity(
                Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                    putExtra(
                        DevicePolicyManager.EXTRA_DEVICE_ADMIN,
                        ComponentName(context, LockAdminReceiver::class.java)
                    )
                    putExtra(
                        DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                        "Aktifkan admin perangkat agar double-tap pada layar dapat mengunci perangkat."
                    )
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        }
    }

    fun lockScreen(context: Context): Boolean {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
            ?: return false
        val admin = ComponentName(context, LockAdminReceiver::class.java)
        return if (dpm.isAdminActive(admin)) {
            runCatching { dpm.lockNow() }.isSuccess
        } else false
    }

    fun expandNotificationsPanel(context: Context) {
        runCatching {
            val service = context.getSystemService("statusbar")
                ?: error("Statusbar service unavailable")
            val method = service.javaClass.getMethod("expandNotificationsPanel")
            method.invoke(service)
        }.onFailure {
            runCatching {
                context.startActivity(
                    Intent("android.settings.NOTIFICATION_SETTINGS")
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        }
    }
}
