package com.example.tripcue.frame.viewmodel

import androidx.lifecycle.ViewModel
import com.example.tripcue.frame.model.ScheduleTitle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 여러 Composable 혹은 화면 간에 선택된 ScheduleTitle 정보를 공유하기 위한 ViewModel
 * - 선택된 스케줄 제목 데이터를 StateFlow로 관리하여 구독 가능
 * - Compose 내에서 상태 공유 및 반응형 UI 구현에 사용
 */
class SharedScheduleViewModel : ViewModel() {

    // 내부에서 변경 가능한 MutableStateFlow (선택된 ScheduleTitle or null)
    private val _selectedSchedule = MutableStateFlow<ScheduleTitle?>(null)

    // 외부에는 읽기 전용 StateFlow로 노출 (불변)
    val selectedSchedule = _selectedSchedule.asStateFlow()

    /**
     * 선택된 스케줄 제목을 설정하는 함수
     * @param schedule 공유할 ScheduleTitle 객체
     */
    fun setSchedule(schedule: ScheduleTitle) {
        _selectedSchedule.value = schedule
    }
}
