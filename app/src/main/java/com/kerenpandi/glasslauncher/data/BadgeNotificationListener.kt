package com.kerenpandi.glasslauncher.data

import android.content.ComponentName
import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class BadgeNotificationListener : NotificationListenerService() {

    companion object {
        private val _counts = MutableStateFlow<Map<String, Int>>(emptyMap())
        val counts: StateFlow<Map<String, Int>> = _counts
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        val map = mutableMapOf<String, Int>()
        activeNotifications?.forEach { sbn ->
            if (isCountable(sbn)) map.merge(sbn.packageName, 1, Int::plus)
        }
        _counts.value = map
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (!isCountable(sbn)) return
        val map = _counts.value.toMutableMap()
        map.merge(sbn.packageName, 1, Int::plus)
        _counts.value = map
    }

    override fun onListenerDisconnected() {
        requestRebind(ComponentName(this, BadgeNotificationListener::class.java))
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        val map = _counts.value.toMutableMap()
        val current = map[sbn.packageName] ?: return
        if (current <= 1) map.remove(sbn.packageName) else map[sbn.packageName] = current - 1
        _counts.value = map
    }

    private fun isCountable(sbn: StatusBarNotification): Boolean =
        !sbn.isOngoing && (sbn.notification.flags and android.app.Notification.FLAG_GROUP_SUMMARY) == 0
}
