package com.example.tripcue.frame.uicomponents.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.viewinterop.AndroidView
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.naver.maps.map.MapView

// [최종] 지도만 보여주는 새로운 화면
@Composable
fun MapScreen(lat: Double, lng: Double, title: String, isDomestic: Boolean) {
    if (isDomestic) {
        // 국내일 경우 Naver 지도 표시
        NaverMapView(lat = lat, lng = lng, title = title)
    } else {
        // 해외일 경우 Google 지도 표시
        GoogleMapView(lat = lat, lng = lng, title = title)
    }
}

@Composable
private fun NaverMapView(lat: Double, lng: Double, title: String) {
    val position = com.naver.maps.geometry.LatLng(lat, lng)
    AndroidView(
        factory = { context ->
            MapView(context).apply {
                getMapAsync { naverMap ->
                    naverMap.moveCamera(com.naver.maps.map.CameraUpdate.scrollTo(position))
                    com.naver.maps.map.overlay.Marker().apply {
                        this.position = position
                        this.captionText = title
                        this.map = naverMap
                    }
                }
            }
        }
    )
}

@Composable
private fun GoogleMapView(lat: Double, lng: Double, title: String) {
    val position = com.google.android.gms.maps.model.LatLng(lat, lng)
    val cameraPositionState: CameraPositionState = rememberCameraPositionState {
        this.position = com.google.android.gms.maps.model.CameraPosition.fromLatLngZoom(position, 15f)
    }
    GoogleMap(cameraPositionState = cameraPositionState) {
        Marker(
            state = MarkerState(position = position),
            title = title
        )
    }
}