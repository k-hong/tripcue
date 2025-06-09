package com.example.tripcue.frame.uicomponents.home

import android.util.Log
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

    val TAG = "TripcueLog"

    var nickname by remember { mutableStateOf("") }
    var region by remember { mutableStateOf("") }
    var interests by remember { mutableStateOf(listOf<String>()) }
    var recommendedPlaces by remember { mutableStateOf(listOf<String>()) }
    var hotPlaces by remember { mutableStateOf(listOf<String>()) }

    LaunchedEffect(user?.uid) {
        Log.d(TAG, "🔥 사용자 UID: ${user?.uid}")

        user?.uid?.let { uid ->
            try {
                val doc = db.collection("users").document(uid).get().await()
                nickname = doc.getString("nickname") ?: "사용자"
                region = doc.getString("region") ?: "서울"
                interests = doc.get("interests") as? List<String> ?: emptyList()
                Log.d(TAG, "👤 Firestore 데이터: region=$region, interests=$interests")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Firestore 사용자 정보 가져오기 실패", e)
            }

            scope.launch {
                try {
                    if (interests.isNotEmpty()) {
                        recommendedPlaces = NaverPlaceApi.searchPlaces(region, interests)
                        Log.d(TAG, "✅ 추천 장소: $recommendedPlaces")
                    }
                    hotPlaces = NaverPlaceApi.getHotPlaces(region)
                    Log.d(TAG, "✅ 핫플레이스: $hotPlaces")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ 네이버 API 실패", e)
                }
            }
        }
    }

    if (recommendedPlaces.isEmpty() && hotPlaces.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("로딩 중 또는 장소가 없습니다.")
        }
    } else {
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)) {

            Text(text = "$nickname 님", fontSize = 22.sp)
            Text(text = "여행을 떠날 준비되셨나요?", fontSize = 18.sp)
            Spacer(modifier = Modifier.height(16.dp))

            recommendedPlaces.take(2).forEachIndexed { index, place ->
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .padding(vertical = 4.dp)
                    .background(Color(0xFFE0F7FA))) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("추천 관심 여행지 ${index + 1}", fontSize = 16.sp)
                        Text("- $place", fontSize = 14.sp)
                    }
                }
            }

            hotPlaces.take(2).forEachIndexed { index, place ->
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .padding(vertical = 4.dp)
                    .background(Color(0xFFFFF9C4))) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("지도 기반 핫플레이스 ${index + 1}", fontSize = 16.sp)
                        Text("- $place", fontSize = 14.sp)
                    }
                }
            }
        }
    }
}
