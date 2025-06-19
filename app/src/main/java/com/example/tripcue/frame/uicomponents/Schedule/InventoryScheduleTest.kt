package com.example.tripcue.frame.uicomponents.Schedule

// 필요한 Android 및 Compose 관련 라이브러리 임포트
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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

/**
 * 선택된 스케줄의 요약 정보를 보여주고,
 * 해당 스케줄에 포함된 세부 일정을 가로 슬라이드 형식으로 보여주는 화면 Composable
 *
 * @param navController 네비게이션 제어를 위한 컨트롤러
 * @param cityDocId 현재 선택된 도시 문서 ID (스케줄 식별자)
 */
@Composable
fun InventoryScheduleTest(navController: NavHostController, cityDocId: String) {
    val context = LocalContext.current

    // 현재 네비게이션 스택에서 최상위 엔트리 가져오기 (디버그용)
    val currentEntry = navController.currentBackStackEntry
    Log.d("NavBackStack", "Current destination route: ${currentEntry?.destination?.route}")

    // SharedViewModel: 액티비티 범위에서 선택된 스케줄 정보 공유
    val sharedScheduleViewModel: SharedScheduleViewModel = viewModel(
        LocalActivity.current as ComponentActivity
    )

    // 선택된 스케줄 (ScheduleTitle)
    val selectedSchedule by sharedScheduleViewModel.selectedSchedule.collectAsState()

    // 개별 스케줄 관련 데이터 관리용 ViewModel
    val scheduleViewModel: ScheduleViewModel = viewModel()

    // 스케줄 제목 리스트를 상태로 구독
    val scheduleTitles by scheduleViewModel.scheduleTitles.collectAsState()

    // 모든 스케줄 데이터를 상태로 구독
    val schedules by scheduleViewModel.schedules.collectAsState()

    // 현재 Pager에서 보고 있는 상세 스케줄 상태 저장
    var viewedSchedule by remember { mutableStateOf<ScheduleData?>(null) }

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

    // 스케줄 제목 리스트를 디버그 로그로 출력
    LaunchedEffect(scheduleTitles) {
        scheduleTitles.forEach { title ->
            Log.d("DebugScheduleTitle", "Title: ${title.title}")
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
            // 선택된 스케줄이 없을 경우 안내 메시지 표시
            Text("선택된 스케줄이 없습니다.")
        }

        // 스케줄 목록 헤더와 '추가하기' 버튼 배치
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("저장된 일정 목록", style = MaterialTheme.typography.titleMedium)
            Button(onClick = {
                if (selectedSchedule != null) {
                    // '추가하기' 버튼 클릭 시 상세 추가 화면으로 이동
                    navController.navigate(Routes.AddDetails.createRoute(cityDocId))
                }
            }) {
                Text("추가하기")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 저장된 스케줄 세부 목록을 LazyColumn 안에 배치
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            item {
                // 스케줄 카드들을 가로 슬라이드 뷰 (Pager) 형태로 보여줌
                SchedulePager(
                    schedules = scheduleDetails,
                    onScheduleClick = { selectedScheduleData ->
                        // 스케줄 카드 클릭 시 해당 일정의 상세 화면으로 이동
                        val scheduleTitle = scheduleTitles.find { it.id == selectedSchedule?.id }
                        if (scheduleTitle != null) {
                            navController.currentBackStackEntry?.savedStateHandle?.set("selectedSchedule", selectedScheduleData)
                            navController.navigate(Routes.InfoCard.createRoute(cityDocId))
                        } else {
                            Toast.makeText(context, "해당 일정의 전체 데이터를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                            Log.w("InventoryScheduleTest", "Can't find ScheduleTitle for ScheduleData id: ${selectedScheduleData.id}")
                        }
                    },
                    onScheduleView = { viewed ->
                        // Pager 페이지 변경 시 viewedSchedule 상태 갱신
                        viewedSchedule = viewed
                    }
                )
            }
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
            item {
                // 현재 선택된 스케줄 위치를 지도 대신 텍스트로 간단 표시
                viewedSchedule?.let {
                    Text("지도 for ${viewedSchedule?.location}")
                } ?: Text("아직 선택된 일정이 없습니다.")
            }
        }
    }
}

/**
 * 단일 스케줄 정보를 카드 형태로 보여주는 Composable
 *
 * @param schedule 표시할 스케줄 데이터
 */
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

/**
 * 스케줄 리스트를 가로 슬라이드 뷰 (Pager)로 보여주는 Composable
 *
 * @param schedules 보여줄 스케줄 리스트
 * @param onScheduleClick 스케줄 카드 클릭 시 호출되는 콜백
 * @param onScheduleView 현재 보고 있는 스케줄 변경 시 호출되는 콜백
 */
@Composable
fun SchedulePager(
    schedules: List<ScheduleData>,
    onScheduleClick: (ScheduleData) -> Unit,
    onScheduleView: (ScheduleData) -> Unit
) {
    // Pager 상태 생성, 총 페이지 수는 스케줄 수에 맞춤
    val pagerState = rememberPagerState(pageCount = { schedules.size })
    val coroutineScope = rememberCoroutineScope()

    // Pager 페이지 변경 시 현재 보고 있는 스케줄 상태 갱신
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage in schedules.indices) {
            onScheduleView(schedules[pagerState.currentPage])
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // 좌우 끝 페이지로 즉시 이동하는 버튼 배치
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
        ) {
            Button(
                onClick = {
                    coroutineScope.launch {
                        pagerState.scrollToPage(0) // 첫 페이지로 이동
                    }
                },
                enabled = schedules.isNotEmpty() && pagerState.currentPage != 0
            ) {
                Text("◀ 제일 왼쪽")
            }

            Button(
                onClick = {
                    coroutineScope.launch {
                        pagerState.scrollToPage(schedules.lastIndex) // 마지막 페이지로 이동
                    }
                },
                enabled = schedules.isNotEmpty() && pagerState.currentPage != schedules.lastIndex
            ) {
                Text("제일 오른쪽 ▶")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // HorizontalPager로 스케줄 카드들을 가로 슬라이드 뷰로 보여줌
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 64.dp),
            pageSpacing = 16.dp,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            val currentPage = pagerState.currentPage
            val pageOffset = (currentPage - page).absoluteValue
            // 현재 페이지에 가까울수록 크기 확대 효과를 줌 (scale)
            val scale = 1f - (0.15f * pageOffset.coerceAtMost(1))

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .clickable {
                        // 스케줄 카드 클릭 시 콜백 호출
                        onScheduleClick(schedules[page])
                    }
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                ScheduleCard(schedule = schedules[page])
            }
        }
    }
}
