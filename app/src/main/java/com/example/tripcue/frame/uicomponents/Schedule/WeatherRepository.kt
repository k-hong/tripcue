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

/**
 * 기상청 단기예보 API를 통해 날씨 정보를 가져오는 리포지토리
 */
object WeatherRepository {

    // 인코딩된 인증키 (공공데이터 포털에서 발급받은 서비스 키)
    private const val SERVICE_KEY = "Etc%2BmcUEPiCt0GoQHPGoc4OQZxgKwWHn6xKifSHGZ5nPMIWKeoMjplfAEFZqER%2FKjDrLJrIW4pdf5C9mUyU0WQ%3D%3D"

    /**
     * 지정된 날짜, 시간, 위치(nx, ny)에 대한 날씨 정보를 가져오는 suspend 함수
     *
     * @param baseDate 기준 날짜 (예보 발표 기준 날짜, yyyyMMdd)
     * @param baseTime 기준 시간 (예보 발표 기준 시간, HHmm)
     * @param nx 예보지점 x좌표
     * @param ny 예보지점 y좌표
     * @param targetDate 실제로 확인하려는 날짜 (yyyyMMdd)
     * @return 날씨 정보 객체 (WeatherInfo), 실패 시 null
     */
    suspend fun fetchWeather(baseDate: String, baseTime: String, nx: Int, ny: Int, targetDate: String): WeatherInfo? {
        return withContext(Dispatchers.IO) {
            try {
                // API URL 구성
                val urlStr = "http://apis.data.go.kr/1360000/VilageFcstInfoService_2.0/getVilageFcst?" +
                        "serviceKey=$SERVICE_KEY" +
                        "&numOfRows=1000" +
                        "&pageNo=1" +
                        "&dataType=JSON" +
                        "&base_date=$baseDate" +
                        "&base_time=$baseTime" +
                        "&nx=$nx" +
                        "&ny=$ny"

                // API 요청 및 응답 문자열 읽기
                val response = URL(urlStr).readText()
                Log.d("WeatherRepo", response)

                // JSON 파싱
                val json = JSONObject(response)
                val items = json.getJSONObject("response")
                    .getJSONObject("body")
                    .getJSONObject("items")
                    .getJSONArray("item")

                // 기본값 설정
                var tmp = 0.0         // 기온
                var pty = "0"         // 강수 형태
                var sky = "1"         // 하늘 상태

                // 예보 항목에서 필요한 정보만 추출 (해당 날짜에 해당하는 데이터만)
                for (i in 0 until items.length()) {
                    val item = items.getJSONObject(i)
                    if (item.getString("fcstDate") == targetDate) {
                        when (item.getString("category")) {
                            "TMP" -> tmp = item.getString("fcstValue").toDouble()   // 기온
                            "PTY" -> pty = item.getString("fcstValue")              // 강수 형태
                            "SKY" -> sky = item.getString("fcstValue")              // 하늘 상태
                        }
                    }
                }

                // 날씨 상태 해석
                val weatherStatus = decodeWeatherStatus(pty, sky)
                val weatherInfo = WeatherInfo(weatherStatus, tmp)

                // 결과 출력
                Log.d("WeatherCheck", "날씨 상태: ${weatherInfo.status}, 기온: ${weatherInfo.temperature}℃")

                weatherInfo

            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    /**
     * 강수형태(PTY)와 하늘상태(SKY)를 조합해 날씨 상태 문자열로 변환
     */
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

    /**
     * 날짜에 따른 baseTime 계산
     * - 오늘인 경우: 현재 시각보다 가장 가까운 과거의 발표 시간 선택
     * - 오늘이 아닌 경우: 예보 가능성이 높은 1400시 고정
     */
    fun getBaseTimeForDate(date: LocalDate): String {
        val now = Calendar.getInstance()
        val isToday = date == LocalDate.now()

        // 발표 시간 목록 (역순)
        val baseTimes = listOf("2300", "2000", "1700", "1400", "1100", "0800", "0500", "0200")

        val currentTime = if (isToday) {
            val hour = now.get(Calendar.HOUR_OF_DAY)
            val minute = now.get(Calendar.MINUTE)
            String.format("%02d%02d", hour, minute)
        } else "1400" // 과거 날짜의 경우 안전하게 1400으로 설정

        return baseTimes.firstOrNull { it <= currentTime } ?: "1400"
    }

    /**
     * 날짜에 맞는 baseDate와 baseTime을 함께 반환
     */
    fun getBaseDateTime(date: LocalDate): Pair<String, String> {
        val baseDate = getBaseDate(date)
        val baseTime = getBaseTimeForDate(date)
        return baseDate to baseTime
    }

    /**
     * LocalDate를 "yyyyMMdd" 형식의 문자열로 변환
     */
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
