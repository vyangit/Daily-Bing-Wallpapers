package com.example.dailybingwallpapers.app.adapters

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RelativeLayout
import androidx.recyclerview.widget.RecyclerView
import com.example.dailybingwallpapers.R
import com.example.dailybingwallpapers.app.storage.database.entities.BingImage
import com.google.android.material.textview.MaterialTextView

class BingImageAdapter :
    RecyclerView.Adapter<BingImageAdapter.BingImageViewHolder>() {

    var bingImages: List<BingImage> = listOf() // Images being served by adapter
    var isDailyOn: Boolean = false // Sets state for the daily rendering based on app preference
        set(isOn) {
            field = isOn
            notifyItemChanged(0)
        }
    lateinit var bingImageSelectedListener: OnBingImageSelectedListener
    lateinit var bingImageLongClickListener: OnBingImageLongClickListener
    lateinit var dailyItemSelectedListener: OnDailyItemSelectedListener
    lateinit var dailyItemLongClickListener: OnDailyItemLongClickListener

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BingImageViewHolder {
        val galleryLayout = LayoutInflater.from(parent.context)
            .inflate(R.layout.activity_main_gallery_grid_item, parent, false) as LinearLayout
        return BingImageViewHolder(galleryLayout)
    }

    override fun onBindViewHolder(holder: BingImageViewHolder, position: Int) {
        // Reset progress bar
        holder.progressBar.visibility = View.VISIBLE
        holder.activeOverlay.visibility = View.GONE
        holder.imageView.setOnLongClickListener(null)
        holder.imageView.setOnClickListener(null)

        if (position == 0) {
            holder.progressBar.visibility = View.GONE
            holder.imageView.setImageResource(R.drawable.ic_baseline_daily)
            holder.imageView.scaleType = ImageView.ScaleType.FIT_CENTER
            holder.dateText.setText(R.string.daily)

            if (!isDailyOn) {
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
                holder.activeOverlay.visibility = View.VISIBLE
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

    class BingImageViewHolder(galleryLayout: View) :
        RecyclerView.ViewHolder(galleryLayout) {
        val imageView: ImageView = galleryLayout
            .findViewById(R.id.activity_main_wallpapers_gallery_grid_item_image)
        val dateText: MaterialTextView = galleryLayout
            .findViewById(R.id.activity_main_wallpapers_gallery_grid_item_date)
        val progressBar: ProgressBar = galleryLayout
            .findViewById(R.id.activity_main_wallpapers_gallery_progress_bar)
        val activeOverlay: RelativeLayout = galleryLayout
            .findViewById(R.id.activity_main_wallpapers_gallery_grid_item_using_overlay)
    }

    /**
     * Event listener for when a click is detected on a bing image
     */
    interface OnBingImageSelectedListener {
        /**
         * Action to take when a click is detected on a bing image
         */
        fun onBingImageSelected(bingImage: BingImage)
    }

    /**
     * Event listener for when a long click is detected on a bing image
     */
    interface OnBingImageLongClickListener {
        /**
         * Action to take when a long click is detected on a bing image
         */
        fun onBingImageLongClickListener(view: View, bingImage: BingImage)
    }

    /**
     * Event listener for when the daily item rendering if selected
     */
    interface OnDailyItemSelectedListener {
        /**
         * Action to take when a click is detected on the daily item rendering
         */
        fun onDailyItemSelected()
    }

    /**
     * Event listener for when a long click is detected on the daily item rendering
     */
    interface OnDailyItemLongClickListener {
        /**
         * Action to take when a long click is detected on the daily item rendering
         */
        fun onDailyItemLongClick(view: View)
    }

}
