package com.example.dailybingwallpapers.app.storage.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.dailybingwallpapers.app.storage.database.dao.BingImageDao
import com.example.dailybingwallpapers.app.storage.database.entities.BingImage

@Database(entities = [BingImage::class], version = 1, exportSchema = false)
abstract class AppDatabase: RoomDatabase() {
    abstract val bingImageDao: BingImageDao

    companion object {
        private lateinit var INSTANCE: AppDatabase

        fun getDatabase(context: Context): AppDatabase {
            synchronized(AppDatabase::class) {
                if (!Companion::INSTANCE.isInitialized) {
                    INSTANCE = Room
                        .databaseBuilder(
                            context.applicationContext,
                            AppDatabase::class.java,
                            "app_db"
                        )
                        .fallbackToDestructiveMigration()
                        .build()
                }
            }

            return INSTANCE
        }
    }
}
