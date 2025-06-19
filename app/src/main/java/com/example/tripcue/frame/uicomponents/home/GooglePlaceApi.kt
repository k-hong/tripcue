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
        isDomestic: Boolean,
        countryCode: String?
    ): List<PlaceInfo> = withContext(Dispatchers.IO) {
        if (!Places.isInitialized()) {
            Places.initialize(context, BuildConfig.GOOGLE_PLACES_API_KEY)
        }
        val client = Places.createClient(context)

        // 중복 장소를 관리하기 위한 Set
        val seenTitles = mutableSetOf<String>()
        val seenCoreNames = mutableSetOf<String>()

        // [수정] 1. 관심사 기반 결과를 저장할 별도의 리스트 생성
        val interestResults = mutableListOf<PlaceInfo>()
        val perKeywordLimit = when (interests.size) {
            1 -> 15; 2 -> 8; 3 -> 7; else -> 5
        }
        val translatedRegion = TranslationUtils.translateToEnglish(region).lowercase()

        // --- 1단계: 관심사 기반 검색 ---
        Log.d(TAG, "--- 1단계: 관심사 기반 검색 시작 ---")
        for (keyword in interests) {
            if (interestResults.size >= totalLimit) break
            val translatedKeyword = InterestConverter.convertToEnglish(keyword)
            val query = "$region $translatedKeyword"
            val predictions = fetchPredictions(client, query, perKeywordLimit, countryCode)
            processPredictions(context, client, predictions, interestResults, seenTitles, seenCoreNames, totalLimit, keyword, isDomestic, region, translatedRegion)
        }

        // [수정] 2. 예비 검색 결과를 저장할 별도의 리스트 생성
        val fallbackResults = mutableListOf<PlaceInfo>()
        // --- 2단계: 예비 검색 (관심사 검색 결과가 부족할 경우) ---
        if (interestResults.size < totalLimit) {
            Log.d(TAG, "--- 2단계: 예비 검색 시작 ---")
            val fallbackKeywords = listOf("attractions", "food", "cafe")
            for (fallbackKeyword in fallbackKeywords) {
                // 전체 결과 수가 한도를 넘으면 중단
                if ((interestResults.size + fallbackResults.size) >= totalLimit) break
                val query = "$region $fallbackKeyword"
                val predictions = fetchPredictions(client, query, 5, countryCode)
                processPredictions(context, client, predictions, fallbackResults, seenTitles, seenCoreNames, totalLimit - interestResults.size, "추천", isDomestic, region, translatedRegion)
            }
        }

        Log.d(TAG, "--- 최종 결과 통합: 관심사 결과 ${interestResults.size}개, 예비 결과 ${fallbackResults.size}개 ---")
        // [수정] 3. 관심사 결과 리스트 뒤에 예비 결과 리스트를 합쳐서 최종 결과 반환
        return@withContext interestResults + fallbackResults
    }

    private suspend fun processPredictions(
        context: Context, client: PlacesClient, predictions: List<com.google.android.libraries.places.api.model.AutocompletePrediction>,
        // [수정] 결과를 담을 리스트를 파라미터로 받도록 변경
        currentResults: MutableList<PlaceInfo>,
        seenTitles: MutableSet<String>,
        seenCoreNames: MutableSet<String>,
        limit: Int, // 이 단계에서 채울 수 있는 최대 개수
        searchKeyword: String, isDomestic: Boolean,
        originalRegion: String, translatedRegion: String
    ) {
        for (prediction in predictions) {
            if (currentResults.size >= limit) break

            val originalTitle = prediction.getPrimaryText(null).toString()
            val lowerCaseTitle = originalTitle.lowercase()

            // 필터링 로직 (기존과 동일)
            if (!seenTitles.add(originalTitle)) continue
            if (originalRegion.lowercase() in lowerCaseTitle || translatedRegion in lowerCaseTitle) continue
            val coreName = originalTitle.split(" ").firstOrNull() ?: originalTitle
            if (seenCoreNames.contains(coreName)) continue
            if (!isValidPlace(originalTitle)) continue
            val place = fetchPlaceDetails(client, prediction.placeId) ?: continue
            if (place.photoMetadatas.isNullOrEmpty()) continue

            val koreanType = PlaceTypeConverter.getKoreanType(place.types)
            val koreanAddress = place.latLng?.let { LocationUtils.getAddressFromCoordinates(context, it.latitude, it.longitude) }

            currentResults.add(
                PlaceInfo(
                    title = originalTitle,
                    koreanType = koreanType,
                    description = koreanAddress ?: place.address ?: prediction.getSecondaryText(null).toString(),
                    category = place.types?.joinToString(" > ") ?: "기타",
                    thumbnailUrl = "HAS_PHOTO",
                    latitude = place.latLng?.latitude ?: 0.0,
                    longitude = place.latLng?.longitude ?: 0.0,
                    searchKeyword = searchKeyword,
                    isDomestic = isDomestic
                )
            )
            seenCoreNames.add(coreName)
        }
    }

    // ... (fetchPredictions, fetchPlaceDetails, isValidPlace 함수는 변경 없음)
    private suspend fun fetchPredictions(client: PlacesClient, query: String, limit: Int, countryCode: String?) =
        try {
            val requestBuilder = FindAutocompletePredictionsRequest.builder().setQuery(query)
            if (countryCode != null) {
                requestBuilder.setCountries(listOf(countryCode))
            }
            val request = requestBuilder.build()
            client.findAutocompletePredictions(request).await().autocompletePredictions.take(limit)
        } catch (e: Exception) {
            Log.e(TAG, " Prediction fetch error: ${e.message}", e)
            emptyList()
        }

    private suspend fun fetchPlaceDetails(client: PlacesClient, placeId: String): Place? =
        try {
            val placeFields = listOf(Place.Field.NAME, Place.Field.ADDRESS, Place.Field.TYPES, Place.Field.PHOTO_METADATAS, Place.Field.LAT_LNG)
            val request = FetchPlaceRequest.newInstance(placeId, placeFields)
            client.fetchPlace(request).await().place
        } catch (e: Exception) {
            Log.e(TAG, " Place detail fetch error: ${e.message}", e)
            null
        }

    private fun isValidPlace(title: String): Boolean {
        if (title.isBlank()) return false
        val lowerTitle = title.lowercase()
        val corporateKeywords = listOf("(주)", "주식회사", "마케팅", "플랫폼", "센터", "솔루션")
        return corporateKeywords.none { lowerTitle.contains(it) }
    }
}