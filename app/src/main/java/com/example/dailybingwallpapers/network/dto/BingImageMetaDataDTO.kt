package com.example.dailybingwallpapers.network.dto

import java.time.LocalDate

data class BingImageMetaDataDTO (
    val date: LocalDate,    // The date for when the image was used
    val imageUrl: String,   // The url directing to the img data
    val copyright: String,  // Copyright information of owner
    val copyrightLink: String,  // Copyright link from bing images
    val headline: String    // The associated headline or comments for the image
)