package com.example.tripcue.frame.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tripcue.frame.model.ScheduleData
import com.example.tripcue.frame.model.Transportation
import com.example.tripcue.frame.uicomponents.Schedule.ScheduleRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ScheduleViewModel(private val repository: ScheduleRepository = ScheduleRepository()
) : ViewModel() {

//    private val firestore = FirebaseFirestore.getInstance()
    private val uid = FirebaseAuth.getInstance().currentUser?.uid
//    private val scheduleCollection = uid?.let {
//        firestore.collection("users").document(it).collection("schedules")
//    } ?: firestore.collection("anonymous")

    private val _schedules = MutableStateFlow<List<ScheduleData>>(emptyList())
    val schedules: StateFlow<List<ScheduleData>> = _schedules
    private val _selectedSchedule = MutableStateFlow<ScheduleData?>(null)
    val selectedSchedule: StateFlow<ScheduleData?> = _selectedSchedule

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    init {
        if (uid == null) {
            // 로그인 안 된 상태면 에러 메시지 세팅
            _errorMessage.value = "로그인이 필요합니다."
        }

        viewModelScope.launch {
            try {
                repository.getSchedules().collect { list ->
                    _schedules.value = list
                }
                _errorMessage.value = null
            } catch (e: Exception) {
                Log.e("ScheduleViewModel", "init error", e)
                _errorMessage.value = "스케줄 불러오기 중 오류가 발생했습니다: ${e.message}"
            }
        }
    }

    fun selectSchedule(schedule: ScheduleData) {
        _selectedSchedule.value = schedule
    }

    // 스케줄 전체 불러오기
    fun loadSchedules() {
        if (uid == null) {
            _errorMessage.value = "로그인이 필요합니다."
            return
        }
        viewModelScope.launch {
            try {
                val list = repository.getSchedules().first()  // getSchedules는 Flow라서, 적절히 변환 필요
                _schedules.value = list
                _errorMessage.value = null
            } catch (e: Exception) {
                Log.e("ScheduleViewModel", "loadSchedules error", e)
                _errorMessage.value = "스케줄 불러오기 중 오류가 발생했습니다: ${e.message}"
            }
        }
    }

    // 새로운 스케줄 추가
    fun addSchedule(schedule: ScheduleData) {
        if (uid == null) {
            _errorMessage.value = "로그인이 필요합니다."
            return
        }
        viewModelScope.launch {
            try {
                repository.addSchedule(schedule) // Firebase 저장
                _schedules.value = _schedules.value + schedule // 로컬에도 추가
                _errorMessage.value = null // 에러 없으면 초기화
            } catch (e: Exception) {
                Log.e("ScheduleViewModel", "addSchedule error", e)
                _errorMessage.value = "스케줄 추가 중 오류가 발생했습니다: ${e.message}"
            }
        }
    }

    // 스케줄 수정 (location + date 조합으로 문서 찾기)
    fun updateSchedule(updatedSchedule: ScheduleData) {
//        scheduleCollection
//            .whereEqualTo("location", updatedSchedule.location)
//            .whereEqualTo("date", updatedSchedule.date)
//            .get()
//            .addOnSuccessListener { querySnapshot ->
//                if (!querySnapshot.isEmpty) {
//                    val docId = querySnapshot.documents.first().id
//                    scheduleCollection.document(docId)
//                        .set(updatedSchedule)
//                        .addOnSuccessListener {
//                            loadSchedules()
//                        }
//                        .addOnFailureListener {
//                            // 수정 실패 처리
//                        }
//                } else {
//                    addSchedule(updatedSchedule)
//                }
//            }
//            .addOnFailureListener {
//                // 조회 실패 처리
//            }
        if (uid == null) {
            _errorMessage.value = "로그인이 필요합니다."
            return
        }
        viewModelScope.launch {
            try {
                repository.updateSchedule(updatedSchedule)
                _errorMessage.value = null
            } catch (e: Exception) {
                Log.e("ScheduleViewModel", "updateSchedule error", e)
                _errorMessage.value = "스케줄 수정 중 오류가 발생했습니다: ${e.message}"
            }
        }
    }

    fun getScheduleById(id: String): ScheduleData? {
        return _schedules.value.find { it.id == id }
    }
}