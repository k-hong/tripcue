package com.example.tripcue.frame.uicomponents.Schedule

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.tripcue.frame.model.Transportation

/**
 * 커스텀 DropdownMenu 컴포저블 - 이동 수단 선택을 위한 드롭다운 메뉴 UI
 *
 * @param selected 현재 선택된 이동 수단
 * @param onSelect 항목 선택 시 호출되는 콜백
 * @param enabled 드롭다운 활성화 여부
 */
@Composable
fun DropdownMenuBox(
    selected: Transportation,                      // 현재 선택된 이동 수단
    onSelect: (Transportation) -> Unit,            // 사용자가 선택 시 실행할 콜백
    enabled: Boolean                               // 클릭 가능 여부
) {
    // 메뉴 확장 여부 상태
    var expanded by remember { mutableStateOf(false) }

    Column {
        // 클릭 가능한 영역(전체 너비 박스)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (enabled) Modifier.clickable { expanded = true }
                    else Modifier // 비활성화 상태일 경우 클릭 비활성
                )
        ) {
            // 선택된 이동 수단을 표시하는 OutlinedTextField
            OutlinedTextField(
                value = selected.displayName,           // 선택된 값의 displayName 표시
                onValueChange = {},                     // 직접 입력은 불가능
                label = { Text("이동 수단", fontSize = 12.sp, color = Color.Gray) },
                readOnly = true,                        // 입력 불가
                enabled = false,                        // 스타일상 비활성처럼 보이도록 설정
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.Black
                )
            )
        }

        // 드롭다운 메뉴: expanded가 true일 때만 표시
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }     // 바깥 클릭 시 닫기
        ) {
            // 이동 수단 목록 반복 렌더링
            Transportation.entries.forEach { transport ->
                DropdownMenuItem(
                    text = {
                        Text(
                            transport.displayName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.Black
                        )
                    },
                    onClick = {
                        onSelect(transport)             // 선택된 항목 콜백 호출
                        expanded = false                // 메뉴 닫기
                    }
                )
            }
        }
    }
}
