package com.example.dailybingwallpapers.network

import android.app.DownloadManager
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.dailybingwallpapers.R
import com.example.dailybingwallpapers.network.dto.BingImageMetaDataDTO
import com.example.dailybingwallpapers.storage.database.entities.BingImage
import com.example.dailybingwallpapers.parsers.BingImageXmlParser
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import java.io.*
import java.lang.Integer.max
import java.lang.Integer.min
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.util.Objects.isNull

// TODO: Add support for mkt parameter
const val bingImageApiUrlFormat =
    "https://www.bing.com/HPImageArchive.aspx?format=xml&idx=%d&n=%d&mkt=en-US"
const val IMAGES_ONLINE_MAX = 16  // Bing Image API only saves the last 16 images online
const val ENTRIES_PER_QUERY_MAX = 8 // Bing Image API only allows queries of 8 entries at a time

class BingWallpaperNetwork {
    private val bingImageXmlParser = BingImageXmlParser()

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

    suspend fun getAllOnlineWallpapersSinceLastUpdate(context: Context): List<BingImage> {
        val sharedPrefs = context.getSharedPreferences(
            context.getString(R.string.shared_prefs_app_globals_file_key),
            Context.MODE_PRIVATE
            )
        val lastUpdateTime = sharedPrefs.getString(context.getString(R.string.shared_prefs_app_globals_last_update_time), "")
        if (lastUpdateTime.isNullOrBlank()) {
            return getAllOnlineWallpapers(context)
        } else {
            var period = Period.between(
                LocalDate.parse(
                    lastUpdateTime,
                    DateTimeFormatter.ISO_LOCAL_DATE),
                LocalDate.now()
            )

            var numMissedDays = 0
            if (period.years > 0 || period.months > 0) {
                numMissedDays = IMAGES_ONLINE_MAX
            } else if ( period.days > 0) {
                numMissedDays = max(IMAGES_ONLINE_MAX, period.days)
            }

            return getAllOnlineWallpapers(context)
        }
    }

    /**
     * Queries the Bing Image API for as many wallpaper images that are current online (max. ~16)
     */
    suspend fun getOnlineWallpapersSinceToday(context: Context, numImagesSinceToday: Int): List<BingImage> {
        return if (numImagesSinceToday < IMAGES_ONLINE_MAX)
            getWallpapers(context, 0, numImagesSinceToday) else
            getAllOnlineWallpapers(context)
    }


    /**
     * Queries the Bing Image API for as many wallpaper images that are current online (max. ~16)
     */
    suspend fun getAllOnlineWallpapers(context: Context): List<BingImage> {
        return getWallpapers(context, 0, IMAGES_ONLINE_MAX)
    }

    private suspend fun getWallpapers(
        context: Context,
        daysBeforeToday: Int = 0,
        numImagesSince: Int = 1
    ): List<BingImage> {
        val images = mutableListOf<BingImage>()

        if (daysBeforeToday < 0 || numImagesSince < 1) return images

        // Bing Image API will query the last N number of entries if [daysBeforeToday] is out of range
        var numPossibleEntries = min(numImagesSince, IMAGES_ONLINE_MAX - daysBeforeToday)
        if (numPossibleEntries == 0) return images
        var daysBefore = daysBeforeToday

        while (numPossibleEntries > 0) {
            // Get the xml to parse
            val qualifiedLink = bingImageApiUrlFormat.format(daysBeforeToday, min(numPossibleEntries, 8))

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
                val imageName = getUniqueBingImageName(data)
                val imageDeviceUri = importImageFromUrl(context, data.imageUrl, imageName)

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

            // Import and parse any remaining images
            numPossibleEntries -= 8
            daysBefore += 8
        }
        return images
    }

    private suspend fun importImageFromUrl(context: Context, imageUrl: String, imageName: String): String {
        // Set up file descriptors to save to device
        val appDir = Environment.DIRECTORY_PICTURES + File.separator + context.getString(R.string.app_name)

        val imageDetails = ContentValues().apply {
            put(MediaStore.Images.ImageColumns.DISPLAY_NAME, imageName)
            put(MediaStore.Images.ImageColumns.TITLE, imageName)
            put(MediaStore.Images.ImageColumns.MIME_TYPE, "image/jpg")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.ImageColumns.RELATIVE_PATH, appDir)
                put(MediaStore.Images.ImageColumns.IS_PENDING, 1)
            }
        }

        // Stream in image data to external storage
        val resolver = context.contentResolver
        val imageContentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        val cursor = getImageEntry(resolver, imageName, appDir)

        if (cursor.count == 0) {
            val uri = resolver.insert(imageContentUri, imageDetails)!!
            resolver.openOutputStream(uri, "w")!!.use { os ->
                // Fetch bitmap to save
                val bm = fetchImageFromUrl(imageUrl)
                val isWritten = bm.compress(Bitmap.CompressFormat.JPEG, 100, os)
            }

            // Update global access after import
            imageDetails.clear()
            imageDetails.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, imageDetails, null, null)
        }

        return imageContentUri.toString()
    }

    private fun getUniqueBingImageName(bingImageMetaDataDTO: BingImageMetaDataDTO): String {
        val imageUrlPart = bingImageMetaDataDTO.imageUrl.let { imageUrl ->
            imageUrl.substring(
                imageUrl.indexOf("id=") + 3,
                imageUrl.indexOf(".jpg") + 4
            )
        }

        val imageDateStamp = bingImageMetaDataDTO.date.toString().replace("-","")

        return imageDateStamp + '_' + imageUrlPart
    }

    private fun fetchImageFromUrl(imageUrl: String): Bitmap {
        val url = URL(imageUrl)
        val bis = BufferedInputStream(url.openStream())
        return BitmapFactory.decodeStream(bis)
    }

    private fun getImageEntry(resolver: ContentResolver, imageDisplayName: String, imageRelativePath: String): Cursor {
        val projection = mutableListOf(
            MediaStore.Images.ImageColumns.DISPLAY_NAME
        )

        var selectionClause = MediaStore.Files.FileColumns.DISPLAY_NAME + " = ?"
        val selectionArgs = mutableListOf<String>(imageDisplayName)

        return resolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection.toTypedArray(),
            selectionClause,
            selectionArgs.toTypedArray(),
            ""
        )!!
    }
}