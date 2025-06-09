package com.example.tripcue.frame.uicomponents.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun Home() {
    val user = FirebaseAuth.getInstance().currentUser
    val db = FirebaseFirestore.getInstance()
    val scope = rememberCoroutineScope()

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

            if (interests.isNotEmpty()) {
                scope.launch {
                    recommendedPlaces = NaverPlaceApi.searchPlaces(region, interests)
                }
            }
        }
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {

        Text(text = "$nickname 님", fontSize = 22.sp)
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
            .height(150.dp)
            .background(Color(0xFFD0F0C0))) {
            Column(modifier = Modifier.padding(8.dp)) {
                Text("추천 여행지", fontSize = 16.sp)
                recommendedPlaces.forEach {
                    Text("- $it", fontSize = 14.sp)
                }
            }
        }
    }
}
