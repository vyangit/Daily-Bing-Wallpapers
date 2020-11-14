package com.example.dailybingwallpapers.adapters

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import com.example.dailybingwallpapers.R
import com.example.dailybingwallpapers.storage.database.entities.BingImage
import com.google.android.material.textview.MaterialTextView

class BingImageAdapter :
    RecyclerView.Adapter<BingImageAdapter.BingImageViewHolder>() {

    interface OnBingImageSelectedListener {
        fun onBingImageSelected(bingImage: BingImage)
    }

    interface OnBingImageLongClickListener {
        fun onBingImageLongClickListener(view: View, bingImage: BingImage)
    }

    interface OnDailyItemSelectedListener {
        fun onDailyItemSelected()
    }

    interface OnDailyItemLongClickListener {
        fun onDailyItemLongClick(view: View)
    }


    var bingImages: List<BingImage> = listOf()
    lateinit var bingImageSelectedListener: OnBingImageSelectedListener
    lateinit var bingImageLongClickListener: OnBingImageLongClickListener
    lateinit var dailyItemSelectedListener: OnDailyItemSelectedListener
    lateinit var dailyItemLongClickListener: OnDailyItemLongClickListener

    class BingImageViewHolder(private val galleryLayout: View) :
        RecyclerView.ViewHolder(galleryLayout) {
        val imageView: ImageView = galleryLayout
            .findViewById(R.id.activity_main_wallpapers_gallery_grid_item_image)
        val dateText: MaterialTextView = galleryLayout
            .findViewById(R.id.activity_main_wallpapers_gallery_grid_item_date)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BingImageViewHolder {
        val galleryLayout = LayoutInflater.from(parent.context)
            .inflate(R.layout.activity_main_gallery_grid_item, parent, false) as LinearLayout
        return BingImageViewHolder(galleryLayout)
    }

    override fun onBindViewHolder(holder: BingImageViewHolder, position: Int) {
        if (position == 0) {
            holder.imageView.setImageResource(R.drawable.ic_baseline_daily)
            holder.imageView.scaleType = ImageView.ScaleType.FIT_CENTER
            holder.dateText.setText(R.string.daily)

            if (this::dailyItemSelectedListener.isInitialized) {
                holder.imageView.setOnClickListener {
                    dailyItemSelectedListener.onDailyItemSelected()
                }
            }

            if (this::dailyItemLongClickListener.isInitialized) {
                holder.imageView.setOnLongClickListener { view ->
                    dailyItemLongClickListener.onDailyItemLongClick(view)
                    true
                }
            }
        } else {
            val bingImage = bingImages[position - 1]
            holder.imageView.setImageURI(Uri.parse(bingImage.imageDeviceUri))
            holder.imageView.scaleType = ImageView.ScaleType.CENTER_CROP

            if (this::bingImageSelectedListener.isInitialized) {
                holder.imageView.setOnClickListener {
                    bingImageSelectedListener.onBingImageSelected(bingImage)
                }
            }

            if (this::bingImageLongClickListener.isInitialized) {
                holder.imageView.setOnLongClickListener { view ->
                    bingImageLongClickListener.onBingImageLongClickListener(view, bingImage)
                    true
                }
            }

            holder.dateText.text = bingImage.date.toString()
        }
    }

    override fun getItemCount(): Int {
        return bingImages.size + 1
    }

}
