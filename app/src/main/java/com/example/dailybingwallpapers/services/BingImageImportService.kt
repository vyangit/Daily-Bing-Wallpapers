package com.example.dailybingwallpapers.services

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.IBinder
import com.example.dailybingwallpapers.R
import com.example.dailybingwallpapers.network.BingWallpaperNetwork
import com.example.dailybingwallpapers.storage.database.AppDatabase
import com.example.dailybingwallpapers.storage.database.repos.BingImageRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

const val UPDATES_CHANNEL_ID = "UPDATES"
const val UPDATES_CHANNEL_IMPORTANCE = NotificationManager.IMPORTANCE_LOW
const val NOTIFICATION_ID_FOREGROUND_SERVICE = 1

class BingImageImportService: Service() {
    private lateinit var database: AppDatabase
    private lateinit var network: BingWallpaperNetwork
    private lateinit var repo: BingImageRepository

    override fun onCreate() {
        super.onCreate()

        database = AppDatabase.getDatabase(this)
        network = BingWallpaperNetwork.getService()
        repo = BingImageRepository(this, network, database.bingImageDao)

        // Android 8 >= foreground promotion needed
        marshallNotificationChannel()
        promoteToForeground()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        checkAndFetchMissingBingImages()
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent?): IBinder? {
        throw UnsupportedOperationException("Not necessary")
    }

    private fun checkAndFetchMissingBingImages() {
        val scope = CoroutineScope(Dispatchers.IO)

        // Fetch the data and close the service after
        scope.launch {
            repo.importMissingBingImages()
        }.invokeOnCompletion {
            stopSelf()
        }
    }

    private fun marshallNotificationChannel() {
        val nManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nChannel =
                NotificationChannel(UPDATES_CHANNEL_ID, "Updates", UPDATES_CHANNEL_IMPORTANCE)
            nChannel.description = "Facilitates app data update notifications"
            nManager.createNotificationChannel(nChannel)
        }
    }

    private fun buildNotification(): Notification {
        val pendingIntent =
            Intent(this, BingImageImportService::class.java).let { importIntent ->
                PendingIntent.getService(this, 0, importIntent, 0)
            }

        return Notification.Builder(
            this,
            UPDATES_CHANNEL_ID)
            .setContentTitle(getText(R.string.import_service_notification_title))
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun promoteToForeground() {
        // Build the foreground notification
        val foregroundNotice = buildNotification()

        // Declare and start service as foreground
        startForeground(NOTIFICATION_ID_FOREGROUND_SERVICE, foregroundNotice)
    }
}