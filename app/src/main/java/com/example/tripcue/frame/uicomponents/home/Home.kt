package com.example.tripcue.frame.uicomponents.home

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.tripcue.frame.model.Routes
import com.example.tripcue.frame.viewmodel.PlaceDetailViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun Home(
    navController: NavController,
    refreshTrigger: Boolean,
    viewModel: PlaceDetailViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val user = FirebaseAuth.getInstance().currentUser
    val db = FirebaseFirestore.getInstance()
    val scope = rememberCoroutineScope()

    val TAG = "TripcueLog"

    var nickname by remember { mutableStateOf("") }
    var region by remember { mutableStateOf("") }
    var interests by remember { mutableStateOf(listOf<String>()) }
    var recommendedPlaces by remember { mutableStateOf(listOf<PlaceInfo>()) }

    fun fetchPlaces() {
        scope.launch {
            try {
                val uid = user?.uid ?: return@launch
                val doc = db.collection("users").document(uid).get().await()
                nickname = doc.getString("nickname") ?: "사용자"
                region = doc.getString("region") ?: "서울"
                interests = doc.get("interests") as? List<String> ?: emptyList()
                Log.d(TAG, "👤 Firestore 데이터: region=$region, interests=$interests")

                if (interests.isNotEmpty()) {
                    val results = GooglePlaceApi.advancedSearchPlaces(
                        context = context,
                        region = region,
                        interests = interests,
                        totalLimit = 15
                    )
                    recommendedPlaces = results
                    Log.d(TAG, "✅ 추천 장소: $recommendedPlaces")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Firestore 사용자 정보 가져오기 실패", e)
            }
        }
    }

    LaunchedEffect(Unit) { fetchPlaces() }
    LaunchedEffect(refreshTrigger) { if (refreshTrigger) fetchPlaces() }

    if (recommendedPlaces.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("로딩 중 또는 관심 장소가 없습니다.")
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            item {
                Text(text = "$nickname 님", fontSize = 22.sp)
                Text(text = "여행을 떠날 준비되셨나요?", fontSize = 18.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Text("🔥 추천 관심 장소", fontSize = 16.sp, modifier = Modifier.padding(top = 8.dp))
            }
            items(recommendedPlaces) {
                PlaceCard(place = it, backgroundColor = Color(0xFFE0F7FA), navController, viewModel)
            }
        }
    }
}

@Composable
fun PlaceCard(
    place: PlaceInfo,
    backgroundColor: Color,
    navController: NavController,
    viewModel: PlaceDetailViewModel
) {
    val context = LocalContext.current
    var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(place.title) {
        imageBitmap = fetchGooglePlacePhoto(place.title, context)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .padding(vertical = 4.dp)
            .background(backgroundColor)
            .clickable {
                viewModel.selectedPlace = place
                navController.navigate(Routes.PlaceDetail.route)
            }
            .padding(12.dp)
    ) {
        if (imageBitmap != null) {
            Image(
                bitmap = imageBitmap!!,
                contentDescription = "Google Place Image",
                modifier = Modifier.size(80.dp)
            )
        } else {
            Image(
                painter = rememberAsyncImagePainter(place.thumbnailUrl),
                contentDescription = "Fallback thumbnail",
                modifier = Modifier.size(80.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(text = place.title, fontSize = 16.sp)
            Text(text = place.description, fontSize = 12.sp, maxLines = 1)
            Text(
                text = "#${place.searchKeyword} " + place.category.replace(">", " #"),
                fontSize = 12.sp,
                color = Color.DarkGray
            )
            Text(
                text = "⭐ ${place.rating} (${place.userRatingsTotal} reviews)",
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}
