package com.example.dailybingwallpapers.network

import android.content.ContentValues
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.dailybingwallpapers.storage.database.entities.BingImage
import com.example.dailybingwallpapers.parsers.BingImageXmlParser
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.DataInputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

// TODO: Add support for mkt
const val bingImageApiUrlFormat =
    "https://www.bing.com/HPImageArchive.aspx?format=xml&idx=%d&n=%d&mkt=en-US"

class BingWallpaperNetwork {
    val bingImageXmlParser = BingImageXmlParser()

    companion object {
        private lateinit var INSTANCE: BingWallpaperNetwork

        fun getService(): BingWallpaperNetwork {
            synchronized(BingWallpaperNetwork::class) {
                if (!::INSTANCE.isInitialized) {
                    INSTANCE = BingWallpaperNetwork()
                }
            }

            return INSTANCE
        }
    }

    suspend fun getDailyWallpaper(): BingImage {
//        val image = getWallpapersMetaData(0,1)
        return getWallpapers(0, 1)[0]
    }

    suspend fun getLastFiveWallpapers(): List<BingImage> {
        return getWallpapers(0, 5)
    }

    private suspend fun getWallpapers(
        daysBeforeToday: Int = 0,
        numImagesSince: Int = 1
    ): List<BingImage> {
        val images = mutableListOf<BingImage>()

        if (daysBeforeToday < 0 || numImagesSince < 1) return images

        // Get the xml to parse
        val qualifiedLink = bingImageApiUrlFormat.format(daysBeforeToday, numImagesSince)

        //TODO: Fix blocking functions
        val url = withContext(IO) { URL(qualifiedLink) }
        val xml = withContext(IO) {
            url.run {
                openConnection().run {
                    this as HttpURLConnection
                    inputStream
                }
            }
        }

        // Parse xml
        val imageMetaData = bingImageXmlParser.parse(xml)

        for (data in imageMetaData) {
            // Import image from url to device for caching
            val imageDeviceUri = importImageFromUrl(data.imageUrl)

            // Create entity objects from dto information
            images.add(
                BingImage(
                    0,
                    data.date,
                    data.imageUrl,
                    imageDeviceUri,
                    data.copyright,
                    data.copyrightLink,
                    data.headline
                )
            )
        }

        return images
    }

    private suspend fun importImageFromUrl(imageUrl: String): String {
        val url = URL(imageUrl)
        val bis = BufferedInputStream(url.openStream())
        val bm = BitmapFactory.decodeStream(bis)

        val appDir = Environment.DIRECTORY_PICTURES + File.pathSeparator + "Daily_Bing_Wallpapers"
        val imageName = imageUrl.substring(
            imageUrl.indexOf("id=") + 3,
            imageUrl.indexOf(".jpg")
        )

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.ImageColumns.DISPLAY_NAME, imageName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg")
            put(MediaStore.Images.ImageColumns.RELATIVE_PATH, appDir)
        }

        val contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI


        return ""
    }
}