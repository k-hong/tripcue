package com.example.tripcue.frame.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tripcue.frame.model.WeatherInfo
import com.example.tripcue.frame.uicomponents.Schedule.WeatherRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * 날씨 정보를 관리하는 ViewModel
 * - 특정 날짜와 위치(nx, ny)에 대한 날씨 예보를 비동기로 조회하여 상태로 보관
 * - UI에서 StateFlow를 구독하여 날씨 정보가 변경될 때 자동으로 UI를 갱신 가능
 */
class WeatherViewModel : ViewModel() {
    // 내부에서 변경 가능한 날씨 정보 상태 흐름 (초기값 null)
    private val _weatherInfo = MutableStateFlow<WeatherInfo?>(null)

    // 외부에 읽기 전용으로 노출하는 날씨 정보 상태 흐름
    val weatherInfo: StateFlow<WeatherInfo?> = _weatherInfo

    /**
     * 특정 날짜(date)와 위치(nx, ny)에 대한 날씨 정보를 조회하는 함수
     * - 발표 시각(baseDate, baseTime)은 항상 '오늘' 기준으로 계산하여 API 호출
     * - targetDate는 사용자가 알고 싶어하는 예보 날짜
     *
     * @param date 예보를 조회할 날짜 (LocalDate)
     * @param nx x 좌표 (격자 위치)
     * @param ny y 좌표 (격자 위치)
     */
    fun fetchWeatherForDateAndLocation(date: LocalDate, nx: Int, ny: Int) {
        // 오늘 기준 발표 날짜와 시간 가져오기
        val (baseDate, baseTime) = WeatherRepository.getBaseDateTime(LocalDate.now())

        // 예보를 원하는 날짜를 yyyyMMdd 형태로 변환
        val targetDate = WeatherRepository.getBaseDate(date)

        // 코루틴을 통해 비동기 호출
        viewModelScope.launch {
            val result = WeatherRepository.fetchWeather(baseDate, baseTime, nx, ny, targetDate)
            // API 호출 결과를 상태에 반영하여 UI 갱신 트리거
            _weatherInfo.value = result
        }
    }
}
