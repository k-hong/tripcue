package com.example.tripcue.frame.uicomponents.Schedule

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tripcue.frame.model.ScheduleData
import com.example.tripcue.frame.model.WeatherInfo
import com.example.tripcue.frame.viewmodel.WeatherViewModel
import java.time.LocalDate

// 테스트용
fun convertLocationToGrid(location: String): Pair<Int, Int> {
    return when (location) {
        "서울" -> 60 to 127
        "부산" -> 98 to 76
        // 실제 변환 로직 또는 Map DB 필요
        else -> 60 to 127
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoCardScreen(
    initialData: ScheduleData,
    onUpdate: (ScheduleData) -> Unit
) {
    val viewModel: WeatherViewModel = viewModel()
    val weatherInfo by viewModel.weatherInfo.collectAsState()

    var isEditing by remember { mutableStateOf(false) }
    var date by remember { mutableStateOf(LocalDate.parse(initialData.date)) }
    var location by remember { mutableStateOf(initialData.location) }
    var transportation by remember { mutableStateOf(initialData.transportation) }
    var details by remember { mutableStateOf(initialData.details) }

    var showDatePicker by remember { mutableStateOf(false) }

    // 위치 → nx, ny 변환 필요, 예시로 임의값 사용
    val (nx, ny) = convertLocationToGrid(location)

    // 날짜 변경되거나 위치가 바뀌면 날씨 갱신
    LaunchedEffect(date, nx, ny) {
        viewModel.fetchWeatherForDateAndLocation(date, nx, ny)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth(0.7f)
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(
                    onClick = {
                        if (isEditing) {
                            onUpdate(
                                ScheduleData(
                                    date = date.toString(),
                                    location = location,
                                    transportation = transportation,
                                    weather = weatherInfo ?: WeatherInfo("정보 없음", 0.0),
                                    details = details
                                )
                            )
                        }
                        isEditing = !isEditing
                    },
                    modifier = Modifier.padding(4.dp),
                    colors = ButtonDefaults.textButtonColors(
                        containerColor = Color.LightGray,
                        contentColor = Color.Black
                    )
                ) {
                    Text(text = if (isEditing) "수정 완료하기" else "수정", fontSize = 12.sp)
                }
            }

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("위치", fontSize = 12.sp, color = Color.Gray) },
                enabled = isEditing,
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    fontSize = 14.sp
                )
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (isEditing) Modifier.clickable { showDatePicker = true } else Modifier)
            ) {
                OutlinedTextField(
                    value = date.toString(),
                    onValueChange = {},
                    label = { Text("날짜", fontSize = 12.sp, color = Color.Gray) },
                    readOnly = true,
                    enabled = false,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        fontSize = 14.sp
                    )
                )
            }

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
                onSelect = { transportation = it },
                enabled = isEditing
            )

            OutlinedTextField(
                value = weatherInfo?.status ?: "",
                onValueChange = {},
                label = { Text("날씨 상태") },
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
                onValueChange = { details = it },
                label = { Text("상세 정보", fontSize = 12.sp, color = Color.Gray) },
                enabled = isEditing,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                textStyle = TextStyle(
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    fontSize = 14.sp
                )
            )
        }
    }
}
