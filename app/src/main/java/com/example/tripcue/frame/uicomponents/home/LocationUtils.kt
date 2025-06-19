package com.example.tripcue.frame.uicomponents.home

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.Locale

object LocationUtils {
    private val domesticKeywords = listOf(
        "서울", "부산", "대구", "인천", "광주", "대전", "울산", "세종", "제주",
        "경기", "강원", "충북", "충청북도", "충남", "충청남도", "전북", "전라북도", "전남", "전라남도",
        "경북", "경상북도", "경남", "경상남도"
    )
    private val popularDomesticKeywords = listOf(
        "홍대", "강남", "이태원", "명동", "종로", "신촌", "가로수길", "인사동", "삼청동",
        "해운대", "광안리", "전주", "경주", "속초", "강릉", "여수"
    )

    suspend fun isDomesticLocation(context: Context, regionName: String): Boolean {
        if (isDomesticFallback(regionName)) {
            return true
        }
        val countryCode = getCountryCodeForRegion(context, regionName)
        return countryCode.equals("KR", ignoreCase = true)
    }

    private fun isDomesticFallback(regionName: String): Boolean {
        val lowerRegionName = regionName.lowercase()
        if (domesticKeywords.any { lowerRegionName.contains(it) }) return true
        if (popularDomesticKeywords.any { lowerRegionName.contains(it) }) return true
        return false
    }

    suspend fun getCountryCodeForRegion(context: Context, regionName: String): String? = withContext(Dispatchers.IO) {
        val DEBUG_TAG = "TRIP_DEBUG" // 디버그용 태그

        try {
            Log.d(DEBUG_TAG, "[LocationUtils] getCountryCodeForRegion 시작. Geocoder로 '$regionName' 직접 조회 시도.")

            val geocoder = Geocoder(context, Locale.getDefault())
            // 참고: 네이버 지오코딩 API 대신 안드로이드 기본 Geocoder를 사용해 해외주소 변환 안정성을 높일 수 있습니다.
            // 여기서는 기존 로직 유지를 위해 NaverPlaceApi를 호출한다고 가정합니다.
            val addresses = geocoder.getFromLocationName(regionName, 1)

            if (addresses?.isNotEmpty() == true) {
                val countryCode = addresses[0].countryCode
                Log.d(DEBUG_TAG, "[LocationUtils] Geocoder가 이름으로 찾은 최종 countryCode: '$countryCode'")
                return@withContext countryCode
            } else {
                Log.w(DEBUG_TAG, "[LocationUtils] Geocoder가 '$regionName'에 대한 주소를 찾지 못했습니다.")
            }
        } catch (e: Exception) {
            Log.e(DEBUG_TAG, "[LocationUtils] getCountryCodeForRegion에서 에러 발생", e)
        }

        Log.w(DEBUG_TAG, "[LocationUtils] 최종적으로 국가 코드를 찾지 못해 null을 반환합니다.")
        return@withContext null
    }

    fun getAddressFromCoordinates(context: Context, lat: Double, lng: Double): String? {
        try {
            val geocoder = Geocoder(context, Locale.KOREAN)
            val addresses: List<Address>? = geocoder.getFromLocation(lat, lng, 1)
            if (addresses?.isNotEmpty() == true) {
                val address = addresses[0]
                return listOfNotNull(address.countryName, address.adminArea, address.locality, address.thoroughfare).joinToString(" ")
            }
        } catch (e: IOException) {
            Log.e("LocationUtils", "좌표 -> 주소 변환 실패", e)
        }
        return null
    }
}