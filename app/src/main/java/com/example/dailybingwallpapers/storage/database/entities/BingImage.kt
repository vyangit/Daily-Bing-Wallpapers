package com.example.dailybingwallpapers.storage.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

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