package com.example.tripcue.frame.uicomponents.home

import android.content.Context
import android.util.Log
import com.example.tripcue.BuildConfig
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

object GooglePlaceApi {
    private const val TAG = "GooglePlaceApi"

    suspend fun advancedSearchPlaces(
        context: Context,
        region: String,
        interests: List<String>,
        totalLimit: Int = 15
    ): List<PlaceInfo> = withContext(Dispatchers.IO) {
        if (!Places.isInitialized()) {
            Places.initialize(context, BuildConfig.GOOGLE_PLACES_API_KEY)
        }

        val client = Places.createClient(context)
        val results = mutableListOf<PlaceInfo>()
        val seenTitles = mutableSetOf<String>()

        val interestsCount = interests.size
        val perKeywordLimit = when (interestsCount) {
            1 -> 15
            2 -> 7
            3 -> 5
            else -> 3
        }

        // 키워드 검색
        for (keyword in interests) {
            val query = "$region $keyword 여행"
            val predictions = fetchPredictions(client, query, perKeywordLimit)
            for (prediction in predictions) {
                val place = fetchPlaceDetails(client, prediction.placeId) ?: continue
                if (!isValidPlace(prediction.getPrimaryText(null).toString(), prediction.getSecondaryText(null).toString(), place)) continue

                val title = prediction.getPrimaryText(null).toString()
                if (seenTitles.add(title)) {
                    results.add(
                        PlaceInfo(
                            title = title,
                            description = prediction.getSecondaryText(null).toString(),
                            category = place.types?.joinToString(" > ") ?: "",
                            thumbnailUrl = if (place.photoMetadatas != null) "HAS_PHOTO" else "https://via.placeholder.com/150",
                            latitude = place.latLng?.latitude ?: 0.0,
                            longitude = place.latLng?.longitude ?: 0.0,
                            searchKeyword = keyword
                        )
                    )
                }
                if (results.size >= totalLimit) break
            }
        }

        // fallback 검색
        val fallbackQueries = listOf("$region 여행지", "$region 맛집", "$region 여행")
        for (fallback in fallbackQueries) {
            if (results.size >= totalLimit) break
            val predictions = fetchPredictions(client, fallback, 5)
            for (prediction in predictions) {
                val place = fetchPlaceDetails(client, prediction.placeId) ?: continue
                if (!isValidPlace(prediction.getPrimaryText(null).toString(), prediction.getSecondaryText(null).toString(), place)) continue

                val title = prediction.getPrimaryText(null).toString()
                if (seenTitles.add(title)) {
                    results.add(
                        PlaceInfo(
                            title = title,
                            description = prediction.getSecondaryText(null).toString(),
                            category = place.types?.joinToString(" > ") ?: "",
                            thumbnailUrl = if (place.photoMetadatas != null) "HAS_PHOTO" else "https://via.placeholder.com/150",
                            latitude = place.latLng?.latitude ?: 0.0,
                            longitude = place.latLng?.longitude ?: 0.0,
                            searchKeyword = fallback
                        )
                    )
                }
                if (results.size >= totalLimit) break
            }
        }

        return@withContext results
    }

    private suspend fun fetchPredictions(client: com.google.android.libraries.places.api.net.PlacesClient, query: String, limit: Int) =
        try {
            val request = FindAutocompletePredictionsRequest.builder()
                .setQuery(query)
                .build()
            client.findAutocompletePredictions(request).await().autocompletePredictions.take(limit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Prediction fetch error: ${e.message}", e)
            emptyList()
        }

    private suspend fun fetchPlaceDetails(client: com.google.android.libraries.places.api.net.PlacesClient, placeId: String): Place? =
        try {
            val placeFields = listOf(
                Place.Field.NAME,
                Place.Field.ADDRESS,
                Place.Field.TYPES,
                Place.Field.PHOTO_METADATAS,
                Place.Field.RATING,
                Place.Field.USER_RATINGS_TOTAL,
                Place.Field.LAT_LNG
            )
            val request = FetchPlaceRequest.newInstance(placeId, placeFields)
            client.fetchPlace(request).await().place
        } catch (e: Exception) {
            Log.e(TAG, "❌ Place detail fetch error: ${e.message}", e)
            null
        }

    private fun isValidPlace(title: String, description: String, place: Place): Boolean {
        val lowerTitle = title.lowercase()
        val corporateKeywords = listOf("(주)", "주식회사", "쇼핑", "마케팅", "플랫폼", "센터", "솔루션")
        val isCorporate = corporateKeywords.any { lowerTitle.contains(it.lowercase()) }
        val isTooGenericTitle = title.matches(Regex(".*여행.*")) && title.length <= 5
        val isDescriptionTooShort = description.trim().split(" ").size < 2
        val hasPhoto = place.photoMetadatas != null
        val hasEnoughReviews = (place.userRatingsTotal ?: 0) >= 2
        return !isCorporate && !isTooGenericTitle && !isDescriptionTooShort && hasPhoto && hasEnoughReviews
    }
}