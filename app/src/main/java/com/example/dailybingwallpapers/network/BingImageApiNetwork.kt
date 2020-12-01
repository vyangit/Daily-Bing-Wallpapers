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

// TODO: Add support for mkt parameter
// TODO: Readjust for Bing Image APIs idx threshold case (after idx=8 all entries start from idx=8)
const val bingImageApiUrlFormat =
    "https://www.bing.com/HPImageArchive.aspx?format=xml&idx=%d&n=%d&mkt=en-US"
const val IMAGES_ONLINE_MAX = 15  // Bing Image API only saves the last 16 images online
const val ENTRIES_PER_QUERY_MAX = 8 // Bing Image API only allows queries of 8 entries at a time

class BingImageApiNetwork {
    private val bingImageXmlParser = BingImageXmlParser()

    companion object {
        private lateinit var INSTANCE: BingImageApiNetwork

        fun getService(): BingImageApiNetwork {
            synchronized(BingImageApiNetwork::class) {
                if (!::INSTANCE.isInitialized) {
                    INSTANCE = BingImageApiNetwork()
                }
            }

            return INSTANCE
        }
    }

    suspend fun fetchImageByUrl(url: String?): Bitmap? {
        if (url == null) return null
        return fetchImageFromUrl(url)
    }

    suspend fun fetchImageMetaDataByDate(date: LocalDate): BingImageMetaDataDTO? {
        if (isDateOutOfBoundOfMaxRange(date)) return null
        val daysBeforeToday = Period.between(LocalDate.now(), date)
        val metaData = fetchImageMetaData(daysBeforeToday.days)
        return if (metaData.isEmpty()) null else metaData[0]
    }

    suspend fun fetchImageMetaDataSinceDate(prevDate: LocalDate): List<BingImageMetaDataDTO> {
        val period = Period.between(prevDate, LocalDate.now())

        var numMissedDays = 0
        if (period.years > 0 || period.months > 0) {
            numMissedDays = IMAGES_ONLINE_MAX
        } else if (period.days > 0) {
            numMissedDays = min(IMAGES_ONLINE_MAX, period.days)
        }

        return fetchImageMetaDataSinceToday(numMissedDays)
    }

    suspend fun importImageFromUrlToDevice(
        context: Context,
        imageUrl: String,
        imageName: String
    ): String? {
        val bm = fetchImageFromUrl(imageUrl) ?: return null
        return importBitmapToDevice(context, bm, imageName)
    }

    private suspend fun importBitmapToDevice(
        context: Context,
        bitmap: Bitmap,
        imageName: String
    ): String {
        // Set up file descriptors to save to device
        val appDir =
            Environment.DIRECTORY_PICTURES + File.separator + context.getString(R.string.app_name)

        // Stream in image data to external storage
        val resolver = context.contentResolver
        val externalContentUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(
                MediaStore.VOLUME_EXTERNAL_PRIMARY
            )
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        // Obtain valid device uri
        lateinit var uri: Uri

        val cursor = getImageEntry(resolver, imageName)

        if (cursor.count != 0 && cursor.moveToFirst()) { // Image already exists
            val id = cursor.getInt(cursor.getColumnIndex(MediaStore.MediaColumns._ID))
            cursor.close()
            uri = Uri.withAppendedPath(externalContentUri, id.toString())
        } else { // Insert new entry if doesn't exist

            val imageDetails = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, imageName)
                put(MediaStore.Images.Media.TITLE, imageName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpg")

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, appDir)
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val imageDeviceUri = resolver.insert(externalContentUri, imageDetails)

            imageDeviceUri?.let { tmpUri ->
                resolver.openOutputStream(tmpUri, "w")!!.use { os ->
                    // Save bitmap
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os)
                }

                // Update global access after import
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    imageDetails.clear()
                    imageDetails.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(tmpUri, imageDetails, null, null)
                }
            }

            uri = imageDeviceUri ?: Uri.EMPTY
        }

        return uri.toString()
    }

    /**
     * Queries the Bing Image API for as many wallpaper images that are current online (max. ~16)
     */
    suspend fun fetchAllImageMetaData(): List<BingImageMetaDataDTO> {
        return fetchImageMetaData(0, IMAGES_ONLINE_MAX)
    }

    /**
     * Queries the Bing Image API for as many wallpaper images that are current online (max. ~16)
     */
    private suspend fun fetchImageMetaDataSinceToday(numImagesSinceToday: Int): List<BingImageMetaDataDTO> {
        return if (numImagesSinceToday < IMAGES_ONLINE_MAX)
            fetchImageMetaData(0, numImagesSinceToday) else
            fetchAllImageMetaData()
    }

    private suspend fun fetchImageMetaData(
        daysBeforeToday: Int = 0,
        numImagesSince: Int = 1
    ): List<BingImageMetaDataDTO> {
        val images = mutableListOf<BingImageMetaDataDTO>()

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
                images.add(data)
            }

            // Import and parse any remaining images
            i -= ENTRIES_PER_QUERY_MAX
            j += ENTRIES_PER_QUERY_MAX
        }
        return images
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
        val projection = arrayOf(
            MediaStore.Images.Media._ID
        )
        val selectionClause = MediaStore.Images.Media.DISPLAY_NAME + " = ?"
        val selectionArgs = mutableListOf(imageDisplayName)

        return resolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            selectionClause,
            selectionArgs.toTypedArray(),
            ""
        )!!
    }
}