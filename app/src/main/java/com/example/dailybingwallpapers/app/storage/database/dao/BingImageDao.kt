package com.example.dailybingwallpapers.app.storage.database.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.dailybingwallpapers.app.storage.database.entities.BingImage

@Dao
interface BingImageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBingImage(bingImage: BingImage)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllBingImages(bingImages: List<BingImage>)

    @get:Query("select * from bing_image order by date desc")
    val allBingImages: LiveData<List<BingImage>>

    @get:Query("select * from bing_image order by date desc limit 1")
    val mostRecentBingImage: BingImage?
}