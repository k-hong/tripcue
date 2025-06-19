package com.example.tripcue.frame.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue
import java.time.LocalDate

/**
 * 이동 수단을 나타내는 열거형 클래스
 * 각 항목은 화면에 표시할 한글 이름(displayName)을 가지고 있음
 * Parcelable 구현으로 Android 컴포넌트 간 데이터 전달 가능
 */
@Parcelize
enum class Transportation(val displayName: String) : Parcelable {
    WALK("걷기"),
    BICYCLE("자전거"),
    TAXI("택시"),
    SUBWAY("지하철"),
    BUS("버스"),
    ETC("기타");

    // enum 값 출력 시 displayName 반환 (예: toString 호출 시 "걷기" 등)
    override fun toString(): String = displayName
}

/**
 * 날씨 정보를 담는 데이터 클래스
 *
 * @param status 날씨 상태 (예: "맑음", "비", "흐림" 등)
 * @param temperature 현재 기온 (섭씨, Double)
 */
data class WeatherInfo(
    val status: String,       // 예: "맑음", "비", "흐림"
    val temperature: Double   // 예: 23.5 (섭씨)
)

/**
 * 개별 일정 정보를 담는 데이터 클래스
 * Parcelable로 액티비티나 프래그먼트 간 전달 가능
 *
 * @param id Firestore 문서 ID 등 고유 식별자
 * @param location 일정 장소 이름
 * @param date 일정 날짜 (형식은 문자열로 관리)
 * @param transportation 이동 수단 (Transportation enum)
 * @param weather 날씨 정보 (nullable, WeatherInfo 타입)
 * @param details 일정에 대한 추가 설명 또는 상세 내용
 * @param latitude 장소 위도 (nullable, Double)
 * @param longitude 장소 경도 (nullable, Double)
 */
@Parcelize
data class ScheduleData(
    var id: String = "",
    var location: String = "",
    var date: String = "",
    var transportation: Transportation = Transportation.WALK,
    var weather: @RawValue WeatherInfo? = null,  // Parcelable이 아닌 WeatherInfo를 안전하게 전달하기 위해 RawValue 사용
    var details: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null
) : Parcelable

/**
 * 스케줄 목록에서 각 스케줄의 대표 정보를 담는 데이터 클래스
 * Parcelable로 UI 컴포넌트 간 데이터 전달에 용이
 *
 * @param id Firestore 문서 ID로 각 스케줄을 식별하는 키
 * @param title 스케줄 제목
 * @param location 스케줄 장소명
 * @param startDate 스케줄 시작일 (예: "2025-06-18" 형식 문자열)
 * @param endDate 스케줄 종료일 (예: "2025-06-20" 형식 문자열)
 */
// 내부에 세부 일정 리스트를 포함할 수도 있으나, 현재는 주석 처리 상태
@Parcelize
data class ScheduleTitle(
    val id: String = "", // Firestore 문서 ID
    val title: String = "",
    val location: String = "",
    val startDate: String = "",  // LocalDate -> String (예: "2025-06-18")
    val endDate: String = ""     // LocalDate -> String
//    var ScheduleData : MutableList<ScheduleData> = mutableListOf() // 카드 클릭시 세부 일정표로 이동 가능
) : Parcelable

/**
 * 장소 검색 결과를 담는 데이터 클래스
 *
 * @param placeId 장소 고유 ID (예: Google Places API의 place_id)
 * @param name 장소 이름
 * @param lat 위도
 * @param lng 경도
 */
data class PlaceResult(
    val placeId: String,
    val name: String,
    val lat: Double,
    val lng: Double
)
