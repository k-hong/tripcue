package com.example.tripcue.frame.uicomponents.home

object InterestConverter {

    // 한글 관심사를 검색에 최적화된 영어 키워드로 미리 매칭시켜 둡니다.
    private val interestMap = mapOf(
        "느긋한" to "relaxing spot",
        "관광지" to "famous tourist attraction",
        "식도락" to "famous restaurant",
        "사진" to "photogenic spot",
        "쇼핑" to "shopping store",
        "휴양" to "vacation spot",
        "모험" to "adventure spot",
        "자연" to "green travel spots",
        "역사" to "historical landmarks",
        "전통" to "traditional places",
        "문화" to "cultural sites",
        "야경" to "night view"
    )


    fun convertToEnglish(koreanInterest: String): String {
        return interestMap[koreanInterest] ?: koreanInterest
    }
}