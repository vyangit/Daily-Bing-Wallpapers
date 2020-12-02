package com.example.dailybingwallpapers.app.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

abstract class DailyWallpaperRefreshReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        receiveResponse()
    }

    abstract fun receiveResponse()

    companion object {
        const val ACTION_DAILY_WALLPAPER_REFRESHED =
            "com.example.dailybingwallpapers.ACTION_DAILY_WALLPAPER_REFRESHED"
    }
}