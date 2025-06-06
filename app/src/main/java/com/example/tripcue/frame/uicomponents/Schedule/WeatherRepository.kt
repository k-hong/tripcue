package com.example.tripcue.frame.uicomponents.Schedule

import android.util.Log
import com.example.tripcue.frame.model.WeatherInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Calendar
import java.util.Locale

object WeatherRepository {
    // 이미 URL 인코딩된 인증키 사용
    private const val SERVICE_KEY = "Etc%2BmcUEPiCt0GoQHPGoc4OQZxgKwWHn6xKifSHGZ5nPMIWKeoMjplfAEFZqER%2FKjDrLJrIW4pdf5C9mUyU0WQ%3D%3D"

    suspend fun fetchWeather(baseDate: String, baseTime: String, nx: Int, ny: Int): WeatherInfo? {
        return withContext(Dispatchers.IO) {
            try {
                val urlStr = "http://apis.data.go.kr/1360000/VilageFcstInfoService_2.0/getVilageFcst?" +
                        "serviceKey=$SERVICE_KEY" +
                        "&numOfRows=1000" +
                        "&pageNo=1" +
                        "&dataType=JSON" +
                        "&base_date=$baseDate" +
                        "&base_time=$baseTime" +
                        "&nx=$nx" +
                        "&ny=$ny"

                val response = URL(urlStr).readText()
                Log.d("WeatherRepo", response)

                val json = JSONObject(response)
                val items = json.getJSONObject("response")
                    .getJSONObject("body")
                    .getJSONObject("items")
                    .getJSONArray("item")

                var tmp = 0.0
                var pty = "0"
                var sky = "1"

                // 필요한 데이터만 파싱
                for (i in 0 until items.length()) {
                    val item = items.getJSONObject(i)
                    when (item.getString("category")) {
                        "TMP" -> tmp = item.getString("fcstValue").toDouble()
                        "PTY" -> pty = item.getString("fcstValue")
                        "SKY" -> sky = item.getString("fcstValue")
                    }
                }

                val weatherStatus = decodeWeatherStatus(pty, sky)
                WeatherInfo(weatherStatus, tmp)

            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    // 강수형태(PTY)와 하늘상태(SKY)를 해석해 날씨 상태 문자열 반환
    private fun decodeWeatherStatus(pty: String, sky: String): String {
        return when (pty) {
            "0" -> when (sky) {
                "1" -> "맑음"
                "3" -> "구름 많음"
                "4" -> "흐림"
                else -> "알수없음"
            }
            "1" -> "비"
            "2" -> "비/눈"
            "3" -> "눈"
            "5" -> "빗방울"
            "6" -> "빗방울눈날림"
            "7" -> "눈날림"
            else -> "알수없음"
        }
    }

    // baseTime 선택 (현재 시각 기준, 오늘이면 가장 가까운 과거 발표시간, 아니면 2300 고정)
    fun getBaseTimeForDate(date: LocalDate): String {
        val now = Calendar.getInstance()
        val isToday = date == LocalDate.now()

        // 내림차순 정렬된 발표 시간 목록
        val baseTimes = listOf("2300", "2000", "1700", "1400", "1100", "0800", "0500", "0200")

        val currentTime = if (isToday) {
            val hour = now.get(Calendar.HOUR_OF_DAY)
            val minute = now.get(Calendar.MINUTE)
            String.format("%02d%02d", hour, minute)
        } else "2300"

        // 현재 시각보다 작거나 같은 가장 최근 발표시간 선택
        return baseTimes.firstOrNull { it <= currentTime } ?: "2300"
    }

    // baseDate와 baseTime을 한꺼번에 얻는 함수
    fun getBaseDateTime(date: LocalDate): Pair<String, String> {
        val baseDate = getBaseDate(date)
        val baseTime = getBaseTimeForDate(date)
        return baseDate to baseTime
    }

    // yyyyMMdd 형식으로 변환
    fun getBaseDate(date: LocalDate): String {
        val formatter = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, date.year)
            set(Calendar.MONTH, date.monthValue - 1)
            set(Calendar.DAY_OF_MONTH, date.dayOfMonth)
        }
        return formatter.format(calendar.time)
    }
}
