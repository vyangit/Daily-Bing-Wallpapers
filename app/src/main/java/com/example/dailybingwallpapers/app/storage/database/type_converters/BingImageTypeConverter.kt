package com.example.dailybingwallpapers.app.storage.database.type_converters

import androidx.room.TypeConverter
import java.time.LocalDate

class BingImageTypeConverter{
    @TypeConverter
    fun localDateToString(localDate: LocalDate): String {
        return localDate.toString()
    }

    @TypeConverter
    fun toLocalDate(localDateString: String): LocalDate {
        return LocalDate.parse(localDateString)
    }
}