package com.example.tripcue.frame.uicomponents.home

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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.tripcue.frame.model.Routes
import com.example.tripcue.frame.viewmodel.PlaceDetailViewModel
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Composable
fun Home(
    navController: NavController,
    refreshTrigger: Boolean,
    placeDetailViewModel: PlaceDetailViewModel = hiltViewModel(),
    homeViewModel: HomeViewModel = viewModel()
) {
    LaunchedEffect(Unit, refreshTrigger) {
        homeViewModel.fetchHomeData()
    }

    when (val state = homeViewModel.uiState) {
        is HomeUiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("추천 장소를 불러오는 중입니다...")
            }
        }
        is HomeUiState.Error -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(state.message)
            }
        }
        is HomeUiState.Success -> {
            if (state.places.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("추천할만한 장소를 찾지 못했어요. 관심사를 변경해보는 건 어떠세요?")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp)
                ) {
                    item {
                        Text(text = "${state.nickname} 님", fontSize = 22.sp)
                        Text(text = "여행을 떠날 준비되셨나요?", fontSize = 18.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("🔥 추천 관심 장소", fontSize = 16.sp, modifier = Modifier.padding(top = 8.dp))
                    }
                    items(state.places) { place ->
                        PlaceCard(place = place, backgroundColor = Color(0xFFE0F7FA), navController, placeDetailViewModel)
                    }
                }
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

    // [되돌리기] '깨끗한' 원본 제목으로 사진을 검색하는 이전 방식으로 변경
    LaunchedEffect(place.title) {
        // 국내 장소이거나, 해외 장소 중 사진이 있다고 확인된 경우에만 사진 검색 실행
        if (place.isDomestic || place.thumbnailUrl == "HAS_PHOTO") {
            imageBitmap = fetchGooglePlacePhoto(place.title, context)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .padding(vertical = 4.dp)
            .background(backgroundColor)
            .clickable {
                val encodedTitle = URLEncoder.encode(place.title, StandardCharsets.UTF_8.name())
                navController.navigate(
                    Routes.PlaceDetail.createRoute(
                        lat = place.latitude,
                        lng = place.longitude,
                        title = encodedTitle,
                        isDomestic = place.isDomestic

                    )
                )
            }
            .padding(12.dp)
    ) {
        val painter = if (imageBitmap != null) {
            remember(imageBitmap) { androidx.compose.ui.graphics.painter.BitmapPainter(imageBitmap!!) }
        } else {
            rememberAsyncImagePainter("https://via.placeholder.com/150")
        }

        Image(
            painter = painter,
            contentDescription = place.title,
            modifier = Modifier.size(80.dp),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            // [수정] 화면에 표시할 때는 원본 제목과 한국어 유형을 조합
            val displayTitle = if (place.koreanType != null) {
                "${place.title} (${place.koreanType})"
            } else {
                place.title
            }
            Text(text = displayTitle, fontSize = 16.sp, maxLines = 2)
            Text(text = place.description, fontSize = 12.sp, maxLines = 1)
            Text(
                text = "#${place.searchKeyword} " + place.category.replace(">", " #"),
                fontSize = 12.sp,
                color = Color.DarkGray
            )
        }
    }
}