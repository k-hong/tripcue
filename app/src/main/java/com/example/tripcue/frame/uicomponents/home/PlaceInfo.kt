package com.example.tripcue.frame.uicomponents.home

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class PlaceInfo(
    val title: String,
    val description: String,
    val category: String,
    val thumbnailUrl: String,
    val latitude: Double,
    val longitude: Double,
    val searchKeyword: String
) : Parcelable