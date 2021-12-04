package com.example.dailybingwallpapers.app.activities

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.Toast
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
import com.example.dailybingwallpapers.app.receivers.DailyWallpaperRefreshReceiver
import com.example.dailybingwallpapers.app.receivers.ImportServiceReceiver
import com.example.dailybingwallpapers.app.receivers.ImportServiceReceiver.Companion.ACTION_APP_REFRESH_BACKGROUND
import com.example.dailybingwallpapers.app.storage.database.AppDatabase
import com.example.dailybingwallpapers.app.storage.database.entities.BingImage
import com.example.dailybingwallpapers.app.storage.database.repos.BingImageRepository
import com.example.dailybingwallpapers.app.utils.PreferencesUtil
import com.example.dailybingwallpapers.app.view_models.MainViewModel
import com.example.dailybingwallpapers.network.BingImageApiNetwork
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textview.MaterialTextView

const val PERMISSION_REQUEST_READ_STORAGE = 0

class MainActivity : AppCompatActivity(),
    BingImageAdapter.OnBingImageSelectedListener,
    BingImageAdapter.OnBingImageLongClickListener,
    BingImageAdapter.OnDailyItemSelectedListener,
    BingImageAdapter.OnDailyItemLongClickListener,
    SharedPreferences.OnSharedPreferenceChangeListener {

    private lateinit var sharedPrefs: SharedPreferences
    private lateinit var mainViewModel: MainViewModel
    private lateinit var refreshIntent: Intent
    private lateinit var wallpaperRefreshReceiver: DailyWallpaperRefreshReceiver

    private lateinit var layout: View
    private lateinit var wallpaperGalleryGridRecyclerView: RecyclerView
    private lateinit var wallpaperGalleryGridAdapter: BingImageAdapter
    private lateinit var wallpaperGalleryGridLayoutManager: RecyclerView.LayoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Set up variables
        layout = findViewById(R.id.activity_main)
        sharedPrefs = getSharedPreferences(
            getString(R.string.shared_prefs_app_globals_file_key),
            Context.MODE_PRIVATE
        )
        refreshIntent = Intent(
            applicationContext,
            ImportServiceReceiver::class.java
        ).apply {
            action = ACTION_APP_REFRESH_BACKGROUND
        }
        wallpaperRefreshReceiver = object : DailyWallpaperRefreshReceiver() {
            override fun receiveResponse() {
                Toast.makeText(
                    applicationContext,
                    "Daily wallpaper successfully changed",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        sharedPrefs.registerOnSharedPreferenceChangeListener(this)

        // Register receiver
        val filter = IntentFilter().apply {
            addAction(DailyWallpaperRefreshReceiver.ACTION_APP_DAILY_WALLPAPER_REFRESHED)
        }
        registerReceiver(wallpaperRefreshReceiver, filter)

        // Set up toolbar
        setSupportActionBar(findViewById(R.id.activity_main_toolbar))
    }

    override fun onResume() {
        super.onResume()

        requestStoragePermission {
            initExternalStorageViews()

            sendBroadcast(refreshIntent)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_app_bar, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_app_bar_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
            }
        }

        return true
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

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == getString(R.string.shared_prefs_app_globals_daily_mode_on)) {
            val isDailyOn = sharedPreferences?.getBoolean(key, false) ?: false
            wallpaperGalleryGridAdapter.isDailyOn = isDailyOn
        }
    }

    override fun onBingImageSelected(bingImage: BingImage) {
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

                            sendBroadcast(refreshIntent)
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
        val network = BingImageApiNetwork.getService()
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
        wallpaperGalleryGridAdapter.isDailyOn = PreferencesUtil.isDailyModeOn(this)

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
    }
}