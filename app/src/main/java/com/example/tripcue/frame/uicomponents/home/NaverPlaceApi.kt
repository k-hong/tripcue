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
    private fun isValidPlace(title: String): Boolean {
        if (title.isBlank()) return false
        val lowerTitle = title.lowercase()
        val corporateKeywords = listOf("(주)", "주식회사", "마케팅", "플랫폼", "센터", "솔루션")
        return corporateKeywords.none { lowerTitle.contains(it) }
    }
    suspend fun advancedSearchPlaces(
        context: Context, // [추가] 구글 API 사용을 위해 Context 필요
        region: String,
        interests: List<String>,
        totalLimit: Int = 15
    ): List<PlaceInfo> = withContext(Dispatchers.IO) {
        val results = mutableListOf<PlaceInfo>()
        val seenTitles = mutableSetOf<String>()
        val seenCoreNames = mutableSetOf<String>()
        val lowerCaseRegion = region.lowercase()

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
            // 네이버 API에서 제공하는 최대치(display=15)의 후보군을 가져옵니다.
            val places = searchNaverLocal(context, query, "comment", keyword)

            // [수정] 필터링 로직 강화
            for (place in places) {
                if (results.size >= totalLimit) break

                val originalTitle = place.title
                val lowerCaseTitle = originalTitle.lowercase()

                // --- GooglePlaceApi와 동일한 필터링 적용 ---
                // 필터 1: 전체 이름이 완전히 동일한 장소 제외
                if (!seenTitles.add(originalTitle)) continue

                // 필터 2: 장소 이름에 검색 지역명이 포함된 경우 제외 (예: '서울' 검색 시 '서울식당' 제외)
                if (lowerCaseRegion in lowerCaseTitle) continue

                // 필터 3: 장소 이름의 첫 단어가 중복인 경우 제외 (핵심 이름 중복 방지)
                val coreName = originalTitle.split(" ").firstOrNull() ?: originalTitle
                if (seenCoreNames.contains(coreName)) continue

                // 필터 4: 유효하지 않은 장소(회사, 솔루션 등) 제외
                if (!isValidPlace(originalTitle)) continue
                val imageBitmap = fetchGooglePlacePhoto(place.title, context)
                if (imageBitmap == null) {
                    Log.w(TAG, "Google Place에서 '${place.title}'의 사진을 찾지 못해 목록에서 제외합니다.")
                    continue // 사진이 없으면 다음 후보로 넘어감
                }

                results.add(place)
                seenCoreNames.add(coreName) // 중복 체크를 위해 추가
            }
        }

        // 2. 예비 검색
        if (results.size < totalLimit) {
            val fallbackQueries = listOf("$region 여행", "$region 맛집", "$region 명소")
            for (query in fallbackQueries) {
                if (results.size >= totalLimit) break
                val places = searchNaverLocal(context, query, "comment", "추천")

                // [수정] 예비 검색에도 동일한 필터링 로직 적용
                for (place in places) {
                    if (results.size >= totalLimit) break

                    val originalTitle = place.title
                    val lowerCaseTitle = originalTitle.lowercase()

                    if (!seenTitles.add(originalTitle)) continue
                    if (lowerCaseRegion in lowerCaseTitle) continue

                    val coreName = originalTitle.split(" ").firstOrNull() ?: originalTitle
                    if (seenCoreNames.contains(coreName)) continue

                    if (!isValidPlace(originalTitle)) continue
                    val imageBitmap = fetchGooglePlacePhoto(place.title, context)
                    if (imageBitmap == null) {
                        continue
                    }
                    results.add(place)
                    seenCoreNames.add(coreName)
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
                        thumbnailUrl ="HAS_PHOTO",
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
            Log.e(TAG, " 네이버 API 실패", e)
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
            throw e
        }
        return@withContext null
    }
}