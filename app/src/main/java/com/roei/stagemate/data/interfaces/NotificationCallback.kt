package com.roei.stagemate.data.interfaces

import com.roei.stagemate.data.models.AppNotification

// Click handler for notification list items, used by NotificationAdapter.
interface NotificationCallback {
    fun onNotificationClicked(notification: AppNotification, position: Int)
    fun onDeleteClicked(notification: AppNotification, position: Int)
}
