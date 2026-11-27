package com.group8.wellbeing.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.group8.wellbeing.R
import com.group8.wellbeing.data.local.WellbeingDatabase
import com.group8.wellbeing.domain.ReminderUserAction
import com.group8.wellbeing.ui.MainActivity
import com.group8.wellbeing.usage.NotificationPermissionChecker
import com.group8.wellbeing.usage.NotificationPermissionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

internal class ReminderNotifications(context: Context) {
    private val appContext = context.applicationContext
    private val manager = requireNotNull(appContext.getSystemService(NotificationManager::class.java))

    fun canPost(): Boolean =
        NotificationPermissionChecker.check(appContext) != NotificationPermissionState.DENIED &&
            manager.areNotificationsEnabled() &&
            manager.getNotificationChannel(CHANNEL_ID)?.importance != NotificationManager.IMPORTANCE_NONE

    fun post(eventId: Long, message: String, timeoutMs: Long, stillAllowed: () -> Boolean): Boolean =
        synchronized(notificationLock) {
            if (!stillAllowed() || !canPost()) return@synchronized false
            val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            val open = PendingIntent.getActivity(
                appContext, eventId.toInt(),
                Intent(appContext, MainActivity::class.java)
                    .setAction(ACTION_OPEN)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    .putExtra(EXTRA_EVENT_ID, eventId),
                flags
            )
            val dismiss = PendingIntent.getBroadcast(
                appContext, eventId.toInt(),
                Intent(appContext, ReminderDismissReceiver::class.java).putExtra(EXTRA_EVENT_ID, eventId),
                flags
            )
            try {
                manager.createNotificationChannel(
                    NotificationChannel(CHANNEL_ID, "Kullanım hatırlatmaları", NotificationManager.IMPORTANCE_DEFAULT)
                )
                val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle(appContext.getString(R.string.app_name))
                    .setContentText(message)
                    .setContentIntent(open)
                    .setDeleteIntent(dismiss)
                    .setAutoCancel(true)
                    .setTimeoutAfter(timeoutMs)
                    .build()
                manager.notify(NOTIFICATION_ID, notification)
                true
            } catch (error: SecurityException) {
                Log.w("ReminderNotifications", "Hatırlatma izni gönderim sırasında değişti.", error)
                false
            }
        }

    fun cancel() = synchronized(notificationLock) {
        manager.cancel(NOTIFICATION_ID)
    }

    companion object {
        const val CHANNEL_ID = "wellbeing_usage_reminders"
        const val ACTION_OPEN = "com.group8.wellbeing.OPEN_REMINDER"
        const val EXTRA_EVENT_ID = "reminder_event_id"
        private const val NOTIFICATION_ID = 1
        private val notificationLock = Any()
    }
}

class ReminderDismissReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val eventId = intent.getLongExtra(ReminderNotifications.EXTRA_EVENT_ID, 0L)
        if (eventId <= 0) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                WellbeingDatabase.getInstance(context).wellbeingDao()
                    .recordReminderAction(eventId, ReminderUserAction.DISMISSED.name)
            } finally {
                pending.finish()
            }
        }
    }
}
