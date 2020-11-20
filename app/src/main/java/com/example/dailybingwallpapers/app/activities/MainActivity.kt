package com.example.dailybingwallpapers.app.activities

import android.Manifest
import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.ContextMenu
import android.view.MenuInflater
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.dailybingwallpapers.R
import com.example.dailybingwallpapers.app.adapters.BingImageAdapter
import com.example.dailybingwallpapers.app.extensions.showSnackbar
import com.example.dailybingwallpapers.app.receivers.BootImportServiceReceiver
import com.example.dailybingwallpapers.app.receivers.BootImportServiceReceiver.Companion.ACTION_APP_REFRESH_BACKGROUND
import com.example.dailybingwallpapers.app.services.BingImageImportService
import com.example.dailybingwallpapers.app.storage.database.AppDatabase
import com.example.dailybingwallpapers.app.storage.database.entities.BingImage
import com.example.dailybingwallpapers.app.storage.database.repos.BingImageRepository
import com.example.dailybingwallpapers.app.view_models.MainViewModel
import com.example.dailybingwallpapers.network.BingWallpaperNetwork
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textview.MaterialTextView

const val PERMISSION_REQUEST_READ_STORAGE = 0

class MainActivity : AppCompatActivity(),
    BingImageAdapter.OnBingImageSelectedListener,
    BingImageAdapter.OnBingImageLongClickListener,
    BingImageAdapter.OnDailyItemSelectedListener,
    BingImageAdapter.OnDailyItemLongClickListener {

    private lateinit var sharedPrefs: SharedPreferences
    private lateinit var wallpaperManager: WallpaperManager
    private lateinit var mainViewModel: MainViewModel

    private lateinit var layout: View
    private lateinit var wallpaperGalleryGridRecyclerView: RecyclerView
    private lateinit var wallpaperGalleryGridAdapter: BingImageAdapter
    private lateinit var wallpaperGalleryGridLayoutManager: RecyclerView.LayoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        layout = findViewById(R.id.activity_main)
        sharedPrefs = getSharedPreferences(
            getString(R.string.shared_prefs_app_globals_file_key),
            Context.MODE_PRIVATE
        )
        wallpaperManager = WallpaperManager.getInstance(this)

        requestStoragePermission {
            initExternalStorageViews()
        }
    }

    override fun onResume() {
        super.onResume()

        updatePrefsIfWallpaperChanged()
    }

    override fun onCreateContextMenu(
        menu: ContextMenu?,
        v: View?,
        menuInfo: ContextMenu.ContextMenuInfo?
    ) {
        super.onCreateContextMenu(menu, v, menuInfo)
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.menu_activity_main_gallery_item, menu)
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
        //TODO: Check current wallpaper and set marker for recycler
        mainViewModel.onPreviewWallpaperSelected(bingImage)
    }

    override fun onBingImageLongClickListener(view: View, bingImage: BingImage) {
        val pMenu = PopupMenu(this, view)
        pMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.activity_main_wallpapers_gallery_item_menu_set_as -> {
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
        inflater.inflate(R.menu.menu_activity_main_gallery_item, pMenu.menu)
        pMenu.show()
    }

    override fun onDailyItemSelected() {
        mainViewModel.onPreviewWallpaperSelected(null)
    }

    override fun onDailyItemLongClick(view: View) {
        val pMenu = PopupMenu(this, view)
        pMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.activity_main_wallpapers_gallery_daily_item_menu_set_wallpaper -> {
                    val dialogBuilder = AlertDialog.Builder(this).apply {
                        setTitle(R.string.activity_main_preview_wallpapers_gallery_daily_item_dialog_title)
                        setMessage(R.string.activity_main_preview_wallpapers_gallery_daily_item_dialog_message)
                        setPositiveButton(R.string.activity_main_preview_wallpapers_gallery_daily_item_dialog_positive_button) { _, _ ->
                            // Set daily mode
                            sharedPrefs.edit {
                                putBoolean(
                                    getString(R.string.shared_prefs_app_globals_daily_mode_on),
                                    true
                                )
                            }

                            // Refresh wallpaper since daily mode is on now
                            val wallpaperRefreshIntent = Intent(
                                applicationContext,
                                BootImportServiceReceiver::class.java
                            ).apply {
                                action = ACTION_APP_REFRESH_BACKGROUND
                            }

                            sendBroadcast(wallpaperRefreshIntent)
                        }
                        setNegativeButton(R.string.activity_main_preview_wallpapers_gallery_daily_item_dialog_negative_button) { _, _ ->
                            // Do nothing, op cancelled
                        }
                    }

                    dialogBuilder.show()
                    true
                }
                else -> false
            }
        }
        val inflater = pMenu.menuInflater
        inflater.inflate(R.menu.menu_activity_main_gallery_daily_item, pMenu.menu)
        pMenu.show()
    }

    private fun requestStoragePermission(
        successAction: () -> Unit
    ) {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED -> {
                successAction()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)
            -> {
                layout.showSnackbar(
                    R.string.storage_permission_required,
                    Snackbar.LENGTH_INDEFINITE,
                    R.string.ok
                ) {
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
        wallpaperGalleryGridLayoutManager =
            GridLayoutManager(this, 1, GridLayoutManager.HORIZONTAL, false)

        val previewImage: ImageView = findViewById(R.id.activity_main_preview_wallpaper_image)
        val previewHeadlineText: MaterialTextView =
            findViewById(R.id.activity_main_preview_headline)
        val previewDetailsDateText: MaterialTextView = findViewById(R.id.activity_main_details_date)
        val previewDetailsCopyrightText: MaterialTextView =
            findViewById(R.id.activity_main_details_copyright)
        val previewDetailsCopyrightLinkText: MaterialTextView =
            findViewById(R.id.activity_main_details_copyright_link)

        wallpaperGalleryGridAdapter.dailyItemSelectedListener = this
        wallpaperGalleryGridAdapter.dailyItemLongClickListener = this
        wallpaperGalleryGridAdapter.bingImageSelectedListener = this
        wallpaperGalleryGridAdapter.bingImageLongClickListener = this
        wallpaperGalleryGridRecyclerView.layoutManager = wallpaperGalleryGridLayoutManager
        wallpaperGalleryGridRecyclerView.adapter = wallpaperGalleryGridAdapter

        mainViewModel.previewImage.observe(this) { value ->
            if (value == null) {
                previewHeadlineText.text =
                    getString(R.string.activity_main_preview_wallpapers_gallery_daily_item_headline)
                previewDetailsDateText.text =
                    getString(R.string.activity_main_preview_wallpapers_gallery_daily_item_date)
                previewDetailsCopyrightText.text = getString(R.string.na)
                previewDetailsCopyrightLinkText.text = getString(R.string.na)

                previewImage.setImageResource(R.drawable.ic_baseline_daily)
                previewImage.scaleType = ImageView.ScaleType.FIT_CENTER
            } else {
                value.let {
                    previewHeadlineText.text = value.headline
                    previewDetailsDateText.text = value.date.toString()
                    previewDetailsCopyrightText.text = value.copyright
                    previewDetailsCopyrightLinkText.text = value.copyrightLink

                    previewImage.setImageURI(Uri.parse(value.imageDeviceUri))
                    previewImage.scaleType = ImageView.ScaleType.CENTER_CROP
                }
            }
        }

        mainViewModel.galleryImages.observe(this) {list ->
            list?.let {
                wallpaperGalleryGridAdapter.bingImages = list
                wallpaperGalleryGridAdapter.notifyDataSetChanged()
                if (list.isNotEmpty()) mainViewModel.onPreviewWallpaperSelected(list[0])
            }
        }

        // Run an image scrap with the import service
        startForegroundService(Intent(this, BingImageImportService::class.java))
    }

    private fun updatePrefsIfWallpaperChanged(): Boolean {
        val recordedWallpaperId = sharedPrefs.getInt(
            getString(R.string.shared_prefs_app_globals_recorded_wallpaper_id),
            -1
        )
        val currWallpaperId = wallpaperManager.getWallpaperId(WallpaperManager.FLAG_SYSTEM)
        if (currWallpaperId != recordedWallpaperId) {
            sharedPrefs.edit {
                putInt(
                    getString(R.string.shared_prefs_app_globals_recorded_wallpaper_id),
                    currWallpaperId
                )
                putBoolean(
                    getString(R.string.shared_prefs_app_globals_daily_mode_on),
                    false
                )
            }

            return true
        }

        return false
    }
}