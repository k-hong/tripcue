package com.example.tripcue.frame.model

import android.os.Parcelable

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

data class ScheduleData(
    val id: String = "",
    var location: String = "",
    var date: String = "",
    var transportation: Transportation = Transportation.WALK,
    var weather: WeatherInfo? = null,
    var details: String = ""
)