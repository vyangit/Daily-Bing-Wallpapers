package com.example.dailybingwallpapers.app.view_models

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.dailybingwallpapers.app.storage.database.entities.BingImage
import com.example.dailybingwallpapers.app.storage.database.repos.BingImageRepository

class MainViewModel(repo: BingImageRepository) : ViewModel() {
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