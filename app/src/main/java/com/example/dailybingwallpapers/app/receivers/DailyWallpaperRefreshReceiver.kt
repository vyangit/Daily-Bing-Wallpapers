package com.example.dailybingwallpapers.app.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

abstract class DailyWallpaperRefreshReceiver : BroadcastReceiver() {

    /**
     * Corresponding action response for when a broadcast is received
     */
    abstract fun receiveResponse()

    override fun onReceive(context: Context?, intent: Intent?) {
        receiveResponse()
    }

    companion object {
        const val ACTION_APP_DAILY_WALLPAPER_REFRESHED =
            "com.example.dailybingwallpapers.ACTION_DAILY_WALLPAPER_REFRESHED"
    }
}