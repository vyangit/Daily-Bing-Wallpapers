package com.example.dailybingwallpapers.storage.database.repos

import android.content.Context
import androidx.lifecycle.LiveData
import com.example.dailybingwallpapers.storage.database.dao.BingImageDao
import com.example.dailybingwallpapers.network.BingWallpaperNetwork
import com.example.dailybingwallpapers.storage.database.entities.BingImage

class BingImageRepository(
    private val context: Context,
    private val network: BingWallpaperNetwork,
    private val bingImageDao: BingImageDao) {

    val bingImages: LiveData<List<BingImage>> = bingImageDao.allBingImages

    suspend fun importMissingBingImages() {
        val bingImageMetaData = network.getAllOnlineWallpapersSinceLastUpdate(context)
//        bingImageDao.insertAllBingImages(bingImageMetaData)
    }

    suspend fun getLastFiveRecentBingImages() {
        //TODO: Implement when adding no saving option wallpapers on phone
    }

}