package com.example.tripcue.frame.uicomponents

import android.content.pm.PackageManager
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import com.example.tripcue.frame.uicomponents.Schedule.DatePickerDialog
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.tripcue.frame.uicomponents.Schedule.DatePickerDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.LocalDate
import com.example.tripcue.frame.model.ScheduleTitle
import com.example.tripcue.frame.model.factory.Schedules.schedules
import androidx.core.util.Pair
import androidx.compose.material3.*
import androidx.compose.ui.text.style.TextOverflow
import java.time.Instant
import java.time.ZoneId
import androidx.compose.ui.text.style.TextAlign

var location = ""
var startDate: LocalDate = LocalDate.of(2000, 1, 1)
var endDate: LocalDate = LocalDate.of(2000, 1, 1)

@Composable
fun getApiKeyFromManifest(): String {
    val context = LocalContext.current
    return context.packageManager
        .getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
        .metaData
        .getString("com.google.android.geo.API_KEY") ?: ""
}

fun formatCountryRegionFromJson(prediction: JSONObject): String {
    val structured = prediction.getJSONObject("structured_formatting")
    val mainText = structured.getString("main_text")         // 지역명 (예: 서울)
    val secondaryText = structured.getString("secondary_text") // 국가명 (예: 대한민국)
    return "$secondaryText - $mainText"                       // "대한민국 - 서울" 형태로 반환
}


