package com.example.dailybingwallpapers.app.receivers

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.dailybingwallpapers.app.services.BingImageImportService
import java.time.LocalTime

class OnDeviceBootReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            startServiceOnBoot(context)
        }
    }

    private fun startServiceOnBoot(context: Context) {
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
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, LocalTime.of(1, 0).toNanoOfDay()*1000,
            1000*60*60*24, // Every day
            pendingIntent
        )
    }

}