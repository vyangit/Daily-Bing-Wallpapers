package com.example.dailybingwallpapers.network

import com.example.dailybingwallpapers.database.entities.BingImage
import com.example.dailybingwallpapers.parsers.BingImageXmlParser
import java.net.HttpURLConnection
import java.net.URL

const val bingImageApiUrlFormat = "https://www.bing.com/HPImageArchive.aspx?format=xml&idx=%&n=%"

class BingWallpaperNetwork {
    val bingImageXmlParser = BingImageXmlParser()

    suspend fun getDailyWallpaper(): BingImage {
//        val image = getWallpapersMetaData(0,1)
        return getWallpapersMetaData(0,1)[0]
    }

    suspend fun getLastFiveWallpapersMetaData(): List<BingImage> {
        return getWallpapersMetaData(0, 5)
    }

    private suspend fun getWallpapersMetaData(daysBeforeToday: Int = 0, numImagesSince: Int = 1): List<BingImage> {
        val images = ArrayList<BingImage>()
        if (daysBeforeToday < 0 || numImagesSince < 1) return images

        // Get the xml to parse
        val qualifiedLink = bingImageApiUrlFormat.format(daysBeforeToday, numImagesSince)
        val url = URL(qualifiedLink)
        val xml = url.run {
            openConnection().run{
                this as HttpURLConnection
                inputStream
            }
        }

        // Parse xml
        bingImageXmlParser.parse(xml)

        return images
    }

}