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

@Composable
fun DropdownMenuBox(
    selected: Transportation,
    onSelect: (Transportation) -> Unit,
    enabled: Boolean
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (enabled) Modifier.clickable { expanded = true } else Modifier)
        ) {
            OutlinedTextField(
                value = selected.displayName,
                onValueChange = {},
                label = { Text("이동 수단", fontSize = 12.sp, color = Color.Gray) },
                readOnly = true,
                enabled = false, // 비활성처럼 보이게
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.Black
                )
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
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
                        onSelect(transport)
                        expanded = false
                    }
                )
            }
        }
    }
}
