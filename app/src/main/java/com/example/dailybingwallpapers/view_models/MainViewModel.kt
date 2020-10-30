package com.example.dailybingwallpapers.view_models

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.dailybingwallpapers.storage.database.entities.BingImage
import com.example.dailybingwallpapers.storage.database.repos.BingImageRepository

class MainViewModel(private val repo: BingImageRepository): ViewModel() {
    companion object {
        val FACTORY = singleArgViewModelFactory(::MainViewModel)
    }

    private val _previewImage = MutableLiveData<BingImage>()
    val previewImage: LiveData<BingImage>
        get() = _previewImage

    private val _galleryImages = MutableLiveData<List<BingImage>>()
    val galleryImages: LiveData<List<BingImage>>
        get() = _galleryImages

    fun onPreviewWallpaperSelected(image: BingImage) {
        _previewImage.value = image
    }

    fun onUpdateExploreView() {

    }

}