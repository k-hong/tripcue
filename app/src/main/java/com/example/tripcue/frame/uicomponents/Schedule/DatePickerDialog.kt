package com.example.tripcue.frame.uicomponents.Schedule

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.DatePicker
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.DialogProperties
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerDialog(
    initialDate: LocalDate,
    onDismissRequest: () -> Unit,
    onDateChange: (LocalDate) -> Unit
) {
    val initialDateMillis = initialDate.atStartOfDay(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()

    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialDateMillis)

    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = {
                datePickerState.selectedDateMillis?.let {
                    val selectedDate = Instant.ofEpochMilli(it)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                    onDateChange(selectedDate)
                }
            }) {
                Text("확인")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("취소")
            }
        },
        text = {
            DatePicker(state = datePickerState)
        },
        properties = DialogProperties()
    )
}