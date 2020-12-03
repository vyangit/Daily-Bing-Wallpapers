package com.example.dailybingwallpapers.app.receivers

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat.startForegroundService
import com.example.dailybingwallpapers.app.services.BingImageImportService
import java.util.*

class ImportServiceReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        scheduleServiceOnBoot(context)
        if (intent.action == ACTION_APP_REFRESH_BACKGROUND) {
            callService(context)
        }
    }

    private fun scheduleServiceOnBoot(context: Context) {
        // Bing Image API adds a new image everyday at 00:00 EST time
        // Set up service to repeat everyday starting at 1am
        val intent = Intent(context, BingImageImportService::class.java)
        val pendingIntent = PendingIntent.getForegroundService(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Check and import wallpaper updates from the Bing Images API
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val cal = Calendar.getInstance()
        cal.timeInMillis = System.currentTimeMillis()
        cal.set(Calendar.HOUR_OF_DAY, 1)
        cal.set(Calendar.MINUTE, 0)
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            cal.timeInMillis,
            AlarmManager.INTERVAL_HOUR * 2,
            pendingIntent
        )
    }

    private fun callService(context: Context) {
        val intent = Intent(context, BingImageImportService::class.java)
        startForegroundService(context, intent)
    }

    companion object {
        const val ACTION_APP_REFRESH_BACKGROUND =
            "com.example.dailybingwallpapers.APP_REFRESH_BACKGROUND"
    }
}