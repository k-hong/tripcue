package com.example.tripcue.frame.uicomponents.map

import android.util.Log // ◀◀◀ Log import 추가
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tripcue.frame.viewmodel.ScheduleViewModel
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.compose.*
// ... 다른 import 문들

// 임시 지오코딩 함수 (이전과 동일)
private fun geocodeAddress(address: String): LatLng? {
    return when (address) {
        "서울" -> LatLng(37.5665, 126.9780)
        // ... 다른 위치들
        else -> null
    }
}

@OptIn(ExperimentalNaverMapApi::class)
@Composable
fun NaverMapScreen(modifier: Modifier = Modifier) {
    val scheduleViewModel: ScheduleViewModel = viewModel()
    val schedules by scheduleViewModel.schedules.collectAsState()

    // --- 디버깅 로그 1: ViewModel이 데이터를 가져오는지 확인 ---
    Log.d("MapDebug", "전체 스케줄 개수: ${schedules.size}개")
    if (schedules.isEmpty()) {
        Log.d("MapDebug", "경고: Firestore에서 스케줄을 가져오지 못했거나, 컬렉션이 비어있습니다.")
    }

    NaverMap(
        // ... 지도 설정은 동일 ...
    ) {
        schedules.forEach { schedule ->
            // --- 디버깅 로그 2: 각 스케줄의 위치 값이 무엇인지 확인 ---
            Log.d("MapDebug", "처리 중인 스케줄 위치: '${schedule.location}'")

            val position = geocodeAddress(schedule.location)

            // --- 디버깅 로그 3: 좌표 변환이 성공했는지 확인 ---
            if (position != null) {
                Log.d("MapDebug", "'${schedule.location}' 좌표 변환 성공: $position")
                Marker(
                    state = rememberMarkerState(position = position),
                    captionText = schedule.location
                )
            } else {
                Log.d("MapDebug", "경고: '${schedule.location}'의 좌표를 찾지 못해 마커를 표시할 수 없습니다.")
            }
        }
    }
}