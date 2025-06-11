package com.example.tripcue.frame.uicomponents.Schedule

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
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

// 테스트용
fun convertLocationToGrid(location: String): Pair<Int, Int> {
    return when (location) {
        "서울" -> 60 to 127
        "부산" -> 98 to 76
        else -> 60 to 127
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoCardScreen(
    navController: NavController,
) {
    val context = LocalContext.current
    val weatherViewModel: WeatherViewModel = viewModel()
    val scheduleViewModel: ScheduleViewModel = viewModel()
    // val selectedSchedule by scheduleViewModel.selectedSchedule.collectAsState()
    val selectedSchedule = navController.previousBackStackEntry
        ?.savedStateHandle
        ?.get<ScheduleData>("selectedSchedule")

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

    var location by remember { mutableStateOf(initialSchedule.location) }
    var date by remember { mutableStateOf(parsedDate) }
    var transportation by remember { mutableStateOf(initialSchedule.transportation) }
    var details by remember { mutableStateOf(initialSchedule.details) }
    var showDatePicker by remember { mutableStateOf(false) }

    val weatherInfo by weatherViewModel.weatherInfo.collectAsState()

    // 날짜 + 위치 변경 시 날씨 정보 가져오기
    LaunchedEffect(location, date) {
        if (location.isNotBlank()) {
            val (nx, ny) = convertLocationToGrid(location)
            weatherViewModel.fetchWeatherForDateAndLocation(date, nx, ny)
        }
    }

    fun onSave() {
        val updatedSchedule = initialSchedule.copy(
            location = location,
            date = date.toString(),
            transportation = transportation,
            details = details
        )
        scheduleViewModel.updateSchedule(updatedSchedule)
        navController.popBackStack()
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
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = { onExport() },
                    colors = ButtonDefaults.textButtonColors(
                        containerColor = Color.LightGray,
                        contentColor = Color.Black
                    )
                ) {
                    Text("내보내기", fontSize = 14.sp)
                }
                TextButton(
                    onClick = {
                        if (isEditing) onSave()
                        isEditing = !isEditing
                    },
                    colors = ButtonDefaults.textButtonColors(
                        containerColor = Color.LightGray,
                        contentColor = Color.Black
                    )
                ) {
                    Text(text = if (isEditing) "수정 완료" else "수정", fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = location,
                onValueChange = { if (isEditing) location = it },
                label = { Text("위치", fontSize = 12.sp, color = Color.Gray) },
                leadingIcon = { Icon(Icons.Default.Place, contentDescription = "위치 아이콘") },
                enabled = isEditing,
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp)
            )

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
    }
}