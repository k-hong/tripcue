package com.example.tripcue.frame.uicomponents.Schedule

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tripcue.frame.viewmodel.ScheduleViewModel
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraPosition
import com.naver.maps.map.compose.*

@OptIn(ExperimentalNaverMapApi::class)
@Composable
fun MySchedulesMapScreen() {
    // 1. ScheduleViewModel에서 데이터 불러옴
    val scheduleViewModel: ScheduleViewModel = viewModel()
    val schedules by scheduleViewModel.schedules.collectAsState()

    // 2. 지도의 초기 카메라 위치 (대한민국 전체)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition(LatLng(37.5665, 126.9780), 6.0)
    }


    NaverMap(cameraPositionState = cameraPositionState, modifier = Modifier.fillMaxSize()) {
            // 3. 스케쥴 하나씩 돌면서 마커 표시
            schedules.forEach { schedule ->
                // 4. 위도(latitude)와 경도(longitude) 값이 있는 일정에 대해서만 마커를 표시
                if (schedule.latitude != null && schedule.longitude != null) {
                    val position = LatLng(schedule.latitude, schedule.longitude)
                    Marker(
                        state = rememberMarkerState(position = position),
                        captionText = schedule.location // 마커 캡션으로 장소 이름 표시
                    )
                }
            }
        }

}


