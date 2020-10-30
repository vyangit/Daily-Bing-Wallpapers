package com.example.dailybingwallpapers.adapters

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.example.dailybingwallpapers.R
import com.example.dailybingwallpapers.storage.database.entities.BingImage
import com.google.android.material.textview.MaterialTextView

class BingImageAdapter: RecyclerView.Adapter<BingImageAdapter.BingImageViewHolder>() {

    private var _bingImages: List<BingImage> = listOf()
    var bingImages: List<BingImage>
        get() = _bingImages
        set(list) {
            _bingImages = list
        }

    class BingImageViewHolder(
        val imageView: ImageView,
        val dateText: MaterialTextView): RecyclerView.ViewHolder(imageView) {
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BingImageViewHolder {
        val galleryLayout = LayoutInflater.from(parent.context)
            .inflate(R.layout.activity_main_wallpaper_gallery_grid_item, parent, false) as ConstraintLayout
        val galleryGridItemImageView = galleryLayout
            .findViewById<ImageView>(R.id.activity_main_wallpapers_gallery_grid_item_image)
        val galleryGridItemDateText = galleryLayout
            .findViewById<MaterialTextView>(R.id.activity_main_wallpapers_gallery_grid_item_date)

        return BingImageViewHolder(galleryGridItemImageView, galleryGridItemDateText)
    }

    override fun onBindViewHolder(holder: BingImageViewHolder, position: Int) {
        val bingImage = bingImages[position]
        holder.imageView.setImageURI(Uri.parse(bingImage.imageDeviceUri))
        holder.dateText.text = bingImage.date.toString()
    }

    override fun getItemCount(): Int {
        return bingImages.size
    }

}
