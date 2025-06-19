package com.example.tripcue.frame.uicomponents.Schedule

import android.R.attr.minDate
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.example.tripcue.TripcueApplication
import com.example.tripcue.frame.model.PlaceResult
import com.example.tripcue.frame.model.Routes
import com.example.tripcue.frame.model.ScheduleData
import com.example.tripcue.frame.model.ScheduleTitle
import com.example.tripcue.frame.model.Transportation
import com.example.tripcue.frame.model.WeatherInfo
import com.example.tripcue.frame.uicomponents.fetchPlaceAutocomplete
import com.example.tripcue.frame.uicomponents.getApiKeyFromManifest
import com.example.tripcue.frame.viewmodel.ScheduleViewModel
import com.example.tripcue.frame.viewmodel.SharedScheduleViewModel
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

// 장소 자동완성 (Autocomplete) API를 호출하여 추천 위치 목록을 가져오는 suspend 함수
suspend fun fetchPlaceAutocomplete2(query: String, apiKey: String): List<PlaceResult> =
    withContext(Dispatchers.IO) {
        val sessionToken = UUID.randomUUID().toString() // 사용자 세션을 식별하기 위한 토큰
        val url = "https://maps.googleapis.com/maps/api/place/autocomplete/json?" +
                "input=${URLEncoder.encode(query, "UTF-8")}&" +
                "key=$apiKey&sessiontoken=$sessionToken&components=country:kr" // 한국 지역에 한정

        val request = Request.Builder().url(url).build()
        val client = OkHttpClient()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: return@withContext emptyList()

        val json = JSONObject(responseBody)
        val predictions = json.getJSONArray("predictions")

        // 각 추천 결과에서 description과 place_id를 추출 후 좌표를 조회하여 리스트 생성
        return@withContext (0 until predictions.length()).mapNotNull { i ->
            val prediction = predictions.getJSONObject(i)
            val description = prediction.getString("description")
            val googlePlaceId = prediction.getString("place_id") // 장소 고유 식별자

            // 좌표 조회 (비동기)
            val latLng = fetchPlaceDetails(googlePlaceId, apiKey) ?: return@mapNotNull null

            // Firestore 저장은 생략하고 빈 ID 사용
            PlaceResult("", description, latLng.first, latLng.second)
        }
    }

// Google Places API를 통해 place_id에 해당하는 장소의 좌표(lat, lng)를 조회하는 함수
suspend fun fetchPlaceDetails(placeId: String, apiKey: String): Pair<Double, Double>? =
    withContext(Dispatchers.IO) {
        val url = "https://maps.googleapis.com/maps/api/place/details/json?" +
                "place_id=$placeId&key=$apiKey"

        val request = Request.Builder().url(url).build()
        val client = OkHttpClient()

        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: return@withContext null

        val json = JSONObject(body)
        val result = json.getJSONObject("result")
        val location = result.getJSONObject("geometry").getJSONObject("location")

        val lat = location.getDouble("lat")
        val lng = location.getDouble("lng")

        Log.d("Debug", "$lat, $lng")

        return@withContext lat to lng
    }

