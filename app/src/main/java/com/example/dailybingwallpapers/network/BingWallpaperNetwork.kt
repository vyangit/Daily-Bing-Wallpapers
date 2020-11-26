package com.example.dailybingwallpapers.network

import android.annotation.SuppressLint
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
import com.example.dailybingwallpapers.app.parsers.BingImageXmlParser
import com.example.dailybingwallpapers.app.storage.database.entities.BingImage
import com.example.dailybingwallpapers.network.dto.BingImageMetaDataDTO
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.File
import java.lang.Integer.max
import java.lang.Integer.min
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter

// TODO: Add support for mkt parameter
// TODO: Readjust for Bing Image APIs idx threshold case (after idx=8 all entries start from idx=8)
const val bingImageApiUrlFormat =
    "https://www.bing.com/HPImageArchive.aspx?format=xml&idx=%d&n=%d&mkt=en-US"
const val IMAGES_ONLINE_MAX = 15  // Bing Image API only saves the last 16 images online
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

    suspend fun getOnlineWallpaperByUrl(url: String): Bitmap? {
        return fetchImageFromUrl(url)
    }

    suspend fun getOnlineWallpaperByDate(context: Context, date: LocalDate): Bitmap? {
        //TODO
        return null
    }

    suspend fun getAllOnlineWallpapersSinceLastUpdate(context: Context): List<BingImage> {
        val sharedPrefs = context.getSharedPreferences(
            context.getString(R.string.shared_prefs_app_globals_file_key),
            Context.MODE_PRIVATE
        )
        val lastUpdateTime = sharedPrefs.getString(
            context.getString(R.string.shared_prefs_app_globals_last_update_time),
            ""
        )
        if (lastUpdateTime.isNullOrBlank()) {
            return getAllOnlineWallpapers(context)
        } else {
            val period = Period.between(
                LocalDate.parse(
                    lastUpdateTime,
                    DateTimeFormatter.ISO_LOCAL_DATE
                ),
                LocalDate.now()
            )

            var numMissedDays = 0
            if (period.years > 0 || period.months > 0) {
                numMissedDays = IMAGES_ONLINE_MAX
            } else if ( period.days > 0) {
                numMissedDays = min(IMAGES_ONLINE_MAX, period.days)
            }

            return getOnlineWallpapersSinceToday(context, numMissedDays)
        }
    }

    /**
     * Queries the Bing Image API for as many wallpaper images that are current online (max. ~16)
     */
    private suspend fun getOnlineWallpapersSinceToday(context: Context, numImagesSinceToday: Int): List<BingImage> {
        return if (numImagesSinceToday < IMAGES_ONLINE_MAX)
            getWallpapers(context, 0, numImagesSinceToday) else
            getAllOnlineWallpapers(context)
    }


    /**
     * Queries the Bing Image API for as many wallpaper images that are current online (max. ~16)
     */
    private suspend fun getAllOnlineWallpapers(context: Context): List<BingImage> {
        return getWallpapers(context, 0, IMAGES_ONLINE_MAX)
    }

    private suspend fun getWallpapers(
        context: Context,
        daysBeforeToday: Int = 0,
        numImagesSince: Int = 1
    ): List<BingImage> {
        val images = mutableListOf<BingImage>()

        if (daysBeforeToday < 0 || numImagesSince < 1) return images

        // Bing Image API thresholds at 8 and always retrieves starting at 7 after (i.e idx=9->7)
        var i = min(numImagesSince, IMAGES_ONLINE_MAX - daysBeforeToday)
        if (i == 0) return images

        var j = daysBeforeToday

        while (i > 0) {
            // Get the xml to parse
            val daysBefore = min(7, j)
            var skips = max(0, j-7)
            val querySize = if (daysBefore < 7) i else i+skips

            val qualifiedLink = bingImageApiUrlFormat.format(daysBefore, querySize)

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
                if (skips-- > 0) continue

                // Import image from url to device for caching
                val imageName = getUniqueBingImageName(data)
                val imageDeviceUri = importImageFromUrl(context, data.imageUrl, imageName)

                // Create entity objects from dto information
                if (!imageDeviceUri.isBlank()) {
                    images.add(
                        BingImage(
                            data.date,
                            data.imageUrl,
                            imageDeviceUri,
                            data.copyright,
                            data.copyrightLink,
                            data.headline
                        )
                    )
                }
            }

            // Import and parse any remaining images
            i -= ENTRIES_PER_QUERY_MAX
            j += ENTRIES_PER_QUERY_MAX
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
        val externalContentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        val cursor = getImageEntry(resolver, imageName)

        if (cursor.count != 0 && cursor.moveToFirst()) { // Image already exists
            val id = cursor.getInt(cursor.getColumnIndex(MediaStore.MediaColumns._ID))
            cursor.close()
            return Uri.withAppendedPath(externalContentUri, id.toString()).toString()
        } else { // Insert new entry if doesn't exist
            val uri = resolver.insert(externalContentUri, imageDetails)!!
            resolver.openOutputStream(uri, "w")!!.use { os ->
                // Fetch bitmap to save
                val bm = fetchImageFromUrl(imageUrl)
                bm?.compress(Bitmap.CompressFormat.JPEG, 100, os)
            }

            // Update global access after import
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                imageDetails.clear()
                imageDetails.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, imageDetails, null, null)
            }
            return uri.toString()
        }
    }

    private fun getUniqueBingImageName(bingImageMetaDataDTO: BingImageMetaDataDTO): String {
        val imageUrlPart = bingImageMetaDataDTO.imageUrl.let { imageUrl ->
            imageUrl.substring(
                imageUrl.indexOf("id=") + 3,
                imageUrl.indexOf(".jpg") + 4
            )
        }

        val imageDateStamp = bingImageMetaDataDTO.date.toString().replace("-", "")

        return imageDateStamp + '_' + imageUrlPart
    }

    private fun fetchImageFromUrl(imageUrl: String): Bitmap? {
        val url = URL(imageUrl)
        val bis = BufferedInputStream(url.openStream())
        return BitmapFactory.decodeStream(bis)
    }

    private fun isDateOutOfBoundOfMaxRange(date: LocalDate): Boolean {
        val period = Period.between(date, LocalDate.now())

        return period.years > 0 || period.months > 0 || period.days > IMAGES_ONLINE_MAX
    }

    @SuppressLint("Recycle") // Closing responsiblility for caller
    private fun getImageEntry(resolver: ContentResolver, imageDisplayName: String): Cursor {
        val projection = mutableListOf(
            MediaStore.Images.Media._ID
        )
        val selectionClause = MediaStore.Files.FileColumns.DISPLAY_NAME + " = ?"
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