package com.example.tripcue.frame.uicomponents.Schedule

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.tripcue.frame.viewmodel.ScheduleViewModel
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraPosition
import com.naver.maps.map.compose.*

@OptIn(ExperimentalNaverMapApi::class)
@Composable
fun MySchedulesMapScreen(
    navController: NavController,
    cityDocId: String,
    scheduleViewModel: ScheduleViewModel = viewModel()
) {
    LaunchedEffect(cityDocId) {
        scheduleViewModel.loadScheduleDetails(cityDocId)
    }

    val scheduleDetails by scheduleViewModel.scheduleDetails.collectAsState()

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition(LatLng(37.5665, 126.9780), 10.0)
    }

        NaverMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState
        ) {
            scheduleDetails.forEach { schedule ->
                if (schedule.latitude != null && schedule.longitude != null) {
                    Marker(
                        state = rememberMarkerState(position = LatLng(schedule.latitude, schedule.longitude)),
                        captionText = schedule.location
                    )
                }
            }
        }
}