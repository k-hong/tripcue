package com.example.tripcue.frame.uicomponents.Schedule

import android.app.Activity
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.tripcue.frame.model.Routes
import com.example.tripcue.frame.model.ScheduleData
import com.example.tripcue.frame.model.WeatherInfo
import com.example.tripcue.frame.viewmodel.ScheduleViewModel
import com.example.tripcue.frame.viewmodel.WeatherViewModel
import com.google.gson.Gson
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.format.DateTimeParseException
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import com.example.tripcue.frame.model.PlaceResult
import com.example.tripcue.frame.uicomponents.getApiKeyFromManifest
import com.example.tripcue.frame.uicomponents.location
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
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
import java.util.UUID
import androidx.lifecycle.viewModelScope
import com.example.tripcue.frame.viewmodel.SharedScheduleViewModel

// 서울 기본 좌표 (위치 정보 없을 때 사용)
const val DEFAULT_LAT = 37.5665  // 서울 위도
const val DEFAULT_LNG = 126.9780 // 서울 경도

/**
 * 장소 자동완성 검색 및 결과 Firestore 저장 (PlaceResult 리스트 반환)
 */
suspend fun fetchPlaceAutocomplete3(query: String, apiKey: String): List<PlaceResult> = withContext(Dispatchers.IO) {
    val sessionToken = UUID.randomUUID().toString()

    val url = "https://maps.googleapis.com/maps/api/place/autocomplete/json?" +
            "input=${URLEncoder.encode(query, "UTF-8")}&" +
            "key=$apiKey&sessiontoken=$sessionToken&components=country:kr"

    val request = Request.Builder().url(url).build()
    val client = OkHttpClient()
    val response = client.newCall(request).execute()
    val responseBody = response.body?.string() ?: return@withContext emptyList()

    val json = JSONObject(responseBody)
    val predictions = json.getJSONArray("predictions")

    return@withContext (0 until predictions.length()).mapNotNull { i ->
        val prediction = predictions.getJSONObject(i)
        val description = prediction.getString("description")
        val googlePlaceId = prediction.getString("place_id")

        // 상세 정보(위경도) 가져오기
        val latLng = fetchPlaceDetails(googlePlaceId, apiKey) ?: return@mapNotNull null

        // Firestore에 장소 정보 저장
        val db = Firebase.firestore
        val newDocRef = db.collection("places").document()
        val firebaseDocId = newDocRef.id
        val placeResult = PlaceResult(firebaseDocId, description, latLng.first, latLng.second)

        newDocRef.set(placeResult).await()

        placeResult
    }
}

/**
 * 장소 ID(place_id)를 통해 위도, 경도 반환
 */
suspend fun fetchPlaceDetails2(placeId: String, apiKey: String): Pair<Double, Double>? = withContext(Dispatchers.IO) {
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

    return@withContext lat to lng
}

/**
 * 위도/경도를 기상청 격자 좌표로 변환하는 함수 (단기예보 API용)
 */
fun latLonToGrid(lat: Double, lon: Double): Pair<Int, Int> {
    // 수치 예보 격자 좌표 변환 공식 (기상청 공식 문서 기반)
    val RE = 6371.00877 // 지구 반경
    val GRID = 5.0      // 격자 간격
    val SLAT1 = 30.0    // 표준 위도 1
    val SLAT2 = 60.0    // 표준 위도 2
    val OLON = 126.0    // 기준 경도
    val OLAT = 38.0     // 기준 위도
    val XO = 43         // 기준 X좌표
    val YO = 136        // 기준 Y좌표

    val DEGRAD = Math.PI / 180.0
    val re = RE / GRID
    val slat1 = SLAT1 * DEGRAD
    val slat2 = SLAT2 * DEGRAD
    val olon = OLON * DEGRAD
    val olat = OLAT * DEGRAD

    val sn = Math.tan(Math.PI * 0.25 + slat2 * 0.5) / Math.tan(Math.PI * 0.25 + slat1 * 0.5)
    val snLog = Math.log(Math.cos(slat1) / Math.cos(slat2)) / Math.log(sn)
    val sf = Math.tan(Math.PI * 0.25 + slat1 * 0.5)
    val sfPow = Math.pow(sf, snLog) * Math.cos(slat1) / snLog
    val ro = re * sfPow / Math.pow(Math.tan(Math.PI * 0.25 + olat * 0.5), snLog)

    val ra = re * sfPow / Math.pow(Math.tan(Math.PI * 0.25 + lat * DEGRAD * 0.5), snLog)
    var theta = lon * DEGRAD - olon
    if (theta > Math.PI) theta -= 2.0 * Math.PI
    if (theta < -Math.PI) theta += 2.0 * Math.PI
    theta *= snLog

    val x = (ra * Math.sin(theta) + XO + 0.5).toInt()
    val y = (ro - ra * Math.cos(theta) + YO + 0.5).toInt()

    return x to y
}

/**
 * 여행 일정 상세 정보를 보여주는 화면
 * - 수정 모드 지원
 * - 장소 자동완성 + 날씨 불러오기
 * - PDF 내보내기
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoCardScreen(
    navController: NavController,
    cityDocId: String
) {
    val context = LocalContext.current
    val activity = LocalActivity.current as ComponentActivity
    val weatherViewModel: WeatherViewModel = viewModel()
    val scheduleViewModel: ScheduleViewModel = viewModel()

    // 이전 화면에서 전달된 ScheduleData (selectedSchedule)
    val selectedSchedule = navController.previousBackStackEntry
        ?.savedStateHandle
        ?.get<ScheduleData>("selectedSchedule")

    if (selectedSchedule == null) {
        Text("선택된 일정이 없습니다.")
        return
    }

    // 초기값 세팅
    val initialSchedule = selectedSchedule
    var isEditing by remember { mutableStateOf(false) }

    var location by remember { mutableStateOf(initialSchedule.location) }
    var latitude by remember { mutableStateOf(initialSchedule.latitude ?: DEFAULT_LAT) }
    var longitude by remember { mutableStateOf(initialSchedule.longitude ?: DEFAULT_LNG) }
    var date by remember {
        mutableStateOf(
            try {
                LocalDate.parse(initialSchedule.date)
            } catch (e: DateTimeParseException) {
                LocalDate.now()
            }
        )
    }
    var transportation by remember { mutableStateOf(initialSchedule.transportation) }
    var details by remember { mutableStateOf(initialSchedule.details) }
    var showDatePicker by remember { mutableStateOf(false) }

    val weatherInfo by weatherViewModel.weatherInfo.collectAsState()

    // 장소 검색용 상태
    var query by remember { mutableStateOf(location) }
    var predictions by remember { mutableStateOf(emptyList<PlaceResult>()) }
    var expanded by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val apiKey = getApiKeyFromManifest()

    // 🔍 검색어 입력 시 자동완성 요청
    LaunchedEffect(query) {
        if (!isEditing) return@LaunchedEffect
        if (query.length >= 2) {
            try {
                predictions = fetchPlaceAutocomplete3(query, apiKey)
                expanded = predictions.isNotEmpty()
            } catch (_: Exception) {
                predictions = emptyList()
                expanded = false
            }
        } else {
            predictions = emptyList()
            expanded = false
        }
    }

    // 📡 날짜 or 위치 변경 시 날씨 정보 요청
    LaunchedEffect(date, latitude, longitude) {
        val (nx, ny) = latLonToGrid(latitude, longitude)
        weatherViewModel.fetchWeatherForDateAndLocation(date, nx, ny)
    }

    /**
     * 일정 수정 저장 처리
     */
    fun onSave(updated: ScheduleData, cityDocId: String) {
        scheduleViewModel.updateSchedule(updated, cityDocId) { success ->
            Log.d("Schedule", if (success) "업데이트 성공" else "업데이트 실패")
        }
    }

    /**
     * 일정 정보를 PDF로 내보내기
     */
    fun onExport() {
        val info = """
            📍 위치: $location
            📅 날짜: $date
            🚗 이동 수단: ${transportation.displayName}
            🌤️ 날씨 상태: ${weatherInfo?.status ?: "알 수 없음"}
            🌡️ 기온: ${weatherInfo?.temperature ?: "알 수 없음"} ℃
            📝 상세 정보: $details
        """.trimIndent()
        exportPdfAndShare(context, "Trip_Info_${date}.pdf", info)
    }

    // 🧾 UI 시작
    Card(
        modifier = Modifier.fillMaxWidth(0.9f).padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 상단 버튼 영역 (수정 / 내보내기)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(
                    onClick = {
                        isEditing = !isEditing
                        if (isEditing) {
                            // 편집 완료 → 저장
                            onSave(
                                ScheduleData(
                                    id = selectedSchedule.id,
                                    location = location,
                                    latitude = latitude,
                                    longitude = longitude,
                                    date = date.toString(),
                                    transportation = transportation,
                                    details = details
                                ),
                                cityDocId
                            )
                        }
                        // UI 리셋
                        expanded = false
                        if (!isEditing) query = location
                    },
                    colors = ButtonDefaults.textButtonColors(
                        containerColor = Color.LightGray,
                        contentColor = Color.Black
                    )
                ) {
                    Text(if (isEditing) "수정 완료" else "수정", fontSize = 14.sp)
                }

                TextButton(
                    onClick = { onExport() },
                    colors = ButtonDefaults.textButtonColors(
                        containerColor = Color.LightGray,
                        contentColor = Color.Black
                    )
                ) {
                    Text("내보내기", fontSize = 14.sp)
                }
            }

            Spacer(Modifier.height(8.dp))

            // 위치 입력 및 자동완성
            OutlinedTextField(
                value = if (isEditing) query else location,
                onValueChange = {
                    if (isEditing) {
                        query = it
                        location = it
                    }
                },
                label = { Text("위치", fontSize = 12.sp, color = Color.Gray) },
                leadingIcon = { Icon(Icons.Default.Place, contentDescription = null) },
                enabled = isEditing,
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp)
            )

            DropdownMenu(
                expanded = expanded && isEditing,
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                predictions.forEach { prediction ->
                    DropdownMenuItem(
                        text = { Text(prediction.name) },
                        onClick = {
                            location = prediction.name
                            query = location
                            expanded = false
                            coroutineScope.launch {
                                fetchPlaceDetails2(prediction.placeId, apiKey)?.let {
                                    latitude = it.first
                                    longitude = it.second
                                }
                            }
                        }
                    )
                }
            }

            // 날짜 선택
            OutlinedTextField(
                value = date.toString(),
                onValueChange = {},
                label = { Text("날짜", fontSize = 12.sp, color = Color.Gray) },
                leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
                enabled = false,
                readOnly = true,
                modifier = Modifier.fillMaxWidth().then(
                    if (isEditing) Modifier.clickable { showDatePicker = true } else Modifier
                ),
                textStyle = TextStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp)
            )

            if (showDatePicker) {
                DatePickerDialog(
                    initialDate = date,
                    onDismissRequest = { showDatePicker = false },
                    onDateChange = {
                        date = it
                        showDatePicker = false
                    }
                )
            }

            // 이동 수단 선택
            DropdownMenuBox(
                selected = transportation,
                onSelect = { if (isEditing) transportation = it },
                enabled = isEditing
            )

            // 날씨 상태
            OutlinedTextField(
                value = weatherInfo?.status ?: "",
                onValueChange = {},
                label = { Text("날씨 상태") },
                leadingIcon = { Icon(Icons.Default.WbSunny, contentDescription = null) },
                enabled = false,
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp)
            )

            // 기온
            OutlinedTextField(
                value = weatherInfo?.temperature?.toString() ?: "",
                onValueChange = {},
                label = { Text("기온 (℃)") },
                leadingIcon = { Icon(Icons.Default.Thermostat, contentDescription = null) },
                enabled = false,
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp)
            )

            // 상세 정보
            OutlinedTextField(
                value = details,
                onValueChange = { if (isEditing) details = it },
                label = { Text("상세 정보", fontSize = 12.sp, color = Color.Gray) },
                leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) },
                enabled = isEditing,
                modifier = Modifier.fillMaxWidth().height(100.dp),
                textStyle = TextStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp)
            )

            // 닫기 버튼
            Button(
                onClick = { navController.popBackStack() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray)
            ) {
                Text("닫기")
            }
        }
    }
}
