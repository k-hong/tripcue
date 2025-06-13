package com.example.tripcue.frame.uicomponents.Schedule

import android.app.Activity
import android.util.Log
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

const val DEFAULT_LAT = 37.5665  // 서울 위도
const val DEFAULT_LNG = 126.9780 // 서울 경도

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
        val googlePlaceId = prediction.getString("place_id") // ✅ Google의 place_id

        val latLng = fetchPlaceDetails(googlePlaceId, apiKey) ?: return@mapNotNull null

        // Firestore용 고유 ID 생성
        val db = Firebase.firestore
        val newDocRef = db.collection("places").document()
        val firebaseDocId = newDocRef.id

        val placeResult = PlaceResult(firebaseDocId, description, latLng.first, latLng.second)

        // 비동기 Firestore 저장 (필요하면 await()으로 처리)
        newDocRef.set(placeResult).await()

        placeResult
    }
}

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

fun latLonToGrid(lat: Double, lon: Double): Pair<Int, Int> {
    val RE = 6371.00877 // 지구 반경(km)
    val GRID = 5.0      // 격자 간격(km)
    val SLAT1 = 30.0    // 투영 위도1(degree)
    val SLAT2 = 60.0    // 투영 위도2(degree)
    val OLON = 126.0    // 기준점 경도(degree)
    val OLAT = 38.0     // 기준점 위도(degree)
    val XO = 43         // 기준점 X좌표(GRID)
    val YO = 136        // 기준점 Y좌표(GRID)

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoCardScreen(
    navController: NavController,
    cityDocId: String
) {
    val context = LocalContext.current
    val weatherViewModel: WeatherViewModel = viewModel()
    val scheduleViewModel: ScheduleViewModel = viewModel()
    // val selectedSchedule by scheduleViewModel.selectedSchedule.collectAsState()
    val selectedSchedule = navController.previousBackStackEntry
        ?.savedStateHandle
        ?.get<ScheduleData>("selectedSchedule")

    val activity = LocalActivity.current as ComponentActivity
    var location by remember { mutableStateOf(selectedSchedule?.location ?: "") }
    var latitude by remember { mutableStateOf(selectedSchedule?.latitude ?: DEFAULT_LAT) }
    var longitude by remember { mutableStateOf(selectedSchedule?.longitude ?: DEFAULT_LNG) }


    if (selectedSchedule == null) {
        Text("선택된 일정이 없습니다.")
        return
    }

    val initialSchedule = selectedSchedule!!

    var isEditing by remember { mutableStateOf(false) }

    val parsedDate = try {
        LocalDate.parse(initialSchedule.date)
    } catch (e: DateTimeParseException) {
        LocalDate.now()
    }

    var date by remember { mutableStateOf(parsedDate) }
    var transportation by remember { mutableStateOf(initialSchedule.transportation) }
    var details by remember { mutableStateOf(initialSchedule.details) }
    var showDatePicker by remember { mutableStateOf(false) }

    val weatherInfo by weatherViewModel.weatherInfo.collectAsState()

    var query by remember { mutableStateOf(location) }
    var predictions by remember { mutableStateOf(listOf<PlaceResult>()) }
    var expanded by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    val apiKey = getApiKeyFromManifest()

    // 검색어가 2글자 이상일 때 예측 결과 가져오기
    LaunchedEffect(query) {
        if (!isEditing) return@LaunchedEffect
        if (query.length >= 2) {
            try {
                val results = fetchPlaceAutocomplete3(query, apiKey)
                predictions = results
                expanded = predictions.isNotEmpty()
            } catch (e: Exception) {
                predictions = emptyList()
                expanded = false
            }
        } else {
            predictions = emptyList()
            expanded = false
        }
    }

    // 날짜 + 위치 변경 시 날씨 정보 가져오기
    LaunchedEffect(date, latitude, longitude) {
        val (nx, ny) = latLonToGrid(latitude, longitude)
        weatherViewModel.fetchWeatherForDateAndLocation(date, nx, ny)
    }

    fun onSave(updatedSchedule: ScheduleData, cityDocId: String) {
        Log.d("Caller", "업데이트 스케줄 호출 시도")
        scheduleViewModel.updateSchedule(updatedSchedule, cityDocId) { success ->
            if (success) {
                // 업데이트 성공 시 UI 처리, 예: 화면 닫기, 토스트 메시지 띄우기 등
                Log.d("Schedule", "스케줄 업데이트 성공")
            } else {
                // 실패 시 처리, 예: 에러 메시지 표시
                Log.d("Schedule", "스케줄 업데이트 실패")
            }
        }
    }

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

    Card(
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(
                    onClick = {
                        isEditing = !isEditing
                        Log.d("Debug", "isEditing before toggle: $isEditing")
                        Log.d("Debug", "isEditing before toggle: $selectedSchedule.id")
                        if (isEditing) {
                            val updatedSchedule = ScheduleData(
                                id = selectedSchedule.id,
                                location = location,
                                latitude = latitude,
                                longitude = longitude,
                                date = date.toString(),
                                transportation = transportation,
                                details = details
                            )
                            onSave(updatedSchedule, cityDocId)
                        }
                            // 편집 종료 시 자동완성 리스트 숨기기
                            expanded = false
                            // 편집 종료 시 선택된 장소 이름으로 쿼리 초기화
                            if (!isEditing) query = location
                    },
                    colors = ButtonDefaults.textButtonColors(
                        containerColor = Color.LightGray,
                        contentColor = Color.Black
                    )
                ) {
                    Text(text = if (isEditing) "수정 완료" else "수정", fontSize = 14.sp)
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

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = if (isEditing) query else location,
                onValueChange = {
                    if (isEditing) {
                        query = it
                        location = it
                    }
                },
                label = { Text("위치", fontSize = 12.sp, color = Color.Gray) },
                leadingIcon = { Icon(Icons.Default.Place, contentDescription = "위치 아이콘") },
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
                                try {
                                    val latLng = fetchPlaceDetails2(prediction.placeId, apiKey)
                                    latLng?.let {
                                        latitude = it.first
                                        longitude = it.second
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                    )
                }
            }

            OutlinedTextField(
                value = date.toString(),
                onValueChange = {},
                label = { Text("날짜", fontSize = 12.sp, color = Color.Gray) },
                leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = "날짜 아이콘") },
                enabled = false,
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (isEditing) Modifier.clickable { showDatePicker = true }
                        else Modifier
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

            DropdownMenuBox(
                selected = transportation,
                onSelect = { if (isEditing) transportation = it },
                enabled = isEditing
            )

            OutlinedTextField(
                value = weatherInfo?.status ?: "",
                onValueChange = {},
                label = { Text("날씨 상태") },
                leadingIcon = { Icon(Icons.Default.WbSunny, contentDescription = "날씨 상태 아이콘") },
                enabled = false,
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    fontSize = 14.sp
                )
            )

            OutlinedTextField(
                value = weatherInfo?.temperature?.toString() ?: "",
                onValueChange = {},
                label = { Text("기온 (℃)") },
                leadingIcon = { Icon(Icons.Default.Thermostat, contentDescription = "기온 아이콘") },
                enabled = false,
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    fontSize = 14.sp
                )
            )

            OutlinedTextField(
                value = details,
                onValueChange = { if (isEditing) details = it },
                label = { Text("상세 정보", fontSize = 12.sp, color = Color.Gray) },
                leadingIcon = { Icon(Icons.Default.Description, contentDescription = "상세 정보 아이콘") },
                enabled = isEditing,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                textStyle = TextStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp)
            )
        }
        Button(
            onClick = {
                navController.popBackStack()
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray)
        ) {
            Text("닫기")
        }
    }
}