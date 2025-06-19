package com.example.tripcue.frame.uicomponents.home

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed interface HomeUiState {
    object Loading : HomeUiState
    data class Success(val places: List<PlaceInfo>, val nickname: String) : HomeUiState
    data class Error(val message: String) : HomeUiState
}

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val placeRepository = PlaceRepository(application.applicationContext)
    private val db = FirebaseFirestore.getInstance()
    private val user = FirebaseAuth.getInstance().currentUser
    private val TAG = "TripcueLog"

    var uiState by mutableStateOf<HomeUiState>(HomeUiState.Loading)
        private set

    fun fetchHomeData() {
        viewModelScope.launch {
            uiState = HomeUiState.Loading
            try {
                val userDoc = user?.uid?.let { db.collection("users").document(it).get().await() }
                if (userDoc == null) {
                    uiState = HomeUiState.Error("사용자 정보를 찾을 수 없습니다.")
                    return@launch
                }

                val nickname = userDoc.getString("nickname") ?: "사용자"
                val region = userDoc.getString("region") ?: "서울"
                val interests = userDoc.get("interests") as? List<String> ?: emptyList()

                // [로그 유지] Firestore에서 가져온 데이터를 기록하는 로그
                Log.d(TAG, "👤 Firestore 데이터: region=$region, interests=$interests")

                if (interests.isNotEmpty()) {
                    val places = placeRepository.getRecommendedPlaces(region, interests)
                    uiState = HomeUiState.Success(places, nickname)
                    // [로그 유지] 최종 성공 결과를 기록하는 로그
                    Log.d(TAG, "✅ 추천 장소 (${places.size}개): $places")
                } else {
                    uiState = HomeUiState.Success(emptyList(), nickname)
                }
            } catch (e: Exception) {

                // [로그 유지] 실패 원인을 기록하는 로그
                Log.e(TAG, "❌ 추천 장소 가져오기 실패", e)
                val errorMessage = if (e.message?.contains("naveropenapi") == true) {
                    "지역 정보를 가져오는데 실패했어요. API 키를 확인해주세요."
                } else {
                    "데이터를 불러오는데 실패했습니다."
                }
                uiState = HomeUiState.Error(errorMessage)
            }
        }
    }
}