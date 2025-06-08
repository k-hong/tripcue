// ✅ NaverPlaceApiService.kt (Retrofit 구성 포함)
package com.example.tripcue.frame.uicomponents.home

import com.google.gson.annotations.SerializedName
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

// 🔹 1. 결과 아이템 데이터 클래스
data class NaverPlaceItem(
    @SerializedName("title") val title: String,
    @SerializedName("link") val link: String,
    @SerializedName("category") val category: String,
    @SerializedName("address") val address: String,
    @SerializedName("mapx") val mapX: String,
    @SerializedName("mapy") val mapY: String
)

// 🔹 2. 검색 응답 데이터 클래스
data class NaverSearchResponse(
    @SerializedName("items") val items: List<NaverPlaceItem>
)

// 🔹 3. Retrofit 인터페이스
interface NaverLocalSearchApi {
    @GET("v1/search/local.json")
    suspend fun searchPlaces(
        @Query("query") query: String,
        @Query("display") display: Int = 5,
        @Query("sort") sort: String = "random"
    ): Response<NaverSearchResponse>
}

// 🔹 4. Retrofit 객체 생성기
object NaverPlaceApiService {
    private const val BASE_URL = "https://openapi.naver.com/"

    private const val CLIENT_ID = ""

    private const val CLIENT_SECRET =""

    private val client = OkHttpClient.Builder()
        .addInterceptor(Interceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("X-Naver-Client-Id", CLIENT_ID)
                .addHeader("X-Naver-Client-Secret", CLIENT_SECRET)
                .build()
            chain.proceed(request)
        })
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val api: NaverLocalSearchApi = retrofit.create(NaverLocalSearchApi::class.java)
}
