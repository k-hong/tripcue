package com.example.tripcue.frame.uicomponents

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.tripcue.frame.model.factory.Schedules.schedules
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.tripcue.frame.model.Routes
import com.example.tripcue.frame.model.ScheduleTitle
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.time.LocalDate

@Composable
fun Schedules(navController: NavHostController) { //스케쥴 타이틀 카드 나열
    val userId = FirebaseAuth.getInstance().currentUser?.uid
    val db = FirebaseFirestore.getInstance()

    var schedules by remember { mutableStateOf<List<ScheduleTitle>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    // Firestore 실시간 리스너 등록 (useEffect 같은 역할)
    DisposableEffect(userId) {
        if (userId == null) {
            loading = false
            schedules = emptyList()
            return@DisposableEffect onDispose { }
        }

        val listenerRegistration: ListenerRegistration = db.collection("users")
            .document(userId)
            .collection("schedules")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // 에러 처리: 로그 출력 등
                    loading = false
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    schedules = snapshot.documents.mapNotNull { doc ->
                        val id = doc.id
                        val title = doc.getString("title") ?: return@mapNotNull null
                        val location = doc.getString("location") ?: ""
                        val startDateStr = doc.getString("startDate") ?: ""
                        val endDateStr = doc.getString("endDate") ?: ""

                        ScheduleTitle(
                            id = id,
                            title = title,
                            location = location,
                            startDate = LocalDate.parse(startDateStr),
                            endDate = LocalDate.parse(endDateStr)
                        )
                    }
                } else {
                    schedules = emptyList()
                }
                loading = false
            }

        onDispose {
            listenerRegistration.remove()
        }
    }

    if (loading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "로딩 중...", style = MaterialTheme.typography.bodyLarge)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            items(schedules, key = { it.id }) { schedule ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth().clickable {
                            // 클릭 시 selectedSchedule 키에 현재 스케줄을 저장하고 화면 이동
                            navController.currentBackStackEntry?.savedStateHandle?.set(
                                "selectedSchedule",
                                schedule
                            )
                            navController.navigate("${Routes.InventSchedule.route.replace("{cityDocId}", schedule.id)}")  // route 이름을 맞춰주세요
                        },
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            schedule.title,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .fillMaxWidth(),
                            fontSize = 20.sp
                        )
                        Divider(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp),
                            color = Color.White
                        )
                        Text("장소: ${schedule.location}")
                        Text("시작일: ${schedule.startDate}")
                        Text("종료일: ${schedule.endDate}")
                    }
                }
            }
        }
    }
}
