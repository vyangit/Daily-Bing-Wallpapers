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

    @get:Query("select * from BingImage limit 5")
    val bingImageLastFiveLiveData: LiveData<List<BingImage>?>
}