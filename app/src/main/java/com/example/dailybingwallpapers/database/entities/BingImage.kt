package com.example.dailybingwallpapers.database.entities

import android.media.Image
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.util.*

@Entity
data class BingImage (
        @PrimaryKey(autoGenerate = true)
        val id: Int = 0,
        val date: LocalDate,
        val imageUrl: String,
        val imageDeviceUri: String,
        val copyright: String,
        val copyrightLink: String,
        val headline: String
)