package com.example.tripcue.frame.viewmodel

import androidx.lifecycle.ViewModel
import com.example.tripcue.frame.model.ScheduleData
import com.example.tripcue.frame.model.ScheduleTitle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class SharedScheduleViewModel : ViewModel() {
    private val _selectedSchedule = MutableStateFlow<ScheduleTitle?>(null)
    val selectedSchedule = _selectedSchedule.asStateFlow()

    fun setSchedule(schedule: ScheduleTitle) {
        _selectedSchedule.value = schedule
    }
}