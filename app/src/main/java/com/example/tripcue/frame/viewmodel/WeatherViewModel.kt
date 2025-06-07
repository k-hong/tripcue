package com.example.tripcue.frame.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tripcue.frame.model.WeatherInfo
import com.example.tripcue.frame.uicomponents.Schedule.WeatherRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

class WeatherViewModel : ViewModel() {
    private val _weatherInfo = MutableStateFlow<WeatherInfo?>(null)
    val weatherInfo: StateFlow<WeatherInfo?> = _weatherInfo

    // 예보 조회 함수
    fun fetchWeatherForDateAndLocation(date: LocalDate, nx: Int, ny: Int) {
        val (baseDate, baseTime) = WeatherRepository.getBaseDateTime(LocalDate.now()) // 항상 오늘 기준 발표 시각을 써야 함
        val targetDate = WeatherRepository.getBaseDate(date) // 내가 알고 싶은 예보 날짜

        viewModelScope.launch {
            val result = WeatherRepository.fetchWeather(baseDate, baseTime, nx, ny, targetDate)
            _weatherInfo.value = result
        }
    }
}
