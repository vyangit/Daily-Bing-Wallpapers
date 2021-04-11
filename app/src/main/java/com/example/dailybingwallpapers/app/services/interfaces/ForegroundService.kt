package com.example.dailybingwallpapers.app.services.interfaces

import android.app.Notification
import android.app.NotificationManager

interface ForegroundService {
    fun marshallNotificationChannel()
    fun buildNotification(): Notification
    fun promoteToForeground()

    companion object {
        const val NOTIFICATION_ID_DEFAULT = 0   // Notification id for general app alerts
        const val NOTIFICATION_ID_IMPORTER = 1  // Notification id for alerts from importer services

        const val NOTIFICATION_CHANNEL_ID_UPDATES = "UPDATES" // Channel id for application update alerts

        const val UPDATES_CHANNEL_IMPORTANCE = NotificationManager.IMPORTANCE_LOW
    }
}
