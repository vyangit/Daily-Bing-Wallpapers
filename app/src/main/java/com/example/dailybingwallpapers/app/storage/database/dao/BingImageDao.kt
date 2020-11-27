package com.example.dailybingwallpapers.app.storage.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.dailybingwallpapers.app.storage.database.entities.BingImage
import com.example.dailybingwallpapers.app.storage.database.entities.BingImageCompositeKeyWithUri

@Dao
interface BingImageDao {
    // Reads
    @get:Query("select * from bing_image order by date desc")
    val allBingImages: LiveData<List<BingImage>>

    @get:Query("select * from bing_image order by date desc limit 1")
    val mostRecentBingImage: BingImage?

    @Query("select date, image_url, image_device_uri from bing_image")
    fun getAllCompositeKeysWithUri(): List<BingImageCompositeKeyWithUri>

    @Query("select * from bing_image where image_url = :image_url")
    fun getByUrl(image_url: String): BingImage?

    @Query("select image_device_uri from bing_image where image_url = :image_url")
    fun getUriByUrl(image_url: String): String?

    // Creates and Updates
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bingImage: BingImage)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bingImages: List<BingImage>)

    // Deletes
    @Delete(entity = BingImage::class)
    suspend fun deleteByCompositeKeyWithUri(compositeKeyWithUri: BingImageCompositeKeyWithUri)
}