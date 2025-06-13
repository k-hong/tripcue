package com.example.tripcue.frame.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue
import java.time.LocalDate

enum class Transportation(val displayName: String) {
    WALK("걷기"),
    BICYCLE("자전거"),
    TAXI("택시"),
    SUBWAY("지하철"),
    BUS("버스"),
    ETC("기타");

    override fun toString(): String = displayName
}

data class WeatherInfo(
    val status: String,       // 예: "맑음", "비", "흐림"
    val temperature: Double  // 예: 23.5 (섭씨)
)

@Parcelize
data class ScheduleData(
    var id: String = "",
    var location: String = "",
    var date: String = "",
    var transportation: Transportation = Transportation.WALK,
    var weather: @RawValue WeatherInfo? = null,
    var details: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null
): Parcelable

@Parcelize
data class ScheduleTitle(
    val id: String = "", // Firestore 문서 ID
    val title : String,
    val location: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    var ScheduleData : MutableList<ScheduleData> = mutableListOf() // 카드 클릭시 세부 일정표로 이동
) : Parcelable

data class PlaceResult( val placeId: String, val name: String, val lat: Double, val lng: Double)