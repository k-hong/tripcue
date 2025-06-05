package com.example.tripcue.frame.uicomponents.Schedule

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tripcue.frame.model.ScheduleData
import com.example.tripcue.frame.model.Transportation
import com.example.tripcue.frame.model.WeatherInfo
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoCardScreen(
    initialData: ScheduleData,
    onUpdate: (ScheduleData) -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var date by remember { mutableStateOf(LocalDate.parse(initialData.date)) }
    var location by remember { mutableStateOf(initialData.location) }
    var transportation by remember { mutableStateOf(initialData.transportation) }
    var weatherStatus by remember { mutableStateOf(initialData.weather?.status ?: "") }
    var temperature by remember { mutableStateOf(initialData.weather?.temperature?.toString() ?: "") }
    var details by remember { mutableStateOf(initialData.details) }

    var showDatePicker by remember { mutableStateOf(false) }

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
                            // 수정 완료
                            onUpdate(
                                ScheduleData(
                                    date = date.toString(),
                                    location = location,
                                    transportation = transportation,
                                    weather = WeatherInfo(
                                        status = weatherStatus,
                                        temperature = temperature.toDoubleOrNull() ?: 0.0
                                    ),
                                    details = details
                                )
                            )
                        }
                        isEditing = !isEditing
                    },
                    modifier = Modifier
                        .padding(4.dp),
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                        containerColor = Color.LightGray,
                        contentColor = Color.Black
                    )
                ) {
                    Text(
                        text = if (isEditing) "수정 완료하기" else "수정",
                        fontSize = 12.sp
                    )
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
                    .then(
                        if (isEditing) Modifier.clickable { showDatePicker = true }
                        else Modifier
                    )
            ) {
                OutlinedTextField(
                    value = date.toString(),
                    onValueChange = {},
                    label = { Text("날짜", fontSize = 12.sp, color = Color.Gray) },
                    readOnly = true,
                    enabled = false, // 아예 수정 불가하게 보이도록
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
                    initialDate = date,  // 반드시 넘겨줘야 함!
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
                value = weatherStatus,
                onValueChange = { weatherStatus = it },
                label = { Text("날씨 상태", fontSize = 12.sp, color = Color.Gray) },
                enabled = isEditing,
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    fontSize = 14.sp
                )
            )

            OutlinedTextField(
                value = temperature,
                onValueChange = { temperature = it },
                label = { Text("기온 (℃)", fontSize = 12.sp, color = Color.Gray) },
                enabled = isEditing,
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

@Preview(showBackground = true)
@Composable
fun InfoCardScreenPreview() {
    InfoCardScreen(
        initialData = ScheduleData(
            date = "2025-06-05",
            location = "서울",
            transportation = Transportation.BUS,
            weather = WeatherInfo("맑음", 23.5),
            details = "출근길 상세 정보"
        ),
        onUpdate = { /* Preview용 */ }
    )
}
