package com.example.tripcue.frame.uicomponents.map


import androidx.compose.foundation.layout.fillMaxSize

import androidx.compose.material3.MaterialTheme

import androidx.compose.runtime.Composable

import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

import com.naver.maps.map.compose.ExperimentalNaverMapApi
import com.naver.maps.map.compose.Marker
import com.naver.maps.map.compose.NaverMap
import com.naver.maps.map.compose.rememberCameraPositionState
import com.naver.maps.map.compose.rememberMarkerState
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraPosition


@OptIn(ExperimentalNaverMapApi::class)
@Composable
fun NaverMapScreen(modifier: Modifier = Modifier) {
    val konkuk = LatLng(37.5408, 127.0793)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition(konkuk, 15.0)
    }

    NaverMap(
        modifier = modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState
    ) {
        Marker(
            state = rememberMarkerState(position = konkuk),
            captionText = "건국대학교"
        )
    }
}

@Preview(showBackground = true)
@Composable
fun NaverMapScreenPreview() {
    // MaterialTheme 또는 AppTheme로 감싸는 게 일반적
    MaterialTheme {
        // 단순한 Modifier를 넘겨 미리보기 호출
        NaverMapScreen(modifier = Modifier.fillMaxSize())
    }
}