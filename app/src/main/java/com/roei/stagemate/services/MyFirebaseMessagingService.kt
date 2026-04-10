package com.roei.stagemate.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.roei.stagemate.R
import com.roei.stagemate.ui.activities.MainActivity
import com.roei.stagemate.utilities.Constants
import com.roei.stagemate.data.repository.DataRepository

// FCM service that handles incoming push notifications and token refresh.
// Tapping a notification launches MainActivity.
class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val CHANNEL_ID = "stagemate_notifications"
        private const val CHANNEL_NAME = "StageMate Notifications"
        private const val CHANNEL_DESCRIPTION = "Notifications for events, tickets, and promotions"
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        createNotificationChannel()

        val title = remoteMessage.notification?.title ?: "StageMate"
        val message = remoteMessage.notification?.body ?: ""
        val eventId = remoteMessage.data["eventId"]
        val type = remoteMessage.data["type"] ?: "EVENT_UPDATE"

        showNotification(title, message, eventId, type)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)

        DataRepository.saveUserFCMToken(token) { _, _ -> }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableLights(true)
                enableVibration(true)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showNotification(title: String, message: String, eventId: String?, type: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(Constants.IntentKeys.EVENT_ID, eventId)
            putExtra(Constants.IntentKeys.NOTIFICATION_TYPE, type)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_bell)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), notification)
    }
}