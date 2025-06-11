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
    // 공식 행정 구역 이름
    private val domesticKeywords = listOf(
        "서울", "부산", "대구", "인천", "광주", "대전", "울산", "세종", "제주",
        "경기", "강원", "충북", "충청북도", "충남", "충청남도", "전북", "전라북도", "전남", "전라남도",
        "경북", "경상북도", "경남", "경상남도"
    )

    // [수정] 누락되었던 대중적인 국내 지명 목록을 여기에 추가합니다.
    private val popularDomesticKeywords = listOf(
        "홍대", "강남", "이태원", "명동", "종로", "신촌", "가로수길", "인사동", "삼청동",
        "해운대", "광안리", "전주", "경주", "속초", "강릉", "여수"
    )

    suspend fun isDomesticLocation(context: Context, regionName: String): Boolean = withContext(Dispatchers.IO) {
        // 1단계: 네이버 API로 좌표 검색
        val coords = NaverPlaceApi.getCoordinatesForAddress(regionName)

        if (coords != null) {
            // 2단계: 얻은 좌표로 국가 코드 확인 (리버스 지오코딩)
            try {
                val geocoder = Geocoder(context, Locale.KOREAN)
                val addresses = geocoder.getFromLocation(coords.latitude, coords.longitude, 1)
                if (addresses?.isNotEmpty() == true) {
                    val countryCode = addresses[0].countryCode
                    Log.d("LocationUtils", "좌표 기반 판별 성공: '$regionName' -> 국가 코드: '$countryCode'")
                    return@withContext countryCode.equals("KR", ignoreCase = true)
                }
            } catch (e: IOException) {
                Log.e("LocationUtils", "리버스 지오코딩 실패. 예비 로직으로 넘어갑니다.", e)
            }
        }

        // 네이버 좌표 검색에 실패했거나, 리버스 지오코딩에 실패한 경우
        Log.w("LocationUtils", "'$regionName' 좌표 기반 판별 실패. 최종 예비 로직을 사용합니다.")
        return@withContext isDomesticFallback(regionName)
    }

    private fun isDomesticFallback(regionName: String): Boolean {
        // 1. 공식 행정 구역 이름에 포함되는지 확인
        val isKnownOfficial = domesticKeywords.any { regionName.contains(it) }
        if (isKnownOfficial) return true

        // 2. 대중적인 지명 목록에 포함되는지 확인
        val isKnownPopular = popularDomesticKeywords.any { regionName.contains(it) }
        if (isKnownPopular) return true

        return false
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