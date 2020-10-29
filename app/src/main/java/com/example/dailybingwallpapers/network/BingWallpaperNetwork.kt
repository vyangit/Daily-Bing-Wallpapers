package com.example.dailybingwallpapers.network

import com.example.dailybingwallpapers.database.entities.BingImage
import com.example.dailybingwallpapers.parsers.BingImageXmlParser
import java.net.HttpURLConnection
import java.net.URL

// TODO: Add support for mkt
const val bingImageApiUrlFormat = "https://www.bing.com/HPImageArchive.aspx?format=xml&idx=%d&n=%d&mkt=en-US"

class BingWallpaperNetwork {
    val bingImageXmlParser = BingImageXmlParser()

    suspend fun getDailyWallpaper(): BingImage {
//        val image = getWallpapersMetaData(0,1)
        return getWallpapers(0,1)[0]
    }

    suspend fun getLastFiveWallpapers(): List<BingImage> {
        return getWallpapers(0, 5)
    }

    private suspend fun getWallpapers(daysBeforeToday: Int = 0, numImagesSince: Int = 1): List<BingImage> {
        val images = mutableListOf<BingImage>()

        if (daysBeforeToday < 0 || numImagesSince < 1) return images

        // Get the xml to parse
        val qualifiedLink = bingImageApiUrlFormat.format(daysBeforeToday, numImagesSince)

        //TODO: Fix blocking functions
        val url = URL(qualifiedLink)
        val xml = url.run {
            openConnection().run{
                this as HttpURLConnection
                inputStream
            }
        }

        // Parse xml
        val imageMetaData = bingImageXmlParser.parse(xml)

        for (data in imageMetaData) {
            // Import image from url to device for caching
            val imageDeviceUri = importImageFromUrl(data.imageUrl)

            // Create entity objects from dto information
            images.add(BingImage(
                0,
                data.date,
                data.imageUrl,
                imageDeviceUri,
                data.copyright,
                data.copyrightLink,
                data.headline
            ))
        }

        return images
    }

    private suspend fun importImageFromUrl(imageUrl: String): String {
        //TODO: Implement
        return "hello"
    }
}