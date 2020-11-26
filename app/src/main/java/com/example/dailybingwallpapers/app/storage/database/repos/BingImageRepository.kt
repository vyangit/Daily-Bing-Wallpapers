package com.example.dailybingwallpapers.app.storage.database.repos

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.LiveData
import com.example.dailybingwallpapers.app.storage.database.dao.BingImageDao
import com.example.dailybingwallpapers.app.storage.database.entities.BingImage
import com.example.dailybingwallpapers.app.storage.database.entities.BingImageCompositeKeyWithUri
import com.example.dailybingwallpapers.network.BingWallpaperNetwork
import java.io.FileNotFoundException

class BingImageRepository(
    private val context: Context,
    private val network: BingWallpaperNetwork,
    private val bingImageDao: BingImageDao
) {

    val bingImages: LiveData<List<BingImage>> = bingImageDao.allBingImages

    fun getAllCompositeKeys(): List<BingImageCompositeKeyWithUri> {
        return getAllCompositeKeys()
    }

    suspend fun createMissingBingImages() {
        val bingImageMetaData = network.getAllOnlineWallpapersSinceLastUpdate(context)

        bingImageDao.insert(bingImageMetaData)
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
            var bm = network.getOnlineWallpaperByUrl(keyWithUri.imageUrl)
            if (bm == null) {
                bm = network.getOnlineWallpaperByDate(context, keyWithUri.date)
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

        // Image file doesn't exist and failed to be re-imported
        return true
    }

    suspend fun deleteByCompositeKey(keyWithUri: BingImageCompositeKeyWithUri) {
        bingImageDao.deleteByCompositeKeyWithUri(keyWithUri)
    }
}