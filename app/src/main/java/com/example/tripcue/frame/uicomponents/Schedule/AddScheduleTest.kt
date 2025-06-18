package com.example.tripcue.frame.uicomponents.Schedule

import android.util.Log
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
import java.util.UUID

suspend fun fetchPlaceAutocomplete2(query: String, apiKey: String): List<PlaceResult> =
    withContext(Dispatchers.IO) {
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
//            val db = Firebase.firestore
//            val newDocRef = db.collection("places").document()
//            val firebaseDocId = newDocRef.id
//
//            val placeResult = PlaceResult(firebaseDocId, description, latLng.first, latLng.second)
//
//            // 비동기 Firestore 저장 (필요하면 await()으로 처리)
//            newDocRef.set(placeResult).await()
//
//            placeResult
            PlaceResult("", description, latLng.first, latLng.second)
        }
    }

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
    cityDocId: String   // 도시 문서 ID 추가
) {
    val scheduleViewModel: ScheduleViewModel = viewModel()
    val errorMessage by scheduleViewModel.errorMessage.collectAsState()
    val sharedScheduleViewModel: SharedScheduleViewModel = viewModel()

    var location by remember { mutableStateOf("") }
//    var date by remember { mutableStateOf(LocalDate.now()) }
    var transportation by remember { mutableStateOf(Transportation.BUS) }
    var details by remember { mutableStateOf("") }
    var selectedLatLng by remember { mutableStateOf<Pair<Double, Double>?>(null) }

    var showDatePicker by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }

    var locationQuery by remember { mutableStateOf("") }
    var suggestions by remember { mutableStateOf<List<PlaceResult>>(emptyList()) }
    var expanded by remember { mutableStateOf(false) }

    val apiKey = getApiKeyFromManifest()

    LaunchedEffect(locationQuery) {
        if (locationQuery.length < 2) {
            suggestions = emptyList()
            expanded = false
            return@LaunchedEffect
        }

        suggestions = fetchPlaceAutocomplete2(locationQuery, apiKey)
        expanded = suggestions.isNotEmpty()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("새 일정 추가", style = MaterialTheme.typography.titleLarge)

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = if (location.isNotEmpty()) location else locationQuery,
            onValueChange = {
                locationQuery = it
                location = ""
            },
            label = { Text("위치") },
            modifier = Modifier.fillMaxWidth()
        )

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

        if (showDatePicker) {
            DatePickerDialog(
                initialDate = selectedDate,
                onDismissRequest = { showDatePicker = false },
                onDateChange = {
                    selectedDate = it
                    showDatePicker = false
                }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        DropdownMenuBox(
            selected = transportation,
            onSelect = { transportation = it },
            enabled = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = details,
            onValueChange = { details = it },
            label = { Text("상세 정보") },
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (errorMessage != null) {
            Text(
                text = errorMessage!!,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        Button(
            onClick = {
                val scheduleId = UUID.randomUUID().toString()
                val finalLocation = if (location.isNotEmpty()) location else locationQuery
                val newSchedule = ScheduleData(
                    id = scheduleId,
                    location = finalLocation,
                    date = selectedDate.toString(),
                    transportation = transportation,
                    weather = null, // AddSchedule 단계에서는 날씨 정보 없음
                    details = details,
                    latitude = selectedLatLng?.first,
                    longitude = selectedLatLng?.second
                )
                scheduleViewModel.addSchedule(newSchedule, cityDocId)
                // ✅ 새 ScheduleTitle 생성 (단일 데이터 포함)
                val scheduleTitle = ScheduleTitle(
                    id = cityDocId,
                    title = finalLocation, // 또는 다른 적절한 제목
                    location = finalLocation,
                    startDate = selectedDate.toString(),
                    endDate = selectedDate.toString()
                )

                // ✅ 공유 ViewModel에 설정
                sharedScheduleViewModel.setSchedule(scheduleTitle)

                // ✅ 이전 화면으로 돌아가기
                navController.navigate(Routes.InventSchedule.createRoute(cityDocId)) {
                    popUpTo(Routes.AddSchedule.route) { inclusive = true }
                }
            },
                modifier = Modifier.fillMaxWidth()
            ) {
            Text("등록하기")
        }

        Spacer(modifier = Modifier.height(8.dp))

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