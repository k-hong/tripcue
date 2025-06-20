// file: tripcue/frame/uicomponents/Schedule/InventoryScheduleTest.kt
package com.example.tripcue.frame.uicomponents.Schedule

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.tripcue.frame.model.Routes
import com.example.tripcue.frame.model.ScheduleData
import com.example.tripcue.frame.viewmodel.ScheduleViewModel
import com.example.tripcue.frame.viewmodel.SharedScheduleViewModel

/**
 * 선택된 스케줄의 요약 정보를 보여주고,
 * 해당 스케줄에 포함된 세부 일정을 가로 슬라이드 형식으로 보여주는 화면 Composable
 *
 * @param navController 네비게이션 제어를 위한 컨트롤러
 * @param cityDocId 현재 선택된 도시 문서 ID (스케줄 식별자)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScheduleTest(navController: NavHostController, cityDocId: String) {
    val context = LocalContext.current

    // SharedViewModel: 액티비티 범위에서 선택된 스케줄 정보 공유
    val sharedScheduleViewModel: SharedScheduleViewModel = viewModel(
        LocalActivity.current as ComponentActivity
    )
    val selectedSchedule by sharedScheduleViewModel.selectedScheduleTitle.collectAsState()

    // 개별 스케줄 관련 데이터 관리용 ViewModel
    val scheduleViewModel: ScheduleViewModel = viewModel()

    // 화면 최초 진입 시 스케줄 제목 목록을 Firestore에서 불러옴
    LaunchedEffect(Unit) {
        scheduleViewModel.loadScheduleTitles()
    }

    // 선택된 스케줄이 변경되면 해당 스케줄의 세부 일정 데이터를 Firestore에서 불러옴
    LaunchedEffect(selectedSchedule?.id) {
        selectedSchedule?.let {
            scheduleViewModel.loadScheduleDetails(it.id)
        }
    }

    // 상세 일정 리스트 상태 구독
    val scheduleDetails by scheduleViewModel.scheduleDetails.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 선택된 스케줄 기본 정보 표시 (제목, 장소, 시작일, 종료일)
        selectedSchedule?.let { schedule ->
            Text("선택된 스케줄 정보", style = MaterialTheme.typography.titleLarge)
            Text("제목: ${schedule.title}")
            Text("장소: ${schedule.location}")
            Text("시작일: ${schedule.startDate}")
            Text("종료일: ${schedule.endDate}")
        } ?: run {
            Text("선택된 스케줄이 없습니다.")
        }

        // 스케줄 목록 헤더와 '추가하기' 버튼 배치
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("저장된 일정 목록", style = MaterialTheme.typography.titleMedium)
            Button(onClick = {
                if (selectedSchedule != null) {
                    navController.navigate(Routes.AddDetails.createRoute(cityDocId))
                }
            }) {
                Text("추가하기")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 저장된 스케줄 세부 목록을 LazyColumn 안에 배치
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp) // 좌우 패딩 살짝 조정
        ) {
            item {
                // [수정] 스케줄 카드들을 날짜별로 그룹화하고, 지도 보기 기능을 포함한 UI로 변경
                DateGroupedSchedule(
                    scheduleDetails = scheduleDetails,
                    onScheduleClick = { selectedScheduleData ->
                        sharedScheduleViewModel.setScheduleData(selectedScheduleData)
                        navController.navigate(Routes.InfoCard.createRoute(cityDocId))
                    }
                )
            }
        }
    }
}

/**
 * 단일 스케줄 정보를 카드 형태로 보여주는 Composable
 */
@Composable
fun ScheduleCard(
    schedule: ScheduleData,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) {
                onClick?.invoke()
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("위치: ${schedule.location}")
            Text("날짜: ${schedule.date}")
            Text("이동 수단: ${schedule.transportation.displayName}")
        }
    }
}

/**
 * [수정됨] 스케줄 리스트를 날짜별로 그룹화하고, 각 그룹마다 Pager와 지도 보기 기능을 제공
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DateGroupedSchedule(
    scheduleDetails: List<ScheduleData>,
    onScheduleClick: (ScheduleData) -> Unit
) {
    // 날짜를 기준으로 일정들을 그룹화하고, 날짜순으로 정렬
    val groupedByDate = scheduleDetails.groupBy { it.date }.toSortedMap()

    if (groupedByDate.isEmpty()) {
        Text("표시할 일정이 없습니다.", modifier = Modifier.padding(16.dp))
        return
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        groupedByDate.forEach { (date, tasksForDate) ->
            // [추가] 지도 표시 여부를 관리하는 상태
            var showMap by remember { mutableStateOf(false) }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                // 날짜 제목과 '지도로 보기' 버튼
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📅 $date (${tasksForDate.size}개)",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                    )
                    TextButton(onClick = { showMap = !showMap }) {
                        Text(if (showMap) "지도 닫기" else "지도로 보기")
                    }
                }

                // [추가] showMap 상태에 따라 지도 또는 Pager를 표시
                if (showMap) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp) // 지도의 높이 지정
                            .padding(vertical = 8.dp)
                    ) {
                        // 새로 만든 구글 지도 Composable 호출
                        SchedulesGoogleMap(schedules = tasksForDate)
                    }
                } else {
                    // 기존의 HorizontalPager 로직
                    val pagerState = rememberPagerState(pageCount = { tasksForDate.size })

                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp), // Pager 높이 조정
                        contentPadding = PaddingValues(horizontal = 32.dp),
                        pageSpacing = 16.dp
                    ) { pageIndex ->
                        val schedule = tasksForDate[pageIndex]
                        ScheduleCard(
                            schedule = schedule,
                            onClick = { onScheduleClick(schedule) }
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "일정 ${pagerState.currentPage + 1} / ${tasksForDate.size}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
            Divider() // 각 날짜 그룹 사이에 구분선 추가
        }
    }
}