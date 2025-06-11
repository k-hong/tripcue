package com.example.tripcue.frame.uicomponents.home

import android.content.Context
import android.util.Log
import com.example.tripcue.BuildConfig
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.gson.JsonParser
import com.naver.maps.geometry.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder

object NaverPlaceApi {
    // --- 키 정의 ---
    // [기존] Naver OpenAPI (지역 검색용)
    private const val OpenAPI_CLIENT_ID = BuildConfig.NAVER_D_CLIENT_ID
    private const val OpenAPI_CLIENT_SECRET = BuildConfig.NAVER_D_CLIENT_SECRET

    // [수정] Naver Cloud Platform (지도/지오코딩용)
    private const val NCP_CLIENT_ID = BuildConfig.NAVER_CLIENT_ID
    private const val NCP_CLIENT_SECRET = BuildConfig.NAVER_CLIENT_SECRET

    // --- URL 정의 ---
    private const val NAVER_SEARCH_URL = "https://openapi.naver.com/v1/search/local.json"
    private const val NCP_GEOCODE_URL = "https://naveropenapi.apigw.ntruss.com/map-geocode/v2/geocode"

    private val client = OkHttpClient()
    private const val TAG = "NaverPlaceApi"

    suspend fun advancedSearchPlaces(
        context: Context, // [추가] 구글 API 사용을 위해 Context 필요
        region: String,
        interests: List<String>,
        totalLimit: Int = 15
    ): List<PlaceInfo> = withContext(Dispatchers.IO) {
        val results = mutableListOf<PlaceInfo>()
        val seenTitles = mutableSetOf<String>()

        val perKeywordLimit = when (interests.size) {
            1 -> 15
            2 -> 8
            3 -> 7
            else -> 5
        }

        // 1. 관심사 키워드로 검색
        for (keyword in interests) {
            if (results.size >= totalLimit) break
            val query = "$region $keyword 장소"
            val places = searchNaverLocal(context, query, "comment", keyword) // context 전달
            for (place in places.take(perKeywordLimit)) {
                if (results.size >= totalLimit) break
                if (seenTitles.add(place.title)) {
                    results.add(place)
                }
            }
        }

        // 2. 예비 검색
        if (results.size < totalLimit) {
            val fallbackQueries = listOf("$region 여행","$region 맛집", "$region 명소")
            for (query in fallbackQueries) {
                if (results.size >= totalLimit) break
                val places = searchNaverLocal(context, query, "comment", "추천")
                for (place in places) {
                    if (results.size >= totalLimit) break
                    if (seenTitles.add(place.title)) {
                        results.add(place)
                    }
                }
            }
        }

        return@withContext results
    }

    private fun searchNaverLocal(
        context: Context,
        query: String,
        sortBy: String,
        keyword: String
    ): List<PlaceInfo> {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val url = "$NAVER_SEARCH_URL?query=$encodedQuery&display=15&sort=$sortBy"
        val request = Request.Builder()
            .url(url)
            .addHeader("X-Naver-Client-Id", OpenAPI_CLIENT_ID)
            .addHeader("X-Naver-Client-Secret", OpenAPI_CLIENT_SECRET)
            .build()

        return try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: return emptyList()
                val json = JsonParser.parseString(body).asJsonObject
                val items = json.getAsJsonArray("items")

                items.mapNotNull {
                    val obj = it.asJsonObject
                    val title = obj["title"]?.asString?.replace(Regex("<.*?>"), "") ?: ""
                    if (title.isBlank()) return@mapNotNull null

                    // [핵심 로직] 네이버 결과로 구글 placeId 찾기
                    //val googlePlaceId = findGooglePlaceId(context, title)

                    PlaceInfo(
                        title = title,
                        koreanType = null,
                        description = obj["roadAddress"]?.asString?.ifBlank { obj["address"]?.asString } ?: "주소 정보 없음",
                        category = obj["category"]?.asString ?: "기타",
                        thumbnailUrl = "HAS_PHOTO",
                        latitude = obj["mapy"]?.asString?.toDoubleOrNull()?.div(10_000_000) ?: 0.0,
                        longitude = obj["mapx"]?.asString?.toDoubleOrNull()?.div(10_000_000) ?: 0.0,
                        searchKeyword = keyword,
                        isDomestic = true
                    )
                }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ 네이버 API 실패", e)
            emptyList()
        }
    }

    private suspend fun findGooglePlaceId(context: Context, placeName: String): String? {
        if (!Places.isInitialized()) {
            Places.initialize(context, BuildConfig.GOOGLE_PLACES_API_KEY)
        }
        val placesClient = Places.createClient(context)
        val request = FindAutocompletePredictionsRequest.builder().setQuery(placeName).build()

        return try {
            val response = placesClient.findAutocompletePredictions(request).await()
            val prediction = response.autocompletePredictions.firstOrNull()
            prediction?.placeId
        } catch (e: Exception) {
            Log.e(TAG, "구글 placeId 검색 실패: ${e.message}")
            null
        }
    }

    suspend fun getCoordinatesForAddress(query: String): LatLng? = withContext(Dispatchers.IO) {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val url = "$NCP_GEOCODE_URL?query=$encodedQuery"
        val request = Request.Builder()
            .url(url)
            .addHeader("X-NCP-APIGW-API-KEY-ID", NCP_CLIENT_ID)
            .addHeader("X-NCP-APIGW-API-KEY", NCP_CLIENT_SECRET)
            .build()

        try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: return@withContext null
                val json = JsonParser.parseString(body).asJsonObject
                val addresses = json.getAsJsonArray("addresses")
                if (addresses.size() > 0) {
                    val firstAddress = addresses[0].asJsonObject
                    val lat = firstAddress["y"].asString.toDouble()
                    val lng = firstAddress["x"].asString.toDouble()
                    Log.d(TAG, "네이버 지오코딩 성공: '$query' -> ($lat, $lng)")
                    return@withContext LatLng(lat, lng)
                }
            } else {
                Log.e(TAG, "네이버 지오코딩 응답 실패: ${response.code} - ${response.message}")
            }
        } catch(e: Exception) {
            Log.e(TAG, "네이버 지오코딩 실패", e)
        }
        return@withContext null
    }
}