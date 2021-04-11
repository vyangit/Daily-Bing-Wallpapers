package com.example.dailybingwallpapers.app.utils

import android.app.WallpaperManager
import android.content.Context
import androidx.preference.PreferenceManager
import com.example.dailybingwallpapers.R

class PreferencesUtil {
    companion object {
        /**
         * Validates if daily mode should stay on.
         *
         * @param context
         *
         * @returns False if daily mode if off or if any target wallpapers are changed, else true
         */
        fun isDailyModeOn(context: Context): Boolean {
            val wallpaperManager = WallpaperManager.getInstance(context)
            val sharedPrefs = context.getSharedPreferences(
                context.getString(R.string.shared_prefs_app_globals_file_key),
                Context.MODE_PRIVATE
            )
            val wallpaperTargetsArray = context.resources.getStringArray(
                R.array.root_preferences_header_wallpaper_target_values
            )
            val recordedSystemWallpaperId = sharedPrefs.getInt(
                context.getString(R.string.shared_prefs_app_globals_recorded_system_wallpaper_id),
                -1
            )
            val recordedLockWallpaperId = sharedPrefs.getInt(
                context.getString(R.string.shared_prefs_app_globals_recorded_lock_wallpaper_id),
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

            if (recordedSystemWallpaperId == -1) return isDailyModeOn

            when (lastWallpaperTarget) {
                wallpaperTargetsArray[0] -> {
                    isDailyModeOn = isDailyModeOn &&
                            recordedSystemWallpaperId == wallpaperManager.getWallpaperId(
                        WallpaperManager.FLAG_SYSTEM
                    )
                }
                wallpaperTargetsArray[1] -> {
                    isDailyModeOn = isDailyModeOn &&
                            recordedLockWallpaperId == wallpaperManager.getWallpaperId(
                        WallpaperManager.FLAG_LOCK
                    )
                }
                wallpaperTargetsArray[2] -> {
                    isDailyModeOn = isDailyModeOn &&
                            recordedLockWallpaperId == wallpaperManager.getWallpaperId(
                        WallpaperManager.FLAG_LOCK
                    ) &&
                            recordedSystemWallpaperId == wallpaperManager.getWallpaperId(
                        WallpaperManager.FLAG_SYSTEM
                    )
                }
                else -> {
                    // Do nothing
                    return false
                }
            }

            return isDailyModeOn
        }

        /**
         * Checks if the user has changed which wallpapers should be changed upon refresh request
         *
         * @param context
         *
         * @return False if target mode has changed, else true
         */
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