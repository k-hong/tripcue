package com.example.tripcue.frame.viewmodel

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
        val (baseDate, baseTime) = WeatherRepository.getBaseDateTime(date)

        viewModelScope.launch {
            val result = WeatherRepository.fetchWeather(baseDate, baseTime, nx, ny)
            _weatherInfo.value = result
        }
    }
}
