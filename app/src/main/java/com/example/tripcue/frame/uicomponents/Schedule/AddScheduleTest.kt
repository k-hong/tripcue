package com.example.tripcue.frame.uicomponents.Schedule

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.example.tripcue.frame.model.ScheduleData
import com.example.tripcue.frame.model.Transportation
import com.example.tripcue.frame.model.WeatherInfo
import com.example.tripcue.frame.viewmodel.ScheduleViewModel
import java.time.LocalDate

@Composable
fun AddScheduleTest(navController: NavHostController) {
    val scheduleViewModel: ScheduleViewModel = viewModel()
    val errorMessage by scheduleViewModel.errorMessage.collectAsState()

    var location by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(LocalDate.now()) }
    var transportation by remember { mutableStateOf(Transportation.BUS) }
    var details by remember { mutableStateOf("") }

    var showDatePicker by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("새 일정 추가", style = MaterialTheme.typography.titleLarge)

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = location,
            onValueChange = { location = it },
            label = { Text("위치") },
            modifier = Modifier.fillMaxWidth()
        )

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
                val newSchedule = ScheduleData(
                    location = location,
                    date = date.toString(),
                    transportation = transportation,
                    weather = null, // AddSchedule 단계에서는 날씨 정보 없음
                    details = details
                )
                scheduleViewModel.addSchedule(newSchedule)
                navController.popBackStack() // 뒤로가기 (InventoryScheduleTest로)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("등록하기")
        }
    }
}