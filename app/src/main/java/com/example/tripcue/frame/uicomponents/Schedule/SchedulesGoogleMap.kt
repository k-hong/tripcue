// file: tripcue/frame/uicomponents/Schedule/SchedulesGoogleMap.kt
package com.example.tripcue.frame.uicomponents.Schedule

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.tripcue.frame.model.ScheduleData
import com.example.tripcue.frame.uicomponents.home.GeocodingUtils
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * ScheduleData 리스트(위치 이름)를 받아 구글 지도에 실시간으로 마커와 경로를 표시하는 Composable
 */
@Composable
fun SchedulesGoogleMap(schedules: List<ScheduleData>) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var resolvedSchedules by remember { mutableStateOf<List<ScheduleData>>(emptyList()) }

    // schedules(이름 목록)가 변경될 때마다 Geocoding을 실행
    LaunchedEffect(schedules) {
        isLoading = true
        // 코루틴 스코프 내에서 비동기 작업을 병렬로 처리
        coroutineScope {
            val resolved = schedules.map { schedule ->
                async {
                    // 주소로 좌표를 검색
                    val latLng = GeocodingUtils.fetchCoordinatesFromAddress(context, schedule.location)
                    if (latLng != null) {
                        // 성공하면 좌표를 포함한 새 ScheduleData 객체 반환
                        schedule.copy(latitude = latLng.latitude, longitude = latLng.longitude)
                    } else {
                        // 실패하면 null 반환
                        null
                    }
                }
            }.awaitAll().filterNotNull() // 모든 작업이 끝날 때까지 기다린 후, null이 아닌 결과만 필터링
            resolvedSchedules = resolved
        }
        isLoading = false
    }

    if (isLoading) {
        // 로딩 중일 때 인디케이터 표시
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (resolvedSchedules.isEmpty()) {
        // 좌표를 찾은 일정이 없을 때 메시지 표시
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("지도에 표시할 위치를 찾을 수 없습니다.")
        }
        return
    }

    // 모든 마커를 포함하는 카메라 경계(Bounds) 계산
    val boundsBuilder = LatLngBounds.builder()
    resolvedSchedules.forEach { schedule ->
        boundsBuilder.include(LatLng(schedule.latitude!!, schedule.longitude!!))
    }
    val bounds = boundsBuilder.build()

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            LatLng(resolvedSchedules.first().latitude!!, resolvedSchedules.first().longitude!!), 12f
        )
    }

    // 카메라 위치 조정
    LaunchedEffect(resolvedSchedules) {
        if (resolvedSchedules.size > 1) {
            cameraPositionState.animate(CameraUpdateFactory.newLatLngBounds(bounds, 100))
        } else {
            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(LatLng(resolvedSchedules.first().latitude!!, resolvedSchedules.first().longitude!!), 14f))
        }
    }

    // 구글 지도 Composable
    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState
    ) {
        // 마커 추가
        resolvedSchedules.forEach { schedule ->
            Marker(
                state = MarkerState(position = LatLng(schedule.latitude!!, schedule.longitude!!)),
                title = schedule.location
            )
        }

        // 마커들을 잇는 선 추가
        if (resolvedSchedules.size > 1) {
            Polyline(
                points = resolvedSchedules.map { LatLng(it.latitude!!, it.longitude!!) },
                color = Color.Blue,
                width = 5f
            )
        }
    }
}