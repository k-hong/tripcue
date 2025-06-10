package com.example.tripcue.frame.uicomponents.home

import android.content.Context
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.rememberAsyncImagePainter
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.MapView
import com.naver.maps.map.overlay.Marker

@Composable
fun PlaceDetailScreen(place: PlaceInfo) {
    val context = LocalContext.current
    var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(place.title) {
        imageBitmap = fetchGooglePlacePhoto(place.title, context)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = place.title,
            fontSize = 24.sp,
            modifier = Modifier.padding(16.dp)
        )

        if (imageBitmap != null) {
            Image(
                bitmap = imageBitmap!!,
                contentDescription = "Google Photo",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )
        } else {
            Image(
                painter = rememberAsyncImagePainter(place.thumbnailUrl),
                contentDescription = "fallback thumbnail",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )
        }

        Text(
            text = "⭐ ${place.rating} (${place.userRatingsTotal} reviews)",
            fontSize = 14.sp,
            color = Color.Gray,
            modifier = Modifier.padding(start = 16.dp, top = 8.dp)
        )

        Text(
            text = place.description,
            fontSize = 14.sp,
            color = Color.DarkGray,
            modifier = Modifier.padding(16.dp)
        )

        Text(
            text = "#${place.searchKeyword} " + place.category.replace(">", " #"),
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Box(modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)) {
            NaverMapView(place = place, context = context)
        }
    }
}

@Composable
fun NaverMapView(place: PlaceInfo, context: Context) {
    AndroidView(
        factory = {
            val mapView = MapView(context)
            mapView.getMapAsync { naverMap ->
                val coord = LatLng(place.latitude, place.longitude)

                Log.d("PlaceDetail", "🗺 lat=${place.latitude}, lng=${place.longitude}")
                Log.d("PlaceDetail", "🏷 title=${place.title}")

                val marker = Marker().apply {
                    position = coord
                    captionText = place.title
                    map = naverMap
                }
                naverMap.moveCamera(CameraUpdate.scrollTo(coord))
            }
            mapView
        },
        modifier = Modifier.fillMaxSize()
    )
}
