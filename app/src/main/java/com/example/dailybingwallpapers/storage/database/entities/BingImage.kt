package com.example.dailybingwallpapers.storage.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.dailybingwallpapers.storage.database.type_converters.BingImageTypeConverter
import java.time.LocalDate

@Entity
@TypeConverters(BingImageTypeConverter::class)
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