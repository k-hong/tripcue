package com.example.tripcue.frame.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tripcue.frame.model.ScheduleData
import com.example.tripcue.frame.model.ScheduleTitle
import com.example.tripcue.frame.uicomponents.Schedule.ScheduleRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.cancellation.CancellationException

/**
 * 여행 일정 관련 데이터 관리용 ViewModel
 * - Firestore에서 일정 데이터 로딩, 추가, 수정 등을 처리
 * - UI와 데이터 상태를 StateFlow로 관리하여 Compose에서 관찰 가능
 */
class ScheduleViewModel(
    private val repository: ScheduleRepository = ScheduleRepository()
) : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()

    // 현재 로그인된 사용자 UID
    private val uid = FirebaseAuth.getInstance().currentUser?.uid

    // 상세 일정 목록 상태 (ScheduleData 리스트)
    private val _scheduleDetails = MutableStateFlow<List<ScheduleData>>(emptyList())
    val scheduleDetails = _scheduleDetails.asStateFlow()

    // 전체 일정 리스트 상태
    private val _schedules = MutableStateFlow<List<ScheduleData>>(emptyList())
    val schedules: StateFlow<List<ScheduleData>> = _schedules

    // 선택된 일정 상태 (상세 조회나 편집 대상)
    private val _selectedSchedule = MutableStateFlow<ScheduleData?>(null)
    val selectedSchedule: StateFlow<ScheduleData?> = _selectedSchedule

    // 일정 제목 리스트 상태 (스케줄 타이틀 목록)
    private val _scheduleTitles = MutableStateFlow<List<ScheduleTitle>>(emptyList())
    val scheduleTitles: StateFlow<List<ScheduleTitle>> = _scheduleTitles

    // 에러 메시지 상태 (UI에서 에러 표시용)
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    init {
        // 사용자 UID 없으면 로그인 필요 메시지 세팅
        if (uid == null) {
            _errorMessage.value = "로그인이 필요합니다."
        }

        // ViewModel 초기화 시 저장소로부터 스케줄 리스트를 실시간 구독하여 업데이트
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

    /**
     * 특정 일정을 선택 상태로 변경 (UI에서 상세 조회나 편집용)
     */
    fun selectSchedule(schedule: ScheduleData) {
        _selectedSchedule.value = schedule
    }

    /**
     * Firestore에서 현재 사용자의 일정 제목 목록을 로드
     * 각 문서의 ID를 ScheduleTitle 객체에 포함하여 상태에 저장
     */
    fun loadScheduleTitles() {
        val uid = getCurrentUserUid() ?: return
        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("users")
                    .document(uid)
                    .collection("schedules")
                    .get()
                    .await()

                // 문서 데이터 디버깅용 로그
                for (doc in snapshot.documents) {
                    Log.d("DebugScheduleTitle", "Document data: ${doc.data}")
                }

                // Firestore 문서 → ScheduleTitle 객체 리스트 변환 (id 포함)
                val titles = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(ScheduleTitle::class.java)?.copy(id = doc.id)
                }

                _scheduleTitles.value = titles
                _errorMessage.value = null
            } catch (e: Exception) {
                Log.e("ScheduleViewModel", "loadScheduleTitles error", e)
                _errorMessage.value = "스케줄 제목 목록 불러오기 실패: ${e.message}"
            }
        }
    }

    /**
     * 특정 cityDocId(도시별 문서 ID)에 해당하는 상세 일정(Tasks) 목록 로드
     * 로드된 상세 일정 리스트를 상태로 저장
     */
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

                // 문서 ID를 ScheduleData.id에 저장하면서 리스트로 변환
                val tasks = tasksSnapshot.documents.map { doc ->
                    val data = doc.toObject(ScheduleData::class.java)!!
                    data.id = doc.id
                    data
                }
                _scheduleDetails.value = tasks
            } catch (e: Exception) {
                Log.e("ScheduleViewModel", "loadScheduleDetails error", e)
                _errorMessage.value = "상세 일정 불러오기 실패: ${e.message}"
            }
        }
    }

    /**
     * 새 스케줄 추가
     * cityDocId에 해당하는 경로에 스케줄을 문서로 저장
     * 저장 후 내부 리스트에도 추가하여 상태 업데이트
     */
    fun addSchedule(schedule: ScheduleData, cityDocId: String) {
        if (uid == null) {
            _errorMessage.value = "로그인이 필요합니다."
            return
        }
        viewModelScope.launch {
            try {
                Log.d("ScheduleViewModel", "Adding schedule: $schedule to cityDocId: $cityDocId")
                val newDocId = firestore.collection("users").document().id
                val docRef = firestore.collection("users")
                    .document(uid)
                    .collection("schedules")
                    .document(cityDocId)
                    .collection("tasks")
                    .document(if (schedule.id.isEmpty()) newDocId else schedule.id)

                docRef.set(schedule).await()
                Log.d("ScheduleViewModel", "Schedule added successfully")

                // 상태 리스트 갱신 (기존 리스트에 새 일정 추가)
                _schedules.value = _schedules.value + schedule
                _errorMessage.value = null
            } catch (e: Exception) {
                Log.e("ScheduleViewModel", "addSchedule error", e)
                _errorMessage.value = "스케줄 추가 중 오류가 발생했습니다: ${e.message}"
            }
        }
    }

    /**
     * 기존 스케줄 업데이트
     * cityDocId 및 schedule.id를 통해 Firestore 문서에 업데이트 수행
     * 성공 시 상태 리스트도 갱신
     *
     * @param onComplete 작업 완료 여부를 콜백으로 반환
     */
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
                Log.d("Coroutine", "업데이트 시도: uid=$uid, cityDocId=$cityDocId, scheduleId=${schedule.id}")
                val docRef = firestore.collection("users")
                    .document(uid)
                    .collection("schedules")
                    .document(cityDocId)
                    .collection("tasks")
                    .document(schedule.id)

                // Firestore에 저장할 Map 형태 데이터 (enum -> String 변환 포함)
                val dataMap = mapOf(
                    "id" to schedule.id,
                    "location" to schedule.location,
                    "date" to schedule.date,
                    "transportation" to schedule.transportation.name,
                    "details" to schedule.details,
                    "latitude" to schedule.latitude,
                    "longitude" to schedule.longitude,
                    "weather" to schedule.weather
                )
                docRef.set(dataMap).await()
                Log.d("Coroutine", "스케줄 업데이트 성공")

                // 상태 리스트에서 업데이트 대상 일정만 교체
                _schedules.value = _schedules.value.map {
                    if (it.id == schedule.id) schedule else it
                }
                loadScheduleDetails(cityDocId)
                _errorMessage.value = null
                onComplete(true)
            } catch (e: CancellationException) {
                // 코루틴 취소 예외는 무시하거나 로그만 기록
                Log.d("Coroutine", "작업 취소됨: ${e.message}")
                onComplete(false)
            } catch (e: Exception) {
                Log.e("Coroutine", "에러 발생", e)
                _errorMessage.value = e.message
                onComplete(false)
            }
        }
    }

    fun deleteSchedule(scheduleId: String, cityDocId: String, onComplete: (Boolean) -> Unit) {
        val uid = getCurrentUserUid() ?: run {
            onComplete(false)
            return
        }

        viewModelScope.launch {
            try {
                firestore.collection("users")
                    .document(uid)
                    .collection("schedules")
                    .document(cityDocId)
                    .collection("tasks")
                    .document(scheduleId)
                    .delete()
                    .await()

                // 삭제 후 상태 업데이트를 원하면 여기에 코드 추가 가능
                loadScheduleDetails(cityDocId)

                onComplete(true)
            } catch (e: Exception) {
                Log.e("ScheduleViewModel", "삭제 실패", e)
                onComplete(false)
            }
        }
    }

    /**
     * 선택된 스케줄 상태를 직접 세팅 (UI 등에서 호출)
     */
    fun setSchedule(schedule: ScheduleData) {
        _selectedSchedule.value = schedule
    }

    /**
     * 현재 로그인한 사용자 UID 반환 (없으면 null)
     */
    fun getCurrentUserUid(): String? {
        return FirebaseAuth.getInstance().currentUser?.uid
    }
}