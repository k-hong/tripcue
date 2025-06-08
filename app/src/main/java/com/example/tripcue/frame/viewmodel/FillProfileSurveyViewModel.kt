package com.example.tripcue.frame.viewmodel

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

data class SurveyState(
    val page: Int = 0,
    val nickname: String = "",
    val age: Int = 0,
    val gender: String = "",
    val region: String = "",
    val interests: List<String> = emptyList()
)

class FillProfileSurveyViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(SurveyState())
    val uiState: StateFlow<SurveyState> = _uiState

    fun updateNickname(v: String) = _uiState.update { it.copy(nickname = v) }
    fun updateAge(v: Int) = _uiState.update { it.copy(age = v) }
    fun updateGender(v: String) = _uiState.update { it.copy(gender = v) }
    fun updateRegion(v: String) = _uiState.update { it.copy(region = v) }
    fun updateInterests(v: List<String>) = _uiState.update { it.copy(interests = v) }

    fun nextPage() = _uiState.update { it.copy(page = it.page + 1) }
    fun prevPage() = _uiState.update { it.copy(page = it.page - 1) }

    fun saveProfile(onComplete: () -> Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val data = _uiState.value
        val userMap = mapOf(
            "nickname" to data.nickname,
            "age" to data.age,
            "gender" to data.gender,
            "region" to data.region,
            "interests" to data.interests
        )
        FirebaseFirestore.getInstance().collection("users").document(uid)
            .update(userMap)
            .addOnSuccessListener { onComplete() }
    }
}
