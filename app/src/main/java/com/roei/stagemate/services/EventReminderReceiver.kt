package com.roei.stagemate.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.roei.stagemate.R
import com.roei.stagemate.data.models.AppNotification
import com.roei.stagemate.ui.activities.MainActivity
import com.roei.stagemate.utilities.Constants
import com.roei.stagemate.data.repository.DataRepository

// BroadcastReceiver for event reminder notifications (course-standard pattern).
// Triggered by AlarmManager, shows a system notification and saves to Firestore.
class EventReminderReceiver : BroadcastReceiver() {

    companion object {
        const val KEY_EVENT_ID = "event_id"
        const val KEY_EVENT_TITLE = "event_title"
        const val KEY_EVENT_LOCATION = "event_location"
        const val KEY_REMINDER_TYPE = "reminder_type"
        const val REMINDER_24H = "24h"
        const val REMINDER_3H = "3h"
        private const val CHANNEL_ID = "event_reminders"
        private const val CHANNEL_NAME = "Event Reminders"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        val eventId = intent.getStringExtra(KEY_EVENT_ID) ?: return
        val eventTitle = intent.getStringExtra(KEY_EVENT_TITLE) ?: return
        val eventLocation = intent.getStringExtra(KEY_EVENT_LOCATION) ?: ""
        val reminderType = intent.getStringExtra(KEY_REMINDER_TYPE) ?: REMINDER_24H

        val (titleRes, messageRes) = when (reminderType) {
            REMINDER_3H -> R.string.event_reminder_3h_title to R.string.event_reminder_3h_message
            else -> R.string.event_reminder_title to R.string.event_reminder_message
        }

        saveReminderToFirestore(context, eventId, eventTitle, eventLocation, titleRes, messageRes)
        showSystemNotification(context, eventId, eventTitle, eventLocation, titleRes, messageRes)
    }

    private fun saveReminderToFirestore(
        context: Context, eventId: String, eventTitle: String,
        eventLocation: String, titleRes: Int, messageRes: Int
    ) {
        val notification = AppNotification.Builder()
            .title(context.getString(titleRes))
            .message(context.getString(messageRes, eventTitle, eventLocation))
            .type(AppNotification.NotificationType.REMINDER)
            .eventId(eventId)
            .build()

        DataRepository.saveNotification(notification) { _, _ -> }
    }

    private fun showSystemNotification(
        context: Context, eventId: String, eventTitle: String,
        eventLocation: String, titleRes: Int, messageRes: Int
    ) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH)
            channel.description = "Reminders for upcoming events"
            notificationManager.createNotificationChannel(channel)
        }

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            putExtra(Constants.IntentKeys.EVENT_ID, eventId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, eventId.hashCode(), launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = context.getString(titleRes)
        val message = context.getString(messageRes, eventTitle, eventLocation)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_bell)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notifId = "${eventId}_${titleRes}".hashCode()
        notificationManager.notify(notifId, notification)
    }
}
