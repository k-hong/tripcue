import android.content.Context
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPhotoRequest
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import kotlinx.coroutines.suspendCancellableCoroutine

suspend fun fetchGooglePlacePhoto(placeName: String, context: Context): ImageBitmap? {
    val placesClient = Places.createClient(context)

    val request = FindAutocompletePredictionsRequest.builder()
        .setQuery(placeName)
        .build()

    return suspendCancellableCoroutine { continuation ->
        placesClient.findAutocompletePredictions(request)
            .addOnSuccessListener { response ->
                val placeId = response.autocompletePredictions.firstOrNull()?.placeId
                if (placeId == null) {
                    continuation.resume(null, null)
                    return@addOnSuccessListener
                }

                val placeRequest = FetchPlaceRequest.builder(placeId, listOf(Place.Field.PHOTO_METADATAS)).build()
                placesClient.fetchPlace(placeRequest)
                    .addOnSuccessListener { placeResponse ->
                        val metadata = placeResponse.place.photoMetadatas?.firstOrNull()
                        if (metadata != null) {
                            val photoRequest = FetchPhotoRequest.builder(metadata)
                                .setMaxWidth(800).setMaxHeight(600).build()
                            placesClient.fetchPhoto(photoRequest)
                                .addOnSuccessListener { photoResponse ->
                                    val bitmap = photoResponse.bitmap
                                    continuation.resume(bitmap?.asImageBitmap(), null)
                                }
                                .addOnFailureListener { continuation.resume(null, null) }
                        } else {
                            continuation.resume(null, null)
                        }
                    }
                    .addOnFailureListener { continuation.resume(null, null) }
            }
            .addOnFailureListener { continuation.resume(null, null) }
    }
}
