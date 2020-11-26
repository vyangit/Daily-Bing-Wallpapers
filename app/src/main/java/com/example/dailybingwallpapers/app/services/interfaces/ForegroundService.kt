package com.example.dailybingwallpapers.app.services.interfaces

import android.app.Notification
import android.app.NotificationManager

interface ForegroundService {
    fun marshallNotificationChannel()
    fun buildNotification(): Notification
    fun promoteToForeground()

    companion object {
        const val NOTIFICATION_ID_DEFAULT = 0
        const val NOTIFICATION_ID_IMPORTER = 1
        const val NOTIFICATION_ID_VALIDATOR = 2

        const val NOTIFICATION_CHANNEL_ID_UPDATES = "UPDATES"

        const val UPDATES_CHANNEL_IMPORTANCE = NotificationManager.IMPORTANCE_LOW
    }
}