@Composable
fun AddScheduleTest(
    navController: NavHostController,
    cityDocId: String   // Firestore의 도시 문서 ID
) {
    val scheduleViewModel: ScheduleViewModel = viewModel()
    val errorMessage by scheduleViewModel.errorMessage.collectAsState()
    val sharedScheduleViewModel: SharedScheduleViewModel = viewModel(
        LocalActivity.current as ComponentActivity
    )
    val selectedSchedule by sharedScheduleViewModel.selectedSchedule.collectAsState()

    // 입력 상태 저장 변수들
    var location by remember { mutableStateOf("") }
    var transportation by remember { mutableStateOf(Transportation.BUS) }
    var details by remember { mutableStateOf("") }
    var selectedLatLng by remember { mutableStateOf<Pair<Double, Double>?>(null) }

    var showDatePicker by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }

    // 스케줄의 시작/종료일 파싱
    val minDate = selectedSchedule?.startDate?.let { LocalDate.parse(it) }
    val maxDate = selectedSchedule?.endDate?.let { LocalDate.parse(it) }

    // 장소 자동완성을 위한 검색어 및 추천 결과 상태
    var locationQuery by remember { mutableStateOf("") }
    var suggestions by remember { mutableStateOf<List<PlaceResult>>(emptyList()) }
    var expanded by remember { mutableStateOf(false) }

    val apiKey = getApiKeyFromManifest()

    // 사용자 입력이 변경될 때마다 자동완성 API 호출
    LaunchedEffect(locationQuery) {
        if (locationQuery.length < 2) {
            suggestions = emptyList()
            expanded = false
            return@LaunchedEffect
        }

        suggestions = fetchPlaceAutocomplete2(locationQuery, apiKey)
        expanded = suggestions.isNotEmpty()
    }

    // UI 레이아웃
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("새 일정 추가", style = MaterialTheme.typography.titleLarge)

        Spacer(modifier = Modifier.height(8.dp))

        // 위치 입력 필드
        OutlinedTextField(
            value = if (location.isNotEmpty()) location else locationQuery,
            onValueChange = {
                locationQuery = it
                location = ""
            },
            label = { Text("위치") },
            modifier = Modifier.fillMaxWidth()
        )

        // 자동완성 추천 목록 드롭다운
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth()
        ) {
            suggestions.forEach { suggestion ->
                DropdownMenuItem(
                    onClick = {
                        location = suggestion.name
                        locationQuery = suggestion.name
                        selectedLatLng = suggestion.lat to suggestion.lng
                        expanded = false
                    },
                    text = { Text(suggestion.name) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Column {
            // 날짜 선택 필드 (DatePicker로 날짜 선택)
            OutlinedTextField(
                value = selectedDate.toString(),
                onValueChange = {},
                label = { Text("날짜") },
                enabled = false,
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true }
            )

            // 날짜 선택 다이얼로그 (제한 적용)
            if (showDatePicker && minDate != null && maxDate != null ) {
                val context = LocalContext.current

                DisposableEffect(showDatePicker) {
                    val dialog = android.app.DatePickerDialog(
                        context,
                        { _, year, month, dayOfMonth ->
                            selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
                            showDatePicker = false
                        },
                        selectedDate.year,
                        selectedDate.monthValue - 1,
                        selectedDate.dayOfMonth
                    )

                    dialog.datePicker.minDate =
                        minDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    dialog.datePicker.maxDate =
                        maxDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

                    dialog.setOnCancelListener {
                        showDatePicker = false
                    }

                    dialog.show()

                    onDispose {
                        dialog.dismiss()
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 교통수단 선택 드롭다운
            DropdownMenuBox(
                selected = transportation,
                onSelect = { transportation = it },
                enabled = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 상세 정보 입력 필드
            OutlinedTextField(
                value = details,
                onValueChange = { details = it },
                label = { Text("상세 정보") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 에러 메시지 표시
            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            // "등록하기" 버튼 - 스케줄 및 타이틀 저장 후 이동
            Button(
                onClick = {
                    val scheduleId = UUID.randomUUID().toString()
                    val finalLocation = if (location.isNotEmpty()) location else locationQuery

                    // 스케줄 데이터 생성
                    val newSchedule = ScheduleData(
                        id = scheduleId,
                        location = finalLocation,
                        date = selectedDate.toString(),
                        transportation = transportation,
                        weather = null, // 날씨 정보는 아직 없음
                        details = details,
                        latitude = selectedLatLng?.first,
                        longitude = selectedLatLng?.second
                    )

                    // Firestore에 저장
                    scheduleViewModel.addSchedule(newSchedule, cityDocId)

                    // 스케줄 타이틀 생성 및 공유 ViewModel에 저장
                    val scheduleTitle = ScheduleTitle(
                        id = cityDocId,
                        title = finalLocation,
                        location = finalLocation,
                        startDate = selectedDate.toString(),
                        endDate = selectedDate.toString()
                    )
                    sharedScheduleViewModel.setSchedule(scheduleTitle)

                    // 일정 목록 화면으로 이동
                    navController.navigate(Routes.InventSchedule.createRoute(cityDocId)) {
                        popUpTo(Routes.AddSchedule.route) { inclusive = true } // 현재 화면 제거
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("등록하기")
            }

            Spacer(modifier = Modifier.height(8.dp))

            // "닫기" 버튼 - 화면 이동만 수행
            Button(
                onClick = {
                    navController.navigate(Routes.InventSchedule.createRoute(cityDocId)) {
                        popUpTo(Routes.AddSchedule.route) { inclusive = true }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("닫기")
            }
        }
    }
}