package com.example.tripcue.frame.uicomponents.Schedule

import android.R.attr.onClick
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.tripcue.frame.viewmodel.ScheduleViewModel
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation.NavHostController
import com.example.tripcue.frame.model.Routes
import com.example.tripcue.frame.model.ScheduleData
import com.example.tripcue.frame.model.ScheduleTitle
import com.example.tripcue.frame.model.factory.Schedules.schedules
import com.google.gson.Gson
import java.net.URLEncoder

@Composable
fun InventoryScheduleTest(navController: NavHostController, cityDocId: String) {
//    val savedStateHandle = navController.previousBackStackEntry?.savedStateHandle
//    val selectedSchedule = savedStateHandle?.get<ScheduleTitle>("selectedSchedule")
//
//    // ScheduleTitle 안에 담긴 세부 일정 리스트 (ScheduleData 리스트)
//    val schedules = selectedSchedule?.ScheduleData ?: emptyList()

    val navBackStackEntry = remember(navController.currentBackStackEntry) {
        navController.getBackStackEntry(Routes.Schedules.route)
    }
    val scheduleViewModel: ScheduleViewModel = viewModel(navBackStackEntry)

    val schedules by scheduleViewModel.schedules.collectAsState()
    val savedStateHandle = navBackStackEntry.savedStateHandle
    val selectedScheduleAny = savedStateHandle?.get<Any>("selectedSchedule")
    val selectedSchedule = selectedScheduleAny as? ScheduleTitle

    LaunchedEffect(selectedSchedule?.id) {
        selectedSchedule?.let {
            scheduleViewModel.loadScheduleDetails(it.id)
        }
    }

    val scheduleDetails by scheduleViewModel.scheduleDetails.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (selectedSchedule != null) {
            Text("선택된 스케줄 정보", style = MaterialTheme.typography.titleLarge)
            Text("제목: ${selectedSchedule.title}")
            Text("장소: ${selectedSchedule.location}")
            Text("시작일: ${selectedSchedule.startDate}")
            Text("종료일: ${selectedSchedule.endDate}")
        } else {
            Text("선택된 스케줄이 없습니다.")
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("저장된 일정 목록", style = MaterialTheme.typography.titleMedium)
            Button(onClick = {
                if (selectedSchedule != null) {
                    navController.navigate(Routes.AddDetails.createRoute(selectedSchedule.id))
                }
            }) {
                Text("추가하기")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(scheduleDetails) { detail ->
                ScheduleCard(schedule = detail) {
                    val backStackEntry = navController.getBackStackEntry(Routes.InventSchedule.route)
                    backStackEntry.savedStateHandle["selectedSchedule"] = detail
                    navController.navigate("${Routes.InfoCard.route.replace("{cityDocId}", cityDocId)}")
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun ScheduleCard(schedule: ScheduleData, onClick: ()->Unit ) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("위치: ${schedule.location}")
            Text("날짜: ${schedule.date}")
            Text("이동 수단: ${schedule.transportation.displayName}")
        }
    }
}