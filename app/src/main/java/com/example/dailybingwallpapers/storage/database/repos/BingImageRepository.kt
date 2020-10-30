package com.example.dailybingwallpapers.storage.database.repos

import com.example.dailybingwallpapers.storage.database.dao.BingImageDao
import com.example.dailybingwallpapers.network.BingWallpaperNetwork

class BingImageRepository(val network: BingWallpaperNetwork, val bingImageDao: BingImageDao) {

    suspend fun getLastFiveRecentBingImages() {
        //TODO: Implement
    }

}