package com.example.tripcue.frame.uicomponents.home

import com.example.tripcue.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import android.util.Log

object NaverPlaceApi {
    suspend fun searchPlaces(region: String, keywords: List<String>): List<String> {
        val client = OkHttpClient()
        val placeCount = mutableMapOf<String, Int>()

        for (keyword in keywords) {
            try {
                val query = "$region $keyword"
                val url = "https://openapi.naver.com/v1/search/local.json?query=${query}&display=10"
                Log.d("NaverAPI", "현재 Client ID = ${BuildConfig.NAVER_D_CLIENT_ID}")
                Log.d("NaverAPI", "현재 Client ID = ${BuildConfig.NAVER_D_CLIENT_SECRET}")

                Log.d("NaverAPI", "searchPlaces 요청 URL: $url")

                val request = Request.Builder()
                    .url(url)
                    .addHeader("X-Naver-Client-Id", BuildConfig.NAVER_D_CLIENT_ID)
                    .addHeader("X-Naver-Client-Secret", BuildConfig.NAVER_D_CLIENT_SECRET)
                    .build()

                val response = withContext(Dispatchers.IO) {
                    client.newCall(request).execute()
                }

                val jsonStr = response.body?.string()
                Log.d("NaverAPI", "searchPlaces 응답: $jsonStr")

                if (!response.isSuccessful || jsonStr == null) {
                    Log.e("NaverAPI", "searchPlaces 실패 코드: ${response.code}")
                    continue
                }

                val items = JSONObject(jsonStr).getJSONArray("items")
                for (i in 0 until items.length()) {
                    val title = items.getJSONObject(i).getString("title").replace(Regex("<[^>]*>"), "")
                    placeCount[title] = placeCount.getOrDefault(title, 0) + 1
                }
            } catch (e: Exception) {
                Log.e("NaverAPI", "searchPlaces 예외 발생", e)
                continue
            }
        }

        return placeCount.entries.sortedByDescending { it.value }.take(3).map { it.key }
    }

    suspend fun getHotPlaces(region: String): List<String> {
        val client = OkHttpClient()
        val url = "https://openapi.naver.com/v1/search/local.json?query=${region}&display=10&sort=comment"
        Log.d("NaverAPI", "getHotPlaces 요청 URL: $url")

        return try {
            val request = Request.Builder()
                .url(url)
                .addHeader("X-Naver-Client-Id", BuildConfig.NAVER_CLIENT_ID)
                .addHeader("X-Naver-Client-Secret", BuildConfig.NAVER_D_CLIENT_SECRET)
                .build()

            val response = withContext(Dispatchers.IO) {
                client.newCall(request).execute()
            }

            val jsonStr = response.body?.string()
            Log.d("NaverAPI", "getHotPlaces 응답: $jsonStr")

            if (!response.isSuccessful || jsonStr == null) {
                Log.e("NaverAPI", "❗ getHotPlaces 실패 코드: ${response.code}")
                return emptyList()
            }

            val items = JSONObject(jsonStr).getJSONArray("items")
            val result = mutableListOf<String>()
            for (i in 0 until items.length()) {
                val title = items.getJSONObject(i).getString("title").replace(Regex("<[^>]*>"), "")
                result.add(title)
            }
            return result.take(2)

        } catch (e: Exception) {
            Log.e("NaverAPI", "getHotPlaces 예외 발생", e)
            return emptyList()
        }
    }
}

