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
    initialDate: LocalDate,                     // 초기 날짜 (기본 선택값)
    onDismissRequest: () -> Unit,               // 다이얼로그 닫기 콜백
    onDateChange: (LocalDate) -> Unit           // 날짜가 선택되었을 때 호출되는 콜백
) {
    // 초기 날짜를 milliseconds(UTC 기준)로 변환
    val initialDateMillis = initialDate.atStartOfDay(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()

    // DatePicker 상태 저장 (선택된 날짜를 기억함)
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialDateMillis)

    // AlertDialog를 사용하여 DatePicker 다이얼로그 구성
    AlertDialog(
        onDismissRequest = onDismissRequest,    // 바깥을 눌렀을 때 또는 취소 시 실행
        confirmButton = {
            TextButton(
                onClick = {
                    // 선택된 날짜가 존재할 경우 처리
                    datePickerState.selectedDateMillis?.let {
                        // milliseconds → LocalDate로 변환
                        val selectedDate = Instant.ofEpochMilli(it)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                        onDateChange(selectedDate) // 선택된 날짜 전달
                    }
                }
            ) {
                Text("확인")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("취소")
            }
        },
        text = {
            // Material3의 DatePicker 컴포넌트 표시
            DatePicker(state = datePickerState)
        },
        properties = DialogProperties() // 기본 다이얼로그 속성 사용
    )
}
