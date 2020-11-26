package com.example.dailybingwallpapers.app.services

import android.app.*
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import com.example.dailybingwallpapers.R
import com.example.dailybingwallpapers.app.services.interfaces.ForegroundService
import com.example.dailybingwallpapers.app.storage.database.AppDatabase
import com.example.dailybingwallpapers.app.storage.database.entities.BingImageCompositeKeyWithUri
import com.example.dailybingwallpapers.app.storage.database.repos.BingImageRepository
import com.example.dailybingwallpapers.network.BingWallpaperNetwork
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BingImageFileCheckerService : Service(), ForegroundService {
    private lateinit var database: AppDatabase
    private lateinit var network: BingWallpaperNetwork
    private lateinit var repo: BingImageRepository

    inner class BingImageFileCheckerServiceBinder : Binder() {
        fun getService() = this@BingImageFileCheckerService
    }

    private val binder = BingImageFileCheckerServiceBinder()

    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Android 8 >= foreground promotion needed
        marshallNotificationChannel()
        promoteToForeground()

        // Main service API call
        validateBingImageDeviceUris()

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onCreate() {
        super.onCreate()

        database = AppDatabase.getDatabase(this)
        network = BingWallpaperNetwork.getService()
        repo = BingImageRepository(this, network, database.bingImageDao)
    }

    override fun marshallNotificationChannel() {
        val nManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nChannel =
                NotificationChannel(
                    ForegroundService.NOTIFICATION_CHANNEL_ID_UPDATES,
                    "Updates",
                    ForegroundService.UPDATES_CHANNEL_IMPORTANCE
                )
            nChannel.description = "Facilitates app data update notifications"
            nManager.createNotificationChannel(nChannel)
        }
    }

    override fun buildNotification(): Notification {
        val pendingIntent =
            Intent(this, BingImageFileCheckerService::class.java).let { importIntent ->
                PendingIntent.getService(this, 0, importIntent, 0)
            }

        return Notification.Builder(
            this,
            ForegroundService.NOTIFICATION_CHANNEL_ID_UPDATES
        )
            .setContentTitle(getText(R.string.validator_service_notification_title))
            .setContentIntent(pendingIntent)
            .build()
    }

    override fun promoteToForeground() {
        // Build the foreground notification
        val foregroundNotice = buildNotification()

        // Declare and start service as foreground
        startForeground(ForegroundService.NOTIFICATION_ID_VALIDATOR, foregroundNotice)
    }

    fun validateBingImageDeviceUris() {
        val imageCompositeKeys = repo.getAllCompositeKeys()
        for (key in imageCompositeKeys) {
            CoroutineScope(Dispatchers.IO).launch {
                validateBingImageDeviceUri(key)
            }
        }
    }

    /**
     * @param keyWithUri An bing image composite key with corresponding device file uri
     *
     * @return True, if image url and device uri are both valid, having a matching http url image
     * and device file. False, if device file is missing and can't be retrieved from http url.
     */
    private suspend fun validateBingImageDeviceUri(keyWithUri: BingImageCompositeKeyWithUri): Boolean {
        repo.deleteByCompositeKeyIfInvalid(keyWithUri)
        return true
    }
}