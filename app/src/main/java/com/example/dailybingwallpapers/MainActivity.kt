package com.example.dailybingwallpapers

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.ContextMenu
import android.view.MenuInflater
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.dailybingwallpapers.adapters.BingImageAdapter
import com.example.dailybingwallpapers.network.BingWallpaperNetwork
import com.example.dailybingwallpapers.services.BingImageImportService
import com.example.dailybingwallpapers.storage.database.AppDatabase
import com.example.dailybingwallpapers.storage.database.entities.BingImage
import com.example.dailybingwallpapers.storage.database.repos.BingImageRepository
import com.example.dailybingwallpapers.utils.extensions.showSnackbar
import com.example.dailybingwallpapers.view_models.MainViewModel
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textview.MaterialTextView

const val PERMISSION_REQUEST_READ_STORAGE = 0

class MainActivity : AppCompatActivity(),
    BingImageAdapter.OnBingImageSelectedListener,
    BingImageAdapter.OnBingImageLongClickListener {
    private lateinit var layout: View

    private lateinit var mainViewModel: MainViewModel
    private lateinit var wallpaperGalleryGridRecyclerView: RecyclerView
    private lateinit var wallpaperGalleryGridAdapter: BingImageAdapter
    private lateinit var wallpaperGalleryGridLayoutManager: RecyclerView.LayoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        layout = findViewById(R.id.activity_main)

        requestStoragePermission()
    }

    override fun onCreateContextMenu(
        menu: ContextMenu?,
        v: View?,
        menuInfo: ContextMenu.ContextMenuInfo?
    ) {
        super.onCreateContextMenu(menu, v, menuInfo)
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.activity_main_gallery_item_menu, menu)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_READ_STORAGE) {
            if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initExternalStorageViews()
            }
        }
    }

    override fun onBingImageSelected(bingImage: BingImage) {
        val sharedPrefs = getSharedPreferences(
            getString(R.string.shared_prefs_app_globals_file_key),
            Context.MODE_PRIVATE
        )
        //TODO: Check current wallpaper and set marker for recycler
        mainViewModel.onPreviewWallpaperSelected(bingImage)
    }

    override fun onBingImageLongClickListener(view: View, bingImage: BingImage) {
        val pMenu = PopupMenu(this, view)
        pMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.activity_main_wallpapers_gallery_grid_item_menu_set_as -> {
                    val intent = Intent(Intent.ACTION_ATTACH_DATA).apply {
                        addCategory(Intent.CATEGORY_DEFAULT)
                        setDataAndType(Uri.parse(bingImage.imageDeviceUri), "image/*")
                        putExtra("mimeType", "image/*")
                    }
                    startActivity(Intent.createChooser(intent, "Set as"))

                    true
                }
                else -> false
            }

        }
        val inflater = pMenu.menuInflater
        inflater.inflate(R.menu.activity_main_gallery_item_menu, pMenu.menu)
        pMenu.show()
    }

    private fun requestStoragePermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED -> {
                // You can use the API that requires the permission.
                initExternalStorageViews()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)
            -> {
                layout.showSnackbar(
                    R.string.storage_permission_required,
                    Snackbar.LENGTH_INDEFINITE,
                    R.string.ok) {
                    requestPermissions(
                        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                        PERMISSION_REQUEST_READ_STORAGE
                    )
                }
            }
            else -> {
                layout.showSnackbar(
                    R.string.storage_permission_not_available,
                    Snackbar.LENGTH_INDEFINITE
                )

                requestPermissions(
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    PERMISSION_REQUEST_READ_STORAGE
                )
            }
        }
    }

    private fun initExternalStorageViews() {
        val database = AppDatabase.getDatabase(this)
        val network = BingWallpaperNetwork.getService()
        val repo = BingImageRepository(this, network, database.bingImageDao)
        mainViewModel = ViewModelProvider(this, MainViewModel.FACTORY(repo)).get(MainViewModel::class.java)

        // Show views
        val previewWrapper: RelativeLayout = findViewById(R.id.activity_main_preview_wallpaper_wrapper)
        val previewDeniedView: MaterialTextView = findViewById(R.id.activity_main_preview_wallpaper_image_storage_denied)

        wallpaperGalleryGridRecyclerView = findViewById(R.id.activity_main_wallpapers_gallery)
        val wallpaperGalleryGridDeniedView: MaterialTextView = findViewById(R.id.activity_main_wallpapers_gallery_storage_denied)

        previewDeniedView.visibility = View.GONE
        previewWrapper.visibility = View.VISIBLE

        wallpaperGalleryGridDeniedView.visibility = View.GONE
        wallpaperGalleryGridRecyclerView.visibility = View.VISIBLE

        // Set view customization/triggers
        wallpaperGalleryGridAdapter = BingImageAdapter()
        wallpaperGalleryGridLayoutManager = GridLayoutManager(this, 1, GridLayoutManager.HORIZONTAL, false)

        val previewImage: ImageView = findViewById(R.id.activity_main_preview_wallpaper_image)
        val previewHeadlineText: MaterialTextView = findViewById(R.id.activity_main_preview_headline)
        val previewDetailsDateText: MaterialTextView = findViewById(R.id.activity_main_details_date)
        val previewDetailsCopyrightText: MaterialTextView = findViewById(R.id.activity_main_details_copyright)
        val previewDetailsCopyrightLinkText: MaterialTextView = findViewById(R.id.activity_main_details_copyright_link)

        wallpaperGalleryGridAdapter.bingImageSelectedListener = this
        wallpaperGalleryGridAdapter.bingImageLongClickListener = this
        wallpaperGalleryGridRecyclerView.layoutManager = wallpaperGalleryGridLayoutManager
        wallpaperGalleryGridRecyclerView.adapter = wallpaperGalleryGridAdapter

        mainViewModel.previewImage.observe(this) {value ->
            value?.let {
                previewHeadlineText.text = value.headline
                previewImage.setImageURI(Uri.parse(value.imageDeviceUri))
                previewDetailsDateText.text = value.date.toString()
                previewDetailsCopyrightText.text = value.copyright
                previewDetailsCopyrightLinkText.text = value.copyrightLink
            }
        }

        mainViewModel.galleryImages.observe(this) {list ->
            list?.let {
                wallpaperGalleryGridAdapter.bingImages = list
                wallpaperGalleryGridAdapter.notifyDataSetChanged()
                mainViewModel.onPreviewWallpaperSelected(list[0])
            }
        }

        // Run an image scrap with the import service
        startForegroundService(Intent(this, BingImageImportService::class.java))
    }
}