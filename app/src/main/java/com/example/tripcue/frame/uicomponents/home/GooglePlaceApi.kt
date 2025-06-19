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
        val results = mutableListOf<PlaceInfo>()
        val seenTitles = mutableSetOf<String>()
        val perKeywordLimit = when (interests.size) {
            1 -> 15; 2 -> 8; 3 -> 7; else -> 5
        }

        val searchLambdas = mutableListOf<suspend () -> Unit>()

        // 1. 주 검색 로직
        interests.forEach { keyword ->
            searchLambdas.add {
                if (results.size >= totalLimit) return@add
                val translatedKeyword = TranslationUtils.translateToEnglish(keyword)
                val query = "$region $translatedKeyword"
                Log.d(TAG, "관심사 검색어: '$query', 국가 제한: '$countryCode'")
                val predictions = fetchPredictions(client, query, perKeywordLimit, countryCode)
                processPredictions(context, client, predictions, results, seenTitles, totalLimit, keyword, isDomestic)
            }
        }

        // 2. 예비 검색 로직
        searchLambdas.add {
            if (results.size < totalLimit) {
                Log.d(TAG, "--- 예비 검색 시작 (결과 수: ${results.size}) ---")
                val fallbackKeywords = listOf("attractions", "food", "cafe")
                for (fallbackKeyword in fallbackKeywords) {
                    if (results.size >= totalLimit) break
                    val query = "$region $fallbackKeyword"
                    Log.d(TAG, "예비 검색어: '$query', 국가 제한: '$countryCode'")
                    val predictions = fetchPredictions(client, query, 5, countryCode)
                    processPredictions(context, client, predictions, results, seenTitles, totalLimit, "추천", isDomestic)
                }
            }
        }

        // 검색 실행
        for (searchLambda in searchLambdas) {
            if (results.size >= totalLimit) break
            searchLambda()
        }

        Log.d(TAG, "--- 최종 검색 결과 (${results.size}개) ---")
        return@withContext results
    }

    private suspend fun processPredictions(
        context: Context, client: PlacesClient, predictions: List<com.google.android.libraries.places.api.model.AutocompletePrediction>,
        results: MutableList<PlaceInfo>, seenTitles: MutableSet<String>, totalLimit: Int,
        searchKeyword: String, isDomestic: Boolean
    ) {
        for (prediction in predictions) {
            if (results.size >= totalLimit) break

            val originalTitle = prediction.getPrimaryText(null).toString()
            val lowerCaseTitle = originalTitle.lowercase()

            // [추가] 1. 'tokyo' 또는 '도쿄'가 포함된 장소 제외
            if ("tokyo" in lowerCaseTitle || "도쿄" in originalTitle) {
                Log.d(TAG, "필터링: '$originalTitle' (도쿄/tokyo 포함)")
                continue
            }

            if (!isValidPlace(originalTitle) || !seenTitles.add(originalTitle)) continue

            val place = fetchPlaceDetails(client, prediction.placeId) ?: continue

            // [추가] 2. 썸네일(사진 정보)이 없는 장소 제외
            if (place.photoMetadatas.isNullOrEmpty()) {
                Log.d(TAG, "필터링: '$originalTitle' (사진 없음)")
                continue
            }

            val koreanType = PlaceTypeConverter.getKoreanType(place.types)
            val koreanAddress = place.latLng?.let { LocationUtils.getAddressFromCoordinates(context, it.latitude, it.longitude) }

            results.add(
                PlaceInfo(
                    title = originalTitle,
                    koreanType = koreanType,
                    description = koreanAddress ?: place.address ?: prediction.getSecondaryText(null).toString(),
                    category = place.types?.joinToString(" > ") ?: "기타",
                    thumbnailUrl = "HAS_PHOTO", // 사진이 있으므로 항상 "HAS_PHOTO"
                    latitude = place.latLng?.latitude ?: 0.0,
                    longitude = place.latLng?.longitude ?: 0.0,
                    searchKeyword = searchKeyword,
                    isDomestic = isDomestic
                )
            )
        }
    }

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