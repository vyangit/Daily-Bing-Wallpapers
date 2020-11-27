package com.example.dailybingwallpapers.app.storage.database.repos

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.LiveData
import com.example.dailybingwallpapers.R
import com.example.dailybingwallpapers.app.storage.database.dao.BingImageDao
import com.example.dailybingwallpapers.app.storage.database.entities.BingImage
import com.example.dailybingwallpapers.app.storage.database.entities.BingImageCompositeKeyWithUri
import com.example.dailybingwallpapers.network.BingImageApiNetwork
import com.example.dailybingwallpapers.network.dto.BingImageMetaDataDTO
import java.io.FileNotFoundException
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class BingImageRepository(
    private val context: Context,
    private val network: BingImageApiNetwork,
    private val bingImageDao: BingImageDao
) {

    val bingImages: LiveData<List<BingImage>> = bingImageDao.allBingImages

    fun getAllCompositeKeys(): List<BingImageCompositeKeyWithUri> {
        return bingImageDao.getAllCompositeKeysWithUri()
    }

    suspend fun createLatestBingImages() {
        // Access prev update date
        val sharedPrefs = context.getSharedPreferences(
            context.getString(R.string.shared_prefs_app_globals_file_key),
            Context.MODE_PRIVATE
        )
        val lastUpdateTime = sharedPrefs.getString(
            context.getString(R.string.shared_prefs_app_globals_last_update_time),
            ""
        )!!

        // Get metadata since last update date
        lateinit var bingImageMetaData: List<BingImageMetaDataDTO>
        if (lastUpdateTime.isNotBlank()) {
            val prevDate = LocalDate.parse(
                lastUpdateTime,
                DateTimeFormatter.ISO_LOCAL_DATE
            )
            bingImageMetaData = network.fetchImageMetaDataSinceDate(prevDate)
        } else {
            bingImageMetaData = network.fetchAllImageMetaData()
        }

        // Create new bing images for missing ones
        val bingImages = mutableListOf<BingImage>()
        for (data in bingImageMetaData) {
            val imageName = generateUniqueBingImageName(data)
            val imageDeviceUri = network.importImageFromUrlToDevice(
                context,
                data.imageUrl,
                imageName
            )

            // Create entity objects from dto information
            if (imageDeviceUri.isNotBlank()) {
                bingImages.add(
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

        // Store in app database
        bingImageDao.insert(bingImages)
    }

    /**
     * @param url The corresponding url for the image
     *
     * @return Boolean True if deletion is successful
     * False if delete is unsuccessful.
     */
    suspend fun deleteByCompositeKeyIfInvalid(keyWithUri: BingImageCompositeKeyWithUri): Boolean {
        // Check if device uri is invalid
        val deviceUri = Uri.parse(keyWithUri.imageDeviceUri)
        val resolver = context.contentResolver
        try {
            // Image is valid cause a file exists
            resolver.openInputStream(deviceUri)
            return false
        } catch (e: FileNotFoundException) {
            // Try retrieving from url or date range
            var bm = network.fetchImageByUrl(keyWithUri.imageUrl)
            if (bm == null) {
                val metaData = network.fetchImageMetaDataByDate(keyWithUri.date)
                bm = network.fetchImageByUrl(metaData?.imageUrl)
            }
            if (bm == null) {
                bingImageDao.deleteByCompositeKeyWithUri(keyWithUri)
                return true
            }

            // Re-copy file to device drive
            resolver.openOutputStream(deviceUri, "w")!!.use { os ->
                bm.compress(Bitmap.CompressFormat.JPEG, 100, os)
            }

            // Image is valid after copying back the file
            return false
        }
    }

    suspend fun deleteByCompositeKey(keyWithUri: BingImageCompositeKeyWithUri) {
        bingImageDao.deleteByCompositeKeyWithUri(keyWithUri)
    }

    private fun generateUniqueBingImageName(bingImageMetaDataDTO: BingImageMetaDataDTO): String {
        val imageUrlPart = bingImageMetaDataDTO.imageUrl.let { imageUrl ->
            imageUrl.substring(
                imageUrl.indexOf("id=") + 3,
                imageUrl.indexOf(".jpg") + 4
            )
        }

        val imageDateStamp = bingImageMetaDataDTO.date.toString().replace("-", "")
        return imageDateStamp + '_' + imageUrlPart
    }
}