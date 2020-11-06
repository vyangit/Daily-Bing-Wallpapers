package com.example.dailybingwallpapers

import android.app.Notification
import android.app.Notification.Builder
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.GridView
import android.widget.ImageView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.dailybingwallpapers.adapters.BingImageAdapter
import com.example.dailybingwallpapers.network.BingWallpaperNetwork
import com.example.dailybingwallpapers.services.BingImageImportService
import com.example.dailybingwallpapers.storage.database.AppDatabase
import com.example.dailybingwallpapers.storage.database.entities.BingImage
import com.example.dailybingwallpapers.storage.database.repos.BingImageRepository
import com.example.dailybingwallpapers.view_models.MainViewModel
import com.google.android.material.textview.MaterialTextView

class MainActivity : AppCompatActivity() {
    private lateinit var wallpaperGalleryGridRecyclerView: RecyclerView
    private lateinit var wallpaperGalleryGridAdapter: BingImageAdapter
    private lateinit var wallpaperGalleryGridLayoutManager: RecyclerView.LayoutManager


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val previewHeadlineText: MaterialTextView = findViewById(R.id.activity_main_preview_headline)
        val previewImage: ImageView = findViewById(R.id.activity_main_preview_wallpaper_image)
        val previewDetailsDateText: MaterialTextView = findViewById(R.id.activity_main_details_date)
        val previewDetailsCopyrightText: MaterialTextView = findViewById(R.id.activity_main_details_copyright)
        val previewDetailsCopyrightLinkText: MaterialTextView = findViewById(R.id.activity_main_details_copyright_link)

        val database = AppDatabase.getDatabase(this)
        val network = BingWallpaperNetwork.getService()
        val repo = BingImageRepository(this, network, database.bingImageDao)
        val viewModel = ViewModelProvider(this, MainViewModel.FACTORY(repo)).get(MainViewModel::class.java)

        wallpaperGalleryGridRecyclerView = findViewById<RecyclerView>(R.id.activity_main_wallpapers_gallery)
        wallpaperGalleryGridAdapter = BingImageAdapter()
        wallpaperGalleryGridLayoutManager = GridLayoutManager(this, 1, GridLayoutManager.HORIZONTAL, false)

        viewModel.previewImage.observe(this) {value ->
            value?.let {
                previewHeadlineText.text = value.headline
                previewImage.setImageURI(Uri.parse(value.imageDeviceUri))
                previewDetailsDateText.text = value.date.toString()
                previewDetailsCopyrightText.text = value.copyright
                previewDetailsCopyrightLinkText.text = value.copyrightLink
            }
        }

        viewModel.galleryImages.observe(this) {list ->
            list?.let {
                wallpaperGalleryGridAdapter.bingImages = list
            }
        }

        // Run an image scrap with the import service
        startForegroundService(Intent(this, BingImageImportService::class.java))
    }
}