package com.example.tripcue.frame.uicomponents.home

import com.google.android.libraries.places.api.model.Place

object PlaceTypeConverter {

    // 주요 장소 유형을 한국어로 미리 정의해 둡니다.
    private val typeMap = mapOf(
        Place.Type.SHOPPING_MALL to "쇼핑몰",
        Place.Type.RESTAURANT to "음식점",
        Place.Type.CAFE to "카페",
        Place.Type.STORE to "상점",
        Place.Type.LODGING to "숙소",
        Place.Type.MUSEUM to "박물관",
        Place.Type.PARK to "공원",
        Place.Type.TOURIST_ATTRACTION to "관광 명소",
        Place.Type.TRAVEL_AGENCY to "여행사",
        Place.Type.ART_GALLERY to "미술관",
        Place.Type.BAKERY to "베이커리",
        Place.Type.BAR to "바(술집)",
        Place.Type.BOOK_STORE to "서점",
        Place.Type.CLOTHING_STORE to "옷 가게",
        Place.Type.DEPARTMENT_STORE to "백화점",
        Place.Type.ELECTRONICS_STORE to "전자제품점",
        Place.Type.POINT_OF_INTEREST to "관심 장소" // 가장 일반적인 유형
    )

    // 더 구체적인 유형에 우선순위를 부여하기 위한 리스트
    private val priorityOrder = listOf(
        Place.Type.SHOPPING_MALL, Place.Type.RESTAURANT, Place.Type.CAFE,
        Place.Type.MUSEUM, Place.Type.PARK, Place.Type.TOURIST_ATTRACTION,
        Place.Type.DEPARTMENT_STORE, Place.Type.ART_GALLERY
    )

    /**
     * 장소의 유형(types) 리스트를 받아 가장 적절한 한국어 유형을 반환합니다.
     */
    fun getKoreanType(types: List<Place.Type>?): String? {
        if (types.isNullOrEmpty()) return null

        // 1. 우선순위 목록에 있는 유형이 있는지 먼저 확인
        for (priorityType in priorityOrder) {
            if (types.contains(priorityType)) {
                return typeMap[priorityType]
            }
        }

        // 2. 우선순위 목록에 없다면, 전체 맵에서 가장 먼저 발견되는 유형을 반환
        for (type in types) {
            if (typeMap.containsKey(type)) {
                return typeMap[type]
            }
        }

        return null // 해당하는 한국어 유형이 없으면 null 반환
    }
}