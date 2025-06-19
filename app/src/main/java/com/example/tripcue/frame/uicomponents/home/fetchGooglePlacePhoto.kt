package com.example.tripcue.frame.uicomponents.home


import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPhotoRequest
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

suspend fun fetchGooglePlacePhoto(placeName: String, context: Context): ImageBitmap? {
    val TAG = "GooglePlaceDebug"

    if (!Places.isInitialized()) {
        Log.e(TAG, " Places API not initialized.")
        return null
    }

    Log.d(TAG, " Searching for place: $placeName")

    val placesClient = Places.createClient(context)

    val request = FindAutocompletePredictionsRequest.builder()
        .setQuery(placeName)
        .build()

    return suspendCancellableCoroutine { continuation ->
        placesClient.findAutocompletePredictions(request)
            .addOnSuccessListener { response ->
                val prediction = response.autocompletePredictions.firstOrNull()
                if (prediction == null) {
                    Log.w(TAG, "No autocomplete prediction found.")
                    continuation.resume(null)
                    return@addOnSuccessListener
                }

                val placeId = prediction.placeId
                Log.d(TAG, "Found placeId: $placeId")

                val placeRequest = FetchPlaceRequest.builder(placeId, listOf(Place.Field.PHOTO_METADATAS)).build()
                placesClient.fetchPlace(placeRequest)
                    .addOnSuccessListener { placeResponse ->
                        val metadata = placeResponse.place.photoMetadatas?.firstOrNull()
                        if (metadata == null) {
                            Log.w(TAG, " No photo metadata found.")
                            continuation.resume(null)
                            return@addOnSuccessListener
                        }

                        Log.d(TAG, " Found photo metadata, fetching photo...")

                        val photoRequest = FetchPhotoRequest.builder(metadata)
                            .setMaxWidth(800)
                            .setMaxHeight(600)
                            .build()

                        placesClient.fetchPhoto(photoRequest)
                            .addOnSuccessListener { photoResponse ->
                                val bitmap: Bitmap? = photoResponse.bitmap
                                if (bitmap != null) {
                                    Log.d(TAG, " Bitmap loaded successfully.")
                                    continuation.resume(bitmap.asImageBitmap())
                                } else {
                                    Log.e(TAG, " Bitmap was null.")
                                    continuation.resume(null)
                                }
                            }
                            .addOnFailureListener { e ->
                                Log.e(TAG, " Failed to fetch photo: ${e.message}", e)
                                continuation.resume(null)
                            }
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, " Failed to fetch place: ${e.message}", e)
                        continuation.resume(null)
                    }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, " Autocomplete failed: ${e.message}", e)
                continuation.resume(null)
            }
    }
}