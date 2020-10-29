package com.example.dailybingwallpapers.database.repos

import com.example.dailybingwallpapers.database.dao.BingImageDao
import com.example.dailybingwallpapers.network.BingWallpaperNetwork

class BingImageRepository(val network: BingWallpaperNetwork, val bingImageDao: BingImageDao) {

    suspend fun getLastFiveRecentBingImages() {
        //TODO: Implement
    }

}