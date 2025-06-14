package com.example.tripcue.frame.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tripcue.frame.model.ScheduleData
import com.example.tripcue.frame.model.Transportation
import com.example.tripcue.frame.uicomponents.Schedule.ScheduleRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException

class ScheduleViewModel(private val repository: ScheduleRepository = ScheduleRepository()
) : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private val _scheduleDetails = MutableStateFlow<List<ScheduleData>>(emptyList())
    val scheduleDetails = _scheduleDetails.asStateFlow()

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
    fun loadScheduleDetails(cityDocId: String) {
        val uid = getCurrentUserUid() ?: return
        viewModelScope.launch {
            try {
                val tasksSnapshot = firestore.collection("users")
                    .document(uid)
                    .collection("schedules")
                    .document(cityDocId)
                    .collection("tasks")
                    .get()
                    .await()

                val tasks = tasksSnapshot.documents.map { doc ->
                    val data = doc.toObject(ScheduleData::class.java)!!
                    data.id = doc.id   // 문서의 ID를 ScheduleData.id에 할당
                    data
                }
                _scheduleDetails.value = tasks
            } catch (e: Exception) {
                Log.e("ScheduleViewModel", "loadScheduleDetails error", e)
                _errorMessage.value = "상세 일정 불러오기 실패: ${e.message}"
            }
        }
    }

    // 새로운 스케줄 추가
    fun addSchedule(schedule: ScheduleData, cityDocId: String) {
        if (uid == null) {
            _errorMessage.value = "로그인이 필요합니다."
            return
        }
        viewModelScope.launch {
            try {
                Log.d("ScheduleViewModel", "Adding schedule: $schedule to cityDocId: $cityDocId")
                val docRef = firestore.collection("users")
                    .document(uid!!)
                    .collection("schedules")
                    .document(cityDocId)
                    .collection("tasks")
                    .document(schedule.id)

                docRef.set(schedule).await()
                Log.d("ScheduleViewModel", "Schedule added successfully")

                _schedules.value = _schedules.value + schedule
                _errorMessage.value = null
            } catch (e: Exception) {
                Log.e("ScheduleViewModel", "addSchedule error", e)
                _errorMessage.value = "스케줄 추가 중 오류가 발생했습니다: ${e.message}"
            }
        }
    }

    fun updateSchedule(schedule: ScheduleData, cityDocId: String, onComplete: (Boolean) -> Unit) {
        Log.d("UpdateSchedule", "updateSchedule 호출됨 cityDocId=$cityDocId, scheduleId=${schedule.id}")
        val uid = getCurrentUserUid()
        if (uid == null) {
            _errorMessage.value = "로그인이 필요합니다."
            onComplete(false)
            return
        }
        if (schedule.id.isEmpty()) {
            _errorMessage.value = "수정할 스케줄 ID가 없습니다."
            onComplete(false)
            return
        }

        viewModelScope.launch {
            try {
                Log.d(
                    "Coroutine",
                    "업데이트 시도: uid=$uid, cityDocId=$cityDocId, scheduleId=${schedule.id}"
                )
                val docRef = firestore.collection("users")
                    .document(uid)
                    .collection("schedules")
                    .document(cityDocId)
                    .collection("tasks")
                    .document(schedule.id)

                val dataMap = mapOf(
                    "id" to schedule.id,
                    "location" to schedule.location,
                    "date" to schedule.date,
                    "transportation" to schedule.transportation.name, // enum -> String 변환
                    "details" to schedule.details,
                    "latitude" to schedule.latitude,
                    "longitude" to schedule.longitude
                )
                docRef.set(dataMap).await()
                Log.d("Coroutine", "스케줄 업데이트 성공")

                _schedules.value = _schedules.value.map {
                    if (it.id == schedule.id) schedule else it
                }
                _errorMessage.value = null
                onComplete(true)
            } catch (e: CancellationException) {
                // 코루틴 취소 시 예외는 무시하거나 로깅만 한다
                Log.d("Coroutine", "작업 취소됨: ${e.message}")
                onComplete(false) // 필요하다면 콜백 호출
            } catch (e: Exception) {
                Log.e("Coroutine", "에러 발생", e)
                _errorMessage.value = e.message
                onComplete(false)
            }
        }
    }


        fun getCurrentUserUid(): String? {
        return FirebaseAuth.getInstance().currentUser?.uid
    }
}