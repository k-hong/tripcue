// 파일 위치: app/src/main/java/com/example/tripcue/frame/uicomponents/schedule/InventoryScheduleTest.kt

package com.example.tripcue.frame.uicomponents.Schedule

import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.tripcue.frame.model.Routes
import com.example.tripcue.frame.model.ScheduleData
import com.example.tripcue.frame.viewmodel.ScheduleViewModel
import com.example.tripcue.frame.viewmodel.SharedScheduleViewModel
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

@Composable
fun InventoryScheduleTest(navController: NavHostController, cityDocId: String) {
    val context = LocalContext.current
    val sharedScheduleViewModel: SharedScheduleViewModel = viewModel(
        LocalActivity.current as ComponentActivity
    )
    val selectedSchedule by sharedScheduleViewModel.selectedSchedule.collectAsState()
    val scheduleViewModel: ScheduleViewModel = viewModel()

    // 선택된 스케줄이 변경되면 해당 스케줄의 세부 일정 데이터를 Firestore에서 불러옴
    LaunchedEffect(selectedSchedule?.id) {
        selectedSchedule?.id?.let {
            // cityDocId를 사용하여 올바른 경로의 상세 일정을 불러옵니다.
            scheduleViewModel.loadScheduleDetails(it)
        }
    }

    val scheduleDetails by scheduleViewModel.scheduleDetails.collectAsState()
    var viewedSchedule by remember { mutableStateOf<ScheduleData?>(null) }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        selectedSchedule?.let { schedule ->
            Text("선택된 스케줄 정보", style = MaterialTheme.typography.titleLarge)
            Text("제목: ${schedule.title}")
            Text("장소: ${schedule.location}")
            Text("시작일: ${schedule.startDate}")
            Text("종료일: ${schedule.endDate}")
        } ?: run {
            Text("선택된 스케줄이 없습니다.")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ▼▼▼ 이 Row 부분을 수정합니다 ▼▼▼
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("저장된 세부 일정", style = MaterialTheme.typography.titleMedium)

            Row {
                // ◀ '지도로 보기' 버튼 추가
                Button(onClick = {
                    navController.navigate(Routes.MySchedulesMap.createRoute(cityDocId))
                }) {
                    Text("지도로 보기")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = {
                    if (selectedSchedule != null) {
                        navController.navigate(Routes.AddDetails.createRoute(cityDocId))
                    }
                }) {
                    Text("추가하기")
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 저장된 스케줄 세부 목록을 Pager로 보여주는 부분 (기존과 동일)
        SchedulePager(
            schedules = scheduleDetails,
            onScheduleClick = { selectedScheduleData ->
                navController.currentBackStackEntry?.savedStateHandle?.set("selectedSchedule", selectedScheduleData)
                navController.navigate(Routes.InfoCard.createRoute(cityDocId))
            },
            onScheduleView = { viewed ->
                viewedSchedule = viewed
            }
        )
    }
}


@Composable
fun ScheduleCard(schedule: ScheduleData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("위치: ${schedule.location}")
            Text("날짜: ${schedule.date}")
            Text("이동 수단: ${schedule.transportation.displayName}")
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SchedulePager(
    schedules: List<ScheduleData>,
    onScheduleClick: (ScheduleData) -> Unit,
    onScheduleView: (ScheduleData) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { schedules.size })
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(pagerState.currentPage, schedules.size) {
        if (schedules.isNotEmpty() && pagerState.currentPage < schedules.size) {
            onScheduleView(schedules[pagerState.currentPage])
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (schedules.isNotEmpty()) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
            ) {
                Button(
                    onClick = { coroutineScope.launch { pagerState.scrollToPage(0) } },
                    enabled = pagerState.currentPage != 0
                ) { Text("◀ 제일 왼쪽") }

                Button(
                    onClick = { coroutineScope.launch { pagerState.scrollToPage(schedules.lastIndex) } },
                    enabled = pagerState.currentPage != schedules.lastIndex
                ) { Text("제일 오른쪽 ▶") }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 64.dp),
            pageSpacing = 16.dp,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            val pageOffset = (pagerState.currentPage - page).absoluteValue
            val scale = 1f - (0.15f * pageOffset.coerceAtMost(1))
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .clickable { onScheduleClick(schedules[page]) }
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                ScheduleCard(schedule = schedules[page])
            }
        }
    }
}