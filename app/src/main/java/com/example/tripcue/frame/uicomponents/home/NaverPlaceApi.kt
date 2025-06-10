package com.example.tripcue.frame.uicomponents.home

import android.util.Log
import com.example.tripcue.BuildConfig
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

object NaverPlaceApi {
    private const val CLIENT_ID = BuildConfig.NAVER_D_CLIENT_ID
    private const val CLIENT_SECRET = BuildConfig.NAVER_D_CLIENT_SECRET
    private const val NAVER_SEARCH_URL = "https://openapi.naver.com/v1/search/local.json"
    private val client = OkHttpClient()

    suspend fun advancedSearchPlaces(
        region: String,
        interests: List<String>,
        totalLimit: Int = 15
    ): List<PlaceInfo> = withContext(Dispatchers.IO) {
        val results = mutableListOf<PlaceInfo>()
        val seenTitles = mutableSetOf<String>()

        val interestsCount = interests.size
        val perKeywordLimit = when (interestsCount) {
            1 -> 15
            2 -> 7
            3 -> 5
            else -> 3
        }

        for (keyword in interests) {
            val query = "$region $keyword 여행"
            val places = searchNaverLocal(client, query, sortBy = "comment", keyword)
                .take(perKeywordLimit)
            for (place in places) {
                if (results.size >= totalLimit) break
                if (seenTitles.add(place.title)) {
                    results.add(place)
                }
            }
        }

        val fallbackQueries = listOf("$region 여행지", "$region 맛집", "$region 여행")
        for (query in fallbackQueries) {
            if (results.size >= totalLimit) break
            val places = searchNaverLocal(client, query, sortBy = "comment", query)
            for (place in places) {
                if (results.size >= totalLimit) break
                if (seenTitles.add(place.title)) {
                    results.add(place)
                }
            }
        }

        return@withContext results
    }


    private fun searchNaverLocal(
        client: OkHttpClient,
        query: String,
        sortBy: String,
        keyword: String
    ): List<PlaceInfo> {
        val url = "$NAVER_SEARCH_URL?query=${query}&display=15&sort=$sortBy"
        val request = Request.Builder()
            .url(url)
            .addHeader("X-Naver-Client-Id", CLIENT_ID)
            .addHeader("X-Naver-Client-Secret", CLIENT_SECRET)
            .build()

        return try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: return emptyList()
                val json = JsonParser.parseString(body).asJsonObject
                val items = json.getAsJsonArray("items")

                items.mapNotNull {
                    val obj = it.asJsonObject
                    try {
                        PlaceInfo(
                            title = obj["title"]?.asString?.replace(Regex("<.*?>"), "") ?: "",
                            description = obj["description"]?.asString?.replace(Regex("<.*?>"), "") ?: "",
                            category = obj["category"]?.asString ?: "",
                            thumbnailUrl = obj["image"]?.asString?.takeIf { it.isNotBlank() } ?: "https://via.placeholder.com/150",
                            latitude = obj["mapy"]?.asDouble ?: 0.0,
                            longitude = obj["mapx"]?.asDouble ?: 0.0,
                            searchKeyword = keyword
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
            } else {
                Log.e("TripcueLog", "❌ 응답 실패: ${response.code}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("TripcueLog", "❌ 네이버 API 실패", e)
            emptyList()
        }
    }
}