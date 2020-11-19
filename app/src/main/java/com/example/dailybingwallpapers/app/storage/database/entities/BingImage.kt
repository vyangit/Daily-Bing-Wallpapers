package com.example.dailybingwallpapers.app.storage.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.TypeConverters
import com.example.dailybingwallpapers.app.storage.database.type_converters.BingImageTypeConverter
import java.time.LocalDate

@Entity(tableName = "bing_image", primaryKeys= ["date", "image_url"])
@TypeConverters(BingImageTypeConverter::class)
data class BingImage (
        @ColumnInfo(name="date")
        val date: LocalDate,

        @ColumnInfo(name="image_url")
        val imageUrl: String,

        @ColumnInfo(name="image_device_uri")
        val imageDeviceUri: String,

        val copyright: String,

        @ColumnInfo(name="copyright_link")
        val copyrightLink: String,

        val headline: String
)