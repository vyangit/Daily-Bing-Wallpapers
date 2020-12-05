package com.example.dailybingwallpapers.app.utils

import android.app.WallpaperManager
import android.content.Context
import androidx.preference.PreferenceManager
import com.example.dailybingwallpapers.R

class PreferencesUtil {
    companion object {
        fun isDailyModeOn(context: Context): Boolean {
            val wallpaperManager = WallpaperManager.getInstance(context)
            val sharedPrefs = context.getSharedPreferences(
                context.getString(R.string.shared_prefs_app_globals_file_key),
                Context.MODE_PRIVATE
            )
            val wallpaperTargetsArray = context.resources.getStringArray(
                R.array.root_preferences_header_wallpaper_target_values
            )
            val recordedWallpaperId = sharedPrefs.getInt(
                context.getString(R.string.shared_prefs_app_globals_recorded_wallpaper_id),
                -1
            )
            val lastWallpaperTarget = sharedPrefs.getString(
                context.getString(R.string.shared_prefs_app_globals_wallpaper_target),
                wallpaperTargetsArray[0]
            )
            var isDailyModeOn = sharedPrefs.getBoolean(
                context.getString(R.string.shared_prefs_app_globals_daily_mode_on),
                false
            )

            if (recordedWallpaperId == -1) return isDailyModeOn

            when (lastWallpaperTarget) {
                wallpaperTargetsArray[0] -> {
                    isDailyModeOn = isDailyModeOn &&
                            recordedWallpaperId == wallpaperManager.getWallpaperId(WallpaperManager.FLAG_SYSTEM)
                }
                wallpaperTargetsArray[1] -> {
                    isDailyModeOn = isDailyModeOn &&
                            recordedWallpaperId == wallpaperManager.getWallpaperId(WallpaperManager.FLAG_LOCK)
                }
                wallpaperTargetsArray[2] -> {
                    isDailyModeOn = isDailyModeOn &&
                            recordedWallpaperId == wallpaperManager.getWallpaperId(WallpaperManager.FLAG_LOCK) &&
                            recordedWallpaperId == wallpaperManager.getWallpaperId(WallpaperManager.FLAG_SYSTEM)
                }
                else -> {
                    // Do nothing
                    return false
                }
            }

            return isDailyModeOn
        }

        fun hasDailyTargetModeChanged(context: Context): Boolean {
            val wallpaperTargetsArray = context.resources.getStringArray(
                R.array.root_preferences_header_wallpaper_target_values
            )
            val recordedWpTarget = context.getSharedPreferences(
                context.getString(R.string.shared_prefs_app_globals_file_key),
                Context.MODE_PRIVATE
            ).getString(
                context.getString(R.string.shared_prefs_app_globals_wallpaper_target),
                wallpaperTargetsArray[0]
            )
            val currentWpTarget = PreferenceManager.getDefaultSharedPreferences(context).getString(
                context.getString(R.string.default_prefs_wallpaper_targets),
                wallpaperTargetsArray[0]
            )

            return recordedWpTarget != currentWpTarget
        }
    }
}