package com.example.tripcue.frame.uicomponents.home

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun getRecommendedPlaces(region: String, interests: List<String>): List<String> {
    val query = "$region ${interests.firstOrNull() ?: "관광지"}"
    return withContext(Dispatchers.IO) {
        try {
            val response = NaverPlaceApiService.api.searchPlaces(query)
            if (response.isSuccessful) {
                val items = response.body()?.items ?: emptyList()
                items.map { it.title.replace("<b>", "").replace("</b>", "") } // 태그 제거
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
