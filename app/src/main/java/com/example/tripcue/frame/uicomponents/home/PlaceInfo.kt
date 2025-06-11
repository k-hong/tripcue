package com.example.tripcue.frame.uicomponents.home

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class PlaceInfo(
    // val placeId: String? = null, // [제거] placeId 필드 제거
    val title: String, // 여기에는 '깨끗한' 원본 영문 이름이 저장됩니다.
    val koreanType: String?, // [추가] 한국어 장소 유형 (예: "쇼핑몰")
    val description: String,
    val category: String,
    val thumbnailUrl: String,
    val latitude: Double,
    val longitude: Double,
    val searchKeyword: String,
    val isDomestic: Boolean,
    val rating: Double = 0.0,
    val userRatingsTotal: Int = 0
) : Parcelable