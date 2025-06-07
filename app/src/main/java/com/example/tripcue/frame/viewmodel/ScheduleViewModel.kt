package com.example.tripcue.frame.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tripcue.frame.model.ScheduleData
import com.example.tripcue.frame.model.Transportation
import com.example.tripcue.frame.uicomponents.Schedule.ScheduleRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ScheduleViewModel(private val repository: ScheduleRepository = ScheduleRepository()
) : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private val scheduleCollection = firestore.collection("schedules")

    private val _schedules = MutableStateFlow<List<ScheduleData>>(emptyList())
    val schedules: StateFlow<List<ScheduleData>> = _schedules
    private val _selectedSchedule = MutableStateFlow<ScheduleData?>(null)
    val selectedSchedule: StateFlow<ScheduleData?> = _selectedSchedule

    init {
        viewModelScope.launch {
            repository.getSchedules().collect { list ->
                _schedules.value = list
            }
        }
    }

    fun selectSchedule(schedule: ScheduleData) {
        _selectedSchedule.value = schedule
    }

    // 스케줄 전체 불러오기
    fun loadSchedules() {
        viewModelScope.launch {
            _schedules.value = repository.getSchedules() as List<ScheduleData>
        }
    }

    // 새로운 스케줄 추가
    fun addSchedule(schedule: ScheduleData) {
        viewModelScope.launch {
            repository.addSchedule(schedule) // Firebase 저장
            _schedules.value = _schedules.value + schedule // 로컬에도 추가
        }
    }

    // 스케줄 수정 (location + date 조합으로 문서 찾기)
    fun updateSchedule(updatedSchedule: ScheduleData) {
        scheduleCollection
            .whereEqualTo("location", updatedSchedule.location)
            .whereEqualTo("date", updatedSchedule.date)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val docId = querySnapshot.documents.first().id
                    scheduleCollection.document(docId)
                        .set(updatedSchedule)
                        .addOnSuccessListener {
                            loadSchedules()
                        }
                        .addOnFailureListener {
                            // 수정 실패 처리
                        }
                } else {
                    addSchedule(updatedSchedule)
                }
            }
            .addOnFailureListener {
                // 조회 실패 처리
            }
    }

    fun getScheduleById(id: String): ScheduleData? {
        return _schedules.value.find { it.id == id }
    }
}