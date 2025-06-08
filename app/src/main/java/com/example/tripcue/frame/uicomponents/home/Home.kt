// ✅ Home.kt (바텀바 제거 완료)
package com.example.tripcue.frame.uicomponents.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

@Composable
fun Home() {
    val user = FirebaseAuth.getInstance().currentUser
    val db = FirebaseFirestore.getInstance()

    var nickname by remember { mutableStateOf("") }
    var region by remember { mutableStateOf("") }
    var interests by remember { mutableStateOf(listOf<String>()) }
    var recommendedPlaces by remember { mutableStateOf(listOf<String>()) }

    LaunchedEffect(user?.uid) {
        user?.uid?.let { uid ->
            val doc = db.collection("users").document(uid).get().await()
            nickname = doc.getString("nickname") ?: "사용자"
            region = doc.getString("region") ?: "서울"
            interests = doc.get("interests") as? List<String> ?: emptyList()

            recommendedPlaces = getRecommendedPlaces(region, interests)
        }
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {

        Text(text = "$nickname", fontSize = 22.sp)
        Text(text = "여행을 떠날 준비되셨나요?", fontSize = 18.sp)
        Spacer(modifier = Modifier.height(16.dp))

        Box(modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .background(Color.LightGray)) {
            Text("지도 영역 (관심지역: $region)", modifier = Modifier.align(Alignment.Center))
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .background(Color(0xFFD0F0C0))) {
            Column(modifier = Modifier.padding(8.dp)) {
                Text("추천 여행지", fontSize = 16.sp)
                recommendedPlaces.take(3).forEach {
                    Text(text = "- $it")
                }
            }
        }
    }
}
