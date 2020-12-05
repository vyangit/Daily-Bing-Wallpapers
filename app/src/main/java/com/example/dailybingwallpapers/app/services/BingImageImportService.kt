package com.example.dailybingwallpapers.app.services

import android.app.*
import android.app.WallpaperManager.FLAG_LOCK
import android.app.WallpaperManager.FLAG_SYSTEM
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.example.dailybingwallpapers.R
import com.example.dailybingwallpapers.app.receivers.DailyWallpaperRefreshReceiver
import com.example.dailybingwallpapers.app.services.interfaces.ForegroundService
import com.example.dailybingwallpapers.app.storage.database.AppDatabase
import com.example.dailybingwallpapers.app.storage.database.repos.BingImageRepository
import com.example.dailybingwallpapers.app.utils.PreferencesUtil
import com.example.dailybingwallpapers.network.BingImageApiNetwork
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.floor
import kotlin.math.min

const val UPDATES_CHANNEL_IMPORTANCE = NotificationManager.IMPORTANCE_LOW

class BingImageImportService : Service(), ForegroundService {
    private lateinit var database: AppDatabase
    private lateinit var network: BingImageApiNetwork
    private lateinit var repo: BingImageRepository
    private lateinit var wallpaperManager: WallpaperManager

    override fun onCreate() {
        super.onCreate()

        database = AppDatabase.getDatabase(this)
        network = BingImageApiNetwork.getService()
        repo = BingImageRepository(this, network, database.bingImageDao)
        wallpaperManager = WallpaperManager.getInstance(applicationContext)

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
            // Check if wifi mode is on before executing network ops
            val prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext)
            val isWifiOnlyOn = prefs.getBoolean(
                getString(R.string.default_prefs_wifi_only),
                false
            )
            val wifiManager = getSystemService(Context.WIFI_SERVICE) as WifiManager
            val isWifiOn = wifiManager.isWifiEnabled
            if ((isWifiOnlyOn && isWifiOn) || !isWifiOnlyOn) {
                importLatestBingImages()
                importMissingBingImages()
            }
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

        if (PreferencesUtil.isDailyModeOn(this) && wallpaperManager.isSetWallpaperAllowed) { // No change from last daily wallpaper set
            database.bingImageDao.mostRecentBingImage?.let { image ->
                val lastDailyUpdateDate = sharedPrefs.getString(
                    getString(R.string.shared_prefs_app_globals_last_daily_mode_update_date),
                    ""
                )

                if (PreferencesUtil.hasDailyTargetModeChanged(this)
                    || lastDailyUpdateDate != LocalDate.now().toString()
                ) {
                    val uri = Uri.parse(image.imageDeviceUri)

                    // Get wallpaper and crop as needed
                    contentResolver.openInputStream(uri)?.use { inputStream ->
                        val bingWallpaper = BitmapFactory.decodeStream(inputStream)
                        val wallpaperId = setDailyImage(bingWallpaper)

                        // Update daily wallpaper refresh
                        sharedPrefs.edit {
                            putBoolean(
                                getString(R.string.shared_prefs_app_globals_daily_mode_on),
                                true
                            )
                            putString(
                                getString(R.string.shared_prefs_app_globals_last_daily_mode_update_date),
                                image.date.toString()
                            )
                            putString(
                                getString(R.string.shared_prefs_app_globals_wallpaper_target),
                                PreferenceManager.getDefaultSharedPreferences(baseContext)
                                    .getString(
                                        getString(R.string.default_prefs_wallpaper_targets),
                                        resources.getStringArray(R.array.root_preferences_header_wallpaper_target_values)[0]
                                    )
                            )
                            putInt(
                                getString(R.string.shared_prefs_app_globals_recorded_wallpaper_id),
                                wallpaperId
                            )
                        }

                        // Send broadcast of wallpaper refresh
                        sendBroadcast(Intent(DailyWallpaperRefreshReceiver.ACTION_APP_DAILY_WALLPAPER_REFRESHED))
                    }
                }
            }
        } else { // Daily mode has been disrupted and should be toggled off
            sharedPrefs.edit {
                putBoolean(getString(R.string.shared_prefs_app_globals_daily_mode_on), false)
                remove(getString(R.string.shared_prefs_app_globals_last_daily_mode_update_date))
                remove(getString(R.string.shared_prefs_app_globals_recorded_wallpaper_id))
            }
        }
    }


    /**
     * Sets the wallpaper based on the daily mode settings
     *
     * @param bingWallpaper Image to set
     *
     * @return The wallpaper id provided by {@link #WallpaperManager.getWallpaperId(Int)}
     */
    private fun setDailyImage(bingWallpaper: Bitmap): Int {
        val wallpaperTargetValue = PreferenceManager
            .getDefaultSharedPreferences(this)
            .getString(
                getString(R.string.default_prefs_wallpaper_targets), "WALLPAPER"
            )
        val wallpaperTargetsArray = resources.getStringArray(
            R.array.root_preferences_header_wallpaper_target_values
        )

        // Create crop rectangles for screens
        val wpHeight = bingWallpaper.height
        val wpWidth = bingWallpaper.width

        // Create centered landscape rect for system wallpaper
        val minDelta = min(wpHeight, wpWidth) / 2
        val leftDelta = (wpWidth / 2) - minDelta
        val topDelta = (wpHeight / 2) - minDelta
        val systemRect = Rect(
            leftDelta,
            topDelta,
            wpWidth - leftDelta,
            wpHeight - topDelta
        )

        // Create cropped center rect for lock screen wallpaper
        val metrics = DisplayMetrics()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            applicationContext.display?.getRealMetrics(metrics)
        } else {
            val windowManager = applicationContext.getSystemService(WINDOW_SERVICE) as WindowManager
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealMetrics(metrics)
        }

        val deviceWidth: Float = metrics.widthPixels.toFloat()
        val deviceHeight: Float = metrics.heightPixels.toFloat()
        val minRatio: Float =
            min(wpWidth.toFloat() / deviceWidth, wpHeight.toFloat() / deviceHeight)
        val cropWidth = deviceWidth * minRatio
        val cropHeight = deviceHeight * minRatio
        val lockLeftDelta = floor(wpWidth / 2 - cropWidth / 2).toInt()
        val lockTopDelta = floor(wpHeight / 2 - cropHeight / 2).toInt()
        val lockScreenRect = Rect(
            lockLeftDelta,
            lockTopDelta,
            wpWidth - lockLeftDelta,
            wpHeight - lockTopDelta
        )

        when (wallpaperTargetValue) {
            wallpaperTargetsArray[0] -> { // target is system
                wallpaperManager.setBitmap(bingWallpaper, systemRect, true, FLAG_SYSTEM)
                return wallpaperManager.getWallpaperId(FLAG_SYSTEM)
            }
            wallpaperTargetsArray[1] -> { // target is lock screen
                wallpaperManager.setBitmap(bingWallpaper, lockScreenRect, true, FLAG_LOCK)
                return wallpaperManager.getWallpaperId(FLAG_LOCK)
            }
            wallpaperTargetsArray[2] -> { // target is system and lock screen
                wallpaperManager.setBitmap(bingWallpaper, systemRect, true, FLAG_SYSTEM)
                wallpaperManager.setBitmap(bingWallpaper, lockScreenRect, true, FLAG_LOCK)
                return wallpaperManager.getWallpaperId(FLAG_SYSTEM)
            }
            else -> {
                //Do nothing
            }
        }

        return 0 // Failure case
    }

}