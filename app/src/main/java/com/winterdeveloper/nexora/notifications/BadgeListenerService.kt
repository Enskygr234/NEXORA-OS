package com.winterdeveloper.nexora.notifications

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** In-process store of notification counts per package. Nothing is persisted or logged. */
object BadgeStore {
    private val _counts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val counts: StateFlow<Map<String, Int>> = _counts.asStateFlow()

    internal fun publish(counts: Map<String, Int>) {
        _counts.value = counts
    }

    fun isAccessGranted(context: Context): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

    fun serviceComponent(context: Context) = ComponentName(context, BadgeListenerService::class.java)
}

/**
 * Counts active notifications per app for icon badges. Only runs if the user grants
 * notification access in system settings. It reads package names only; notification
 * content is never read or stored.
 */
class BadgeListenerService : NotificationListenerService() {

    override fun onListenerConnected() = recount()
    override fun onNotificationPosted(sbn: StatusBarNotification?) = recount()
    override fun onNotificationRemoved(sbn: StatusBarNotification?) = recount()

    override fun onListenerDisconnected() {
        BadgeStore.publish(emptyMap())
    }

    private fun recount() {
        val active: Array<StatusBarNotification> = try {
            activeNotifications ?: emptyArray()
        } catch (e: SecurityException) {
            emptyArray()
        } catch (e: RuntimeException) {
            emptyArray()
        }
        val counts = HashMap<String, Int>()
        for (sbn in active) {
            val n = sbn.notification ?: continue
            if (!sbn.isClearable) continue
            if (n.flags and Notification.FLAG_GROUP_SUMMARY != 0) continue
            counts[sbn.packageName] = (counts[sbn.packageName] ?: 0) + 1
        }
        BadgeStore.publish(counts)
    }
}