suspend fun fetchPlaceAutocomplete(input: String, apiKey: String): List<String> = withContext(
    Dispatchers.IO) {
    // API 요청 URL 만들기 (입력값 인코딩, 지역/국가 검색 필터 포함)
    val urlString = "https://maps.googleapis.com/maps/api/place/autocomplete/json?" +
            "input=${URLEncoder.encode(input, "UTF-8")}&" +
            "types=(regions)&" +
            "language=ko&" +   // 국가/지역 중심으로 검색
            "key=$apiKey"

    val url = URL(urlString)
    val connection = url.openConnection() as HttpURLConnection

    try {
        connection.requestMethod = "GET"       // GET 방식 요청
        connection.connectTimeout = 5000       // 연결 제한시간 5초
        connection.readTimeout = 5000          // 읽기 제한시간 5초

        val response = connection.inputStream.bufferedReader().use { it.readText() }

        val jsonObject = JSONObject(response)   // 응답 JSON 파싱
        val predictions = jsonObject.getJSONArray("predictions")  // 자동완성 결과 배열 가져오기

        val results = mutableListOf<String>()
        for (i in 0 until predictions.length()) {
            val prediction = predictions.getJSONObject(i)
            // 국가 - 지역 형태 문자열로 변환 후 리스트에 추가
            results.add(formatCountryRegionFromJson(prediction))
        }

        results     // 최종 결과 리스트 반환
    } catch (e: Exception) {
        e.printStackTrace()
        emptyList() // 에러 발생 시 빈 리스트 반환
    } finally {
        connection.disconnect()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSchedule(  onDone: () -> Unit) { //스케쥴 타이틀 추가 함수
    var showLocation by remember { mutableStateOf(true) }
    var showCard by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedlocation by remember { mutableStateOf("") }
    var isSelected by remember { mutableStateOf(false) }
    var selectedIndex by remember { mutableStateOf(-1) }
    val coroutineScope = rememberCoroutineScope()
    val apiKey = getApiKeyFromManifest()
    val state = rememberDateRangePickerState()


    Box(modifier = Modifier.fillMaxSize()) {
        LaunchedEffect(Unit) {
            showCard = true
        }


        AnimatedVisibility(
            visible = showCard,
            enter = slideInVertically(
                initialOffsetY = { fullHeight -> fullHeight },
            ),
            exit = slideOutVertically(
                targetOffsetY = { fullHeight -> fullHeight },
            ),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(750.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    if(showLocation){
                        OutlinedTextField(
                            value = text,
                            onValueChange = { newText ->
                                text = newText

                                if (newText.length >= 2) {

                                    coroutineScope.launch {
                                        Log.d("Autocomplete", "입력한 텍스트: $newText")
                                        val results = fetchPlaceAutocomplete(newText, apiKey)
                                        Log.d("Autocomplete", "결과: $results")
                                        Log.d("APIKEY", apiKey)
                                        searchResults = results
                                    }
                                } else {
                                    searchResults = emptyList()
                                }
                            },
                            label = { Text("어디로 떠나시나요?") },
                            modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // 검색 결과 목록
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 80.dp, bottom = 80.dp)
                        ) {
                            items(searchResults.take(5).withIndex().toList()) { (index,result) ->
                                val isThisSelected = (index == selectedIndex)
                                Text(
                                    text = result,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp)
                                        .background(
                                            color = if (isThisSelected) Color.LightGray else Color.Transparent
                                        )
                                        .clickable {
                                            selectedIndex = index
                                            isSelected = true
                                            selectedlocation = result
                                        }
                                )
                            }
                        }
                        Row(
                            modifier = Modifier
                                .padding(16.dp).align(Alignment.Center)
                            ){
                            Button(
                                onClick = { showCard = false },
                                modifier = Modifier.padding(8.dp)
                            ) {
                                Text("닫기")
                            }
                            Button(
                                enabled = isSelected,
                                onClick = {
                                    location = selectedlocation
                                    showDialog = true
                                },
                                modifier = Modifier.padding(8.dp)
                            ) {
                                Text("다음")
                            }
                            if (showDialog) {
                                DatePickerDialog(
                                    onDismissRequest = { showDialog = false },
                                    confirmButton = {
                                        TextButton(enabled = isSelected,onClick = {
                                            startDate = state.selectedStartDateMillis
                                                ?.let { millis ->
                                                    Instant.ofEpochMilli(millis)
                                                        .atZone(ZoneId.systemDefault())
                                                        .toLocalDate()
                                                }
                                                ?:startDate   // 선택이 없으면 기존 날짜 유지
                                            endDate = state.selectedEndDateMillis
                                                ?.let { millis ->
                                                    Instant.ofEpochMilli(millis)
                                                        .atZone(ZoneId.systemDefault())
                                                        .toLocalDate()
                                                }
                                                ?:  endDate // 선택이 없으면 기존 날짜 유지
                                            showDialog = false
                                            showLocation = false
                                            text = ""
                                        }) {
                                            Text("확인")
                                        }
                                    },

                                    dismissButton = {
                                        TextButton(onClick = { showDialog = false }) {
                                            Text("취소")
                                        }
                                    }
                                ) {
                                    DateRangePicker(
                                        state = state,
                                        title = {
                                            val titleText = when {
                                                state.selectedStartDateMillis == null && state.selectedEndDateMillis == null ->
                                                    "언제 떠나시나요?"
                                                state.selectedStartDateMillis != null && state.selectedEndDateMillis != null ->
                                                    "${Instant.ofEpochMilli(state.selectedStartDateMillis!!).atZone(ZoneId.systemDefault()).toLocalDate().toString()} ~ " +
                                                            "${Instant.ofEpochMilli(state.selectedEndDateMillis!!).atZone(ZoneId.systemDefault()).toLocalDate().toString()}"
                                                else -> "언제 떠나시나요?"
                                            }

                                            Text(
                                                text = titleText,
                                                modifier = Modifier
                                                    .fillMaxWidth()       // 텍스트 영역을 전체 넓이로 확장
                                                    .padding(16.dp),
                                                textAlign = TextAlign.Center
                                            )
                                        },
                                        headline = null,           // 줄 바뀜 방지 위해 헤드라인 제거
                                        showModeToggle = false     // 입력모드 토글 숨김
                                    )
                                }
                            }
                        }
                    }
                    else{
                        OutlinedTextField(
                            value = text,
                            onValueChange = { newText ->
                                text = newText
                                title = text
                            },
                            label = { Text("여행의 제목을 정해주세요!") },
                            modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter)
                        )
                        Row(
                            modifier = Modifier
                                .padding(16.dp).align(Alignment.Center)
                        ) {
                            Button(
                                onClick = {
                                    showCard = false
                                    EnrollSchedule(title, location, startDate, endDate)
                                    onDone()
                                },
                                modifier = Modifier.padding(8.dp)
                            ) {
                                Text("완료")
                            }
                        }
                    }
                }
            }
        }
    }
}

fun EnrollSchedule(title : String, location : String, startDate : LocalDate, endDate : LocalDate){
    schedules.add(ScheduleTitle(title, location,startDate, endDate))
}