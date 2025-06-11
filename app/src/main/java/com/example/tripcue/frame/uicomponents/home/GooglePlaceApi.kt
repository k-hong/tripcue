package com.example.tripcue.frame.uicomponents.home

import android.content.Context
import android.util.Log
import com.example.tripcue.BuildConfig
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

object GooglePlaceApi {
    private const val TAG = "GooglePlaceApi"

    suspend fun advancedSearchPlaces(
        context: Context,
        region: String,
        interests: List<String>,
        totalLimit: Int = 15,
        isDomestic: Boolean // [추가]

    ): List<PlaceInfo> = withContext(Dispatchers.IO) {
        // --- 초기 설정 ---
        if (!Places.isInitialized()) {
            Places.initialize(context, BuildConfig.GOOGLE_PLACES_API_KEY)
        }
        val client = Places.createClient(context)
        val results = mutableListOf<PlaceInfo>()
        val seenTitles = mutableSetOf<String>()

        val perKeywordLimit = when (interests.size) {
            1 -> 15
            2 -> 8
            3 -> 7
            else -> 5
        }

        // --- 1. 관심사 키워드 번역 검색 (핵심 로직) ---
        Log.d(TAG, "--- 관심사 기반 검색 시작 ---")
        for (keyword in interests) {
            if (results.size >= totalLimit) break

            val translatedKeyword = TranslationUtils.translateToEnglish(keyword)
            val query = "$region $translatedKeyword"

            Log.d(TAG, "관심사 검색어: '$query'")
            val predictions = fetchPredictions(client, query, perKeywordLimit)

            for (prediction in predictions) {
                if (results.size >= totalLimit) break

                val placeId = prediction.placeId
                val originalTitle = prediction.getPrimaryText(null).toString()

                // [개선] 최소한의 필터로 품질 관리
                if (!isValidPlace(originalTitle)) continue

                if (seenTitles.add(originalTitle)) {
                    val place = fetchPlaceDetails(client, placeId) ?: continue

                    val koreanType = PlaceTypeConverter.getKoreanType(place.types)
                    val koreanAddress = place.latLng?.let {
                        LocationUtils.getAddressFromCoordinates(context, it.latitude, it.longitude)
                    }

                    results.add(
                        PlaceInfo(
                            title = originalTitle,
                            koreanType = koreanType,
                            description = koreanAddress ?: place.address ?: prediction.getSecondaryText(null).toString() ?: "주소 정보 없음",
                            category = place.types?.joinToString(" > ") ?: "기타",
                            thumbnailUrl = if (place.photoMetadatas?.isNotEmpty() == true) "HAS_PHOTO" else "NO_PHOTO",
                            latitude = place.latLng?.latitude ?: 0.0,
                            longitude = place.latLng?.longitude ?: 0.0,
                            searchKeyword = keyword,
                                    isDomestic = isDomestic
                        )
                    )
                }
            }
        }

        // --- 2. 예비 검색 (결과가 부족할 경우) ---
        if (results.size < totalLimit) {
            Log.d(TAG, "--- 예비 검색 시작 (결과 수: ${results.size}) ---")
            val fallbackKeywords = listOf("attractions", "food", "cafe") // 해외에서 잘 통용되는 일반 키워드
            for (fallbackKeyword in fallbackKeywords) {
                if (results.size >= totalLimit) break

                val query = "$region $fallbackKeyword"
                Log.d(TAG, "예비 검색어: '$query'")
                val predictions = fetchPredictions(client, query, 5)

                for (prediction in predictions) {
                    if (results.size >= totalLimit) break

                    val placeId = prediction.placeId
                    val originalTitle = prediction.getPrimaryText(null).toString()

                    if (!isValidPlace(originalTitle)) continue
                    if (seenTitles.add(originalTitle)) {
                        val place = fetchPlaceDetails(client, placeId) ?: continue
                        val koreanType = PlaceTypeConverter.getKoreanType(place.types)
                        val koreanAddress = place.latLng?.let {
                            LocationUtils.getAddressFromCoordinates(context, it.latitude, it.longitude)
                        }

                        results.add(
                            PlaceInfo(
                                title = originalTitle,
                                koreanType = koreanType,
                                description = koreanAddress ?: place.address ?: prediction.getSecondaryText(null).toString() ?: "주소 정보 없음",
                                category = place.types?.joinToString(" > ") ?: "기타",
                                thumbnailUrl = if (place.photoMetadatas?.isNotEmpty() == true) "HAS_PHOTO" else "NO_PHOTO",
                                latitude = place.latLng?.latitude ?: 0.0,
                                longitude = place.latLng?.longitude ?: 0.0,
                                searchKeyword = "추천", // 예비 검색어로 찾았음을 표시
                                isDomestic = isDomestic // [추가] isDomestic 값 저장

                            )
                        )
                    }
                }
            }
        }

        Log.d(TAG, "--- 최종 검색 결과 (${results.size}개) ---")
        return@withContext results
    }

    // --- Helper Functions ---

    private suspend fun fetchPredictions(client: PlacesClient, query: String, limit: Int) =
        try {
            val request = FindAutocompletePredictionsRequest.builder().setQuery(query).build()
            client.findAutocompletePredictions(request).await().autocompletePredictions.take(limit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Prediction fetch error: ${e.message}", e)
            emptyList()
        }

    private suspend fun fetchPlaceDetails(client: PlacesClient, placeId: String): Place? =
        try {
            val placeFields = listOf(Place.Field.NAME, Place.Field.ADDRESS, Place.Field.TYPES,
                Place.Field.PHOTO_METADATAS, Place.Field.LAT_LNG)
            val request = FetchPlaceRequest.newInstance(placeId, placeFields)
            client.fetchPlace(request).await().place
        } catch (e: Exception) {
            Log.e(TAG, "❌ Place detail fetch error: ${e.message}", e)
            null
        }

    // [개선] 최소한의 필터링: 제목이 비어있지 않은지만 확인
    private fun isValidPlace(title: String): Boolean {
        // 1. 제목이 비어있는지 먼저 확인 (기존 유지)
        if (title.isBlank()) {
            return false
        }

        // 2. 요청하신 회사/기관 관련 키워드 필터링 로직 추가
        val lowerTitle = title.lowercase()
        val corporateKeywords = listOf("(주)", "주식회사", "마케팅", "플랫폼", "센터", "솔루션")
        val isCorporate = corporateKeywords.any { lowerTitle.contains(it) }

        // isCorporate가 true이면(키워드가 포함되어 있으면) false를 반환하여 필터링
        return !isCorporate
    }

}