package com.example.dailybingwallpapers.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dailybingwallpapers.database.repos.BingImageRepository

class MainViewModel(private val repo: BingImageRepository): ViewModel() {

}