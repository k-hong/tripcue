package com.example.tripcue.frame.uicomponents.home

import com.example.tripcue.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

object NaverPlaceApi {
    suspend fun searchPlaces(region: String, keywords: List<String>): List<String> {
        val client = OkHttpClient()
        val placeCount = mutableMapOf<String, Int>()

        for (keyword in keywords) {
            try {
                val query = "$region $keyword"
                val url = "https://openapi.naver.com/v1/search/local.json?query=${query}&display=10"

                val request = Request.Builder()
                    .url(url)
                    .addHeader("X-Naver-Client-Id", BuildConfig.NAVER_CLIENT_ID)
                    .addHeader("X-Naver-Client-Secret", BuildConfig.NAVER_CLIENT_SECRET)
                    .build()

                val response = withContext(Dispatchers.IO) {
                    client.newCall(request).execute()
                }

                val jsonStr = response.body?.string()
                if (!response.isSuccessful || jsonStr == null) {
                    println("네이버 API 응답 실패: ${response.code}")
                    continue
                }

                val items = JSONObject(jsonStr).getJSONArray("items")
                for (i in 0 until items.length()) {
                    val title = items.getJSONObject(i).getString("title").replace(Regex("<[^>]*>"), "")
                    placeCount[title] = placeCount.getOrDefault(title, 0) + 1
                }
            } catch (e: Exception) {
                e.printStackTrace()
                continue
            }
        }

        return placeCount.entries.sortedByDescending { it.value }.take(3).map { it.key }
    }
}
