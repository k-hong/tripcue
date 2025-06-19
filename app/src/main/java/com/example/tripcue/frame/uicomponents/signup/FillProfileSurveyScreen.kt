package com.example.tripcue.frame.uicomponents.signup

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun FillProfileSurveyScreen(navController: NavController) {
    val user = FirebaseAuth.getInstance().currentUser
    val db = FirebaseFirestore.getInstance()

    var nickname by remember { mutableStateOf("") }
    var region by remember { mutableStateOf("") }
    var selectedTags by remember { mutableStateOf(listOf<String>()) }

    val interestOptions = listOf(
        "느긋한", "관광지", "식당", "사진", "쇼핑", "휴양",
        "모험", "자연", "역사", "전통", "문화", "야경"
    )

    Column(modifier = Modifier.padding(24.dp)) {
        OutlinedTextField(
            value = nickname,
            onValueChange = { nickname = it },
            label = { Text("닉네임") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = region,
            onValueChange = { region = it },
            label = { Text("지역") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text("관심사 선택")
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .height(300.dp)
                .padding(8.dp)
        ) {
            items(interestOptions) { tag ->
                val selected = tag in selectedTags
                FilterChip(
                    selected = selected,
                    onClick = {
                        selectedTags = if (selected) {
                            selectedTags - tag
                        } else if (selectedTags.size < 4) {
                            selectedTags + tag
                        } else {
                            selectedTags
                        }
                    }
                    ,
                    label = { Text(tag) },
                    modifier = Modifier.padding(4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = {
            user?.uid?.let { uid ->
                db.collection("users").document(uid)
                    .update(
                        mapOf(
                            "nickname" to nickname,
                            "region" to region,
                            "interests" to selectedTags
                        )
                    )
                    .addOnSuccessListener {
                        navController.navigate("home") {
                            popUpTo("fill_profile_survey") { inclusive = true }
                        }
                    }
            }
        }) {
            Text("저장하고 시작하기")
        }
    }
}
