package com.example.tripcue.frame.uicomponents.home

import android.content.Context
import android.util.Log

class PlaceRepository(private val context: Context) {
    private val TAG = "TripcueLog"

    suspend fun getRecommendedPlaces(region: String, interests: List<String>): List<PlaceInfo> {
        val isDomestic = LocationUtils.isDomesticLocation(context, region)
        return if (isDomestic) {
            Log.d(TAG, "🌍 국내 지역($region)으로 판별되어 Naver API를 호출합니다.")
            NaverPlaceApi.advancedSearchPlaces(context, region, interests, 15)
        } else {
            val countryCode = LocationUtils.getCountryCodeForRegion(context, region)

            Log.d(TAG, "✈️ 해외 지역($region)으로 판별되어 Google API를 호출합니다. 국가코드: $countryCode")
            GooglePlaceApi.advancedSearchPlaces(
                context = context,
                region = region,
                interests = interests,
                totalLimit = 15,
                isDomestic = false,
                countryCode = countryCode
            )
        }
    }
}