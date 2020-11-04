package com.example.dailybingwallpapers.storage.database.repos

import androidx.lifecycle.LiveData
import com.example.dailybingwallpapers.storage.database.dao.BingImageDao
import com.example.dailybingwallpapers.network.BingWallpaperNetwork
import com.example.dailybingwallpapers.storage.database.entities.BingImage

class BingImageRepository(private val network: BingWallpaperNetwork, private val bingImageDao: BingImageDao) {

    val bingImages: LiveData<List<BingImage>> = bingImageDao.allBingImages

    suspend fun importMissingBingImages() {
        //TODO: Implement
    }

    suspend fun getLastFiveRecentBingImages() {
        //TODO: Implement when adding no saving option wallpapers on phone
    }

}