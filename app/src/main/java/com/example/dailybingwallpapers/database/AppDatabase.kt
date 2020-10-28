package com.example.dailybingwallpapers.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.dailybingwallpapers.database.dao.BingImageDao
import com.example.dailybingwallpapers.database.entities.BingImage

@Database(entities = [BingImage::class], version = 1, exportSchema = false)
abstract class AppDatabase: RoomDatabase() {
    abstract val bingImageDao: BingImageDao
}