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
import com.example.dailybingwallpapers.network.BingImageApiNetwork
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.Integer.min
import java.time.LocalDate

const val UPDATES_CHANNEL_IMPORTANCE = NotificationManager.IMPORTANCE_LOW

class BingImageImportService : Service(), ForegroundService {
    private lateinit var database: AppDatabase
    private lateinit var network: BingImageApiNetwork
    private lateinit var repo: BingImageRepository

    override fun onCreate() {
        super.onCreate()

        database = AppDatabase.getDatabase(this)
        network = BingImageApiNetwork.getService()
        repo = BingImageRepository(this, network, database.bingImageDao)

        // Android 8 >= foreground promotion needed
        marshallNotificationChannel()
        promoteToForeground()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Run service and schedule stop
        runService()
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
            Intent(this, BingImageImportService::class.java).let { importIntent ->
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

    private fun runService() {
        // Fetch the data and close the service after
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.IO) { importLatestBingImages() }
            withContext(Dispatchers.IO) { importMissingBingImages() }
        }.invokeOnCompletion {
            refreshDailyWallpaper()
            stopSelf()
        }
    }

    private suspend fun importLatestBingImages() {
        repo.createLatestBingImages()
    }

    private suspend fun importMissingBingImages() {
        val keys = repo.getAllCompositeKeys()
        for (key in keys) { // deletes or revalidates if image file is lost
            repo.deleteByCompositeKeyIfInvalid(key)
        }
    }

    private fun refreshDailyWallpaper() {
        val sharedPrefs = getSharedPreferences(
            getString(R.string.shared_prefs_app_globals_file_key),
            Context.MODE_PRIVATE
        )
        val isDailyModeOn = sharedPrefs.getBoolean(
            getString(R.string.shared_prefs_app_globals_daily_mode_on),
            false
        )
        val recordedWallpaperId = sharedPrefs.getInt(
            getString(R.string.shared_prefs_app_globals_recorded_wallpaper_id),
            -1
        )
        val wpManager = WallpaperManager.getInstance(applicationContext)
        val currWallpaperId = wpManager.getWallpaperId(WallpaperManager.FLAG_SYSTEM)

        if (isDailyModeOn && currWallpaperId == recordedWallpaperId) { // No change from last daily wallpaper set
            database.bingImageDao.mostRecentBingImage?.let { image ->
                if (image.date == LocalDate.now() && wpManager.isSetWallpaperAllowed) {
                    val uri = Uri.parse(image.imageDeviceUri)

                    // Get wallpaper and crop as needed
                    contentResolver.openInputStream(uri)?.use { inputStream ->
                        val bingWallpaper = BitmapFactory.decodeStream(inputStream)
                        val minSize = min(bingWallpaper.height, bingWallpaper.width)
                        val minDelta = minSize / 2
                        val leftDelta = (bingWallpaper.width / 2) - minDelta
                        val topDelta = (bingWallpaper.height / 2) - minDelta
                        val rect = Rect(
                            leftDelta,
                            topDelta,
                            leftDelta + minSize,
                            topDelta + minSize
                        )
                        wpManager.setBitmap(bingWallpaper, rect, true, FLAG_SYSTEM)
                    }
                }
            }
        } else { // Daily mode has been disrupted and should be toggled off
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