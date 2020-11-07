package com.example.dailybingwallpapers.storage.database.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.dailybingwallpapers.storage.database.entities.BingImage

@Dao
interface BingImageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBingImage(bingImage: BingImage)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllBingImages(bingImages: List<BingImage>)

    @get:Query("select * from bing_image")
    val allBingImages: LiveData<List<BingImage>>
}