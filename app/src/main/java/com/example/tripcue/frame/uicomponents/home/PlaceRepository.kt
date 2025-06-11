package com.example.tripcue.frame.uicomponents.home

import android.content.Context
import android.util.Log

// 데이터 소스를 관리하고 가져오는 역할만 수행
class PlaceRepository(private val context: Context) {
    private val TAG = "TripcueLog"

    suspend fun getRecommendedPlaces(region: String, interests: List<String>): List<PlaceInfo> {
        val isDomestic = LocationUtils.isDomesticLocation(context, region)
        return if (isDomestic) {
            Log.d(TAG, "🌍 국내 지역($region)으로 판별되어 Naver API를 호출합니다.")
            // [수정] isDomestic 파라미터 전달 코드 삭제
            NaverPlaceApi.advancedSearchPlaces(context, region, interests, 15)
        } else {
            Log.d(TAG, "✈️ 해외 지역($region)으로 판별되어 Google API를 호출합니다.")
            // [수정] Google API에는 isDomestic 플래그 전달 유지
            GooglePlaceApi.advancedSearchPlaces(context, region, interests, 15, isDomestic = false)
        }
    }
}