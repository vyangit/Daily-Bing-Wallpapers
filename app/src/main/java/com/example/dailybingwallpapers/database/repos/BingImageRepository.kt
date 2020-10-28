package com.example.dailybingwallpapers.database.repos

import com.example.dailybingwallpapers.database.dao.BingImageDao
import com.example.dailybingwallpapers.network.MainNetwork

class BingImageRepository(val network: MainNetwork, val bingImageDao: BingImageDao) {

    suspend fun getLastFiveRecentBingImages() {
        //TODO: Implement
    }

}