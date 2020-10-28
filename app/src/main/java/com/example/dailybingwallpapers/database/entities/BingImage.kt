package com.example.dailybingwallpapers.database.entities

import android.media.Image
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity
data class BingImage (
        @PrimaryKey val id: Int = 0,
        val date: Date,
        val imageUrl: String,
        val image: Image,
        val copyright: String,
        val copyrightLink: String,
        val headline: String
)