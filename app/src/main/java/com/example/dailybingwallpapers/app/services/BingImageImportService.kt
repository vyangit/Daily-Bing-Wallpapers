package com.example.dailybingwallpapers.app.services

import android.app.*
import android.app.WallpaperManager.FLAG_SYSTEM
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.core.content.edit
import com.example.dailybingwallpapers.R
import com.example.dailybingwallpapers.app.services.interfaces.ForegroundService
import com.example.dailybingwallpapers.app.storage.database.AppDatabase
import com.example.dailybingwallpapers.app.storage.database.repos.BingImageRepository
import com.example.dailybingwallpapers.network.BingWallpaperNetwork
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.Integer.max
import java.lang.Integer.min

const val UPDATES_CHANNEL_IMPORTANCE = NotificationManager.IMPORTANCE_LOW

class BingImageImportService : Service(), ForegroundService {
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
        throw UnsupportedOperationException("BingImageImportService doesn't support binding")
    }

    override fun marshallNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
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
            .setContentTitle(getText(R.string.import_service_notification_title))
            .setContentIntent(pendingIntent)
            .build()
    }

    override fun promoteToForeground() {
        // Build the foreground notification
        val foregroundNotice = buildNotification()

        // Declare and start service as foreground
        startForeground(ForegroundService.NOTIFICATION_ID_IMPORTER, foregroundNotice)
    }

    private fun checkAndFetchMissingBingImages() {
        val scope = CoroutineScope(Dispatchers.IO)

        // Fetch the data and close the service after
        scope.launch {
            repo.createMissingBingImages()
        }.invokeOnCompletion {
            scope.launch {
                withContext(Dispatchers.IO) { refreshDailyWallpaper() }
            }
            stopSelf()
        }
    }

    private suspend fun refreshDailyWallpaper() {
        val sharedPrefs = getSharedPreferences(
            getString(R.string.shared_prefs_app_globals_file_key),
            Context.MODE_PRIVATE
        )
        val isDailyModeOn = sharedPrefs.getBoolean(
            getString(R.string.shared_prefs_app_globals_daily_mode_on),
            false
        )

        if (isDailyModeOn) {
            val wpManager = WallpaperManager.getInstance(applicationContext)
            val recordedWallpaperId = sharedPrefs.getInt(
                getString(R.string.shared_prefs_app_globals_recorded_wallpaper_id),
                -1
            )
            val currWallpaperId = wpManager.getWallpaperId(WallpaperManager.FLAG_SYSTEM)
            if (currWallpaperId == recordedWallpaperId) {
                database.bingImageDao.mostRecentBingImage?.let { image ->
                    if (wpManager.isSetWallpaperAllowed) {
                        val uri = Uri.parse(image.imageDeviceUri)
                        contentResolver.openInputStream(uri)!!.use { inputStream ->

                            val bingWallpaper = BitmapFactory.decodeStream(inputStream)
                            val minSize = min(bingWallpaper.height, bingWallpaper.width)
                            val leftDelta = (bingWallpaper.width / 2) - (minSize / 2)
                            val topDelta = (bingWallpaper.height / 2) - (minSize / 2)
                            val rect = Rect(
                                max(0, leftDelta),
                                max(0, topDelta),
                                min(leftDelta + minSize, minSize),
                                min(topDelta + minSize, minSize)
                            )
                            wpManager.setStream(inputStream, rect, true, FLAG_SYSTEM)
                        }
                    }
                }
            } else {
                sharedPrefs.edit {
                    putBoolean(getString(R.string.shared_prefs_app_globals_daily_mode_on), false)
                    putInt(
                        getString(R.string.shared_prefs_app_globals_recorded_wallpaper_id),
                        currWallpaperId
                    )
                }
            }
        }
    }
}