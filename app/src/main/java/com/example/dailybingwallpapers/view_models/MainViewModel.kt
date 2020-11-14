package com.example.dailybingwallpapers.view_models

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.dailybingwallpapers.storage.database.entities.BingImage
import com.example.dailybingwallpapers.storage.database.repos.BingImageRepository

class MainViewModel(private val repo: BingImageRepository): ViewModel() {
    companion object {
        val FACTORY = singleArgViewModelFactory(::MainViewModel)
    }

    val galleryImages = repo.bingImages

    private val _previewImage = MutableLiveData<BingImage?>()
    val previewImage: LiveData<BingImage?>
        get() = _previewImage

    fun onPreviewWallpaperSelected(image: BingImage?) {
        _previewImage.value = image
    }
}