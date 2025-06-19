package com.example.tripcue.frame.uicomponents.home

import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object TranslationUtils {
    // 한국어 -> 영어 번역기 옵션 설정
    private val options = TranslatorOptions.Builder()
        .setSourceLanguage(TranslateLanguage.KOREAN)
        .setTargetLanguage(TranslateLanguage.ENGLISH)
        .build()

    private val translator = Translation.getClient(options)


    suspend fun translateToEnglish(text: String): String = withContext(Dispatchers.IO) {
        try {
            // 번역 모델 다운로드가 필요한지 확인하고, 필요 시 다운로드 (와이파이 연결 시)
            val conditions = DownloadConditions.Builder()
                .requireWifi()
                .build()
            Tasks.await(translator.downloadModelIfNeeded(conditions))

            // 번역 실행
            val translatedText = Tasks.await(translator.translate(text))
            Log.d("TranslationUtils", "'$text' -> '$translatedText' 번역 성공")
            return@withContext translatedText
        } catch (e: Exception) {
            Log.e("TranslationUtils", "'$text' 번역 실패", e)
            return@withContext text // 번역 실패 시 원본 텍스트 반환
        }
    }
}