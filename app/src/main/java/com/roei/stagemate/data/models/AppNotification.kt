package com.roei.stagemate.data.models

import com.roei.stagemate.utilities.DateFormatter
import java.util.concurrent.TimeUnit

// In-app notification model (payment confirmations, reminders, event updates).
// Used by NotificationsFragment, NotificationAdapter, and MyFirebaseMessagingService.
data class AppNotification(
    var id: String = "",
    var title: String = "",
    var message: String = "",
    var type: NotificationType = NotificationType.INFO,
    var timestamp: Long = System.currentTimeMillis(),
    var isRead: Boolean = false,
    var eventId: String = "",
    var imageUrl: String = "",
    var ticketId: String = ""
) {
    
    enum class NotificationType {
        SUCCESS,    // Payment Successful
        CANCELLED,  // Order Cancelled
        INFO,       // New Features Available
        REMINDER,   // Event Reminder
        EVENT_UPDATE // Event Update (used by Firebase)
    }
    
    fun markAsRead() = apply { isRead = true }

    // Relative time string ("5 minutes ago", "Just now", absolute date for older items).
    fun getFormattedTime(): String = DateFormatter.formatTimestamp(timestamp)

    // Notifications older than 30 days are considered expired and filtered from the inbox.
    fun isExpired(): Boolean =
        System.currentTimeMillis() - timestamp > TimeUnit.DAYS.toMillis(30)
    
    class Builder(
        var title: String = "",
        var message: String = "",
        var type: NotificationType = NotificationType.INFO,
        var timestamp: Long = System.currentTimeMillis(),
        var eventId: String = "",
        var imageUrl: String = "",
        var ticketId: String = ""
    ) {
        fun title(title: String) = apply { this.title = title }
        fun message(message: String) = apply { this.message = message }
        fun type(type: NotificationType) = apply { this.type = type }
        fun timestamp(timestamp: Long) = apply { this.timestamp = timestamp }
        fun eventId(eventId: String) = apply { this.eventId = eventId }
        fun imageUrl(imageUrl: String) = apply { this.imageUrl = imageUrl }
        fun ticketId(ticketId: String) = apply { this.ticketId = ticketId }

        fun build() = AppNotification(
            id = "",
            title = title,
            message = message,
            type = type,
            timestamp = timestamp,
            isRead = false,
            eventId = eventId,
            imageUrl = imageUrl,
            ticketId = ticketId
        )
    }
}
