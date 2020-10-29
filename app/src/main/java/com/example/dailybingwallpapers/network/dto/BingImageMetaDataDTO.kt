package com.example.dailybingwallpapers.network.dto

import java.time.LocalDate

data class BingImageMetaDataDTO (
    val date: LocalDate,
    val imageUrl: String,
    val copyright: String,
    val copyrightLink: String,
    val headline: String
)