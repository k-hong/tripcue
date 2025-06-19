package com.example.tripcue.frame.uicomponents.Schedule

import com.example.tripcue.frame.model.ScheduleData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Firebase Firestore를 사용하여 스케줄 데이터를 관리하는 저장소 클래스
 */
class ScheduleRepository {

    // Firebase 인스턴스 초기화
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // 현재 로그인된 사용자의 스케줄 컬렉션 참조
    private val collectionRef
        get() = firestore.collection("users")
            .document(auth.currentUser?.uid ?: throw IllegalStateException("User not logged in"))
            .collection("schedules")

    /**
     * Firestore에 새 스케줄 추가
     * @param schedule 추가할 스케줄 데이터
     */
    suspend fun addSchedule(schedule: ScheduleData) {
        collectionRef.add(schedule).await()
    }

    /**
     * 실시간으로 모든 스케줄 데이터를 구독해서 가져오는 Flow
     * - snapshotListener를 사용하여 변경 사항 감지
     * @return 스케줄 목록 Flow
     */
    fun getSchedules() = callbackFlow<List<ScheduleData>> {
        val listener = collectionRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                // 에러 발생 시 스트림 종료
                close(error)
                return@addSnapshotListener
            }

            // 스냅샷이 유효하면 ScheduleData 객체로 변환하여 전송
            if (snapshot != null) {
                val schedules = snapshot.documents.mapNotNull { it.toObject(ScheduleData::class.java) }
                trySend(schedules).isSuccess
            }
        }

        // 리스너 해제
        awaitClose { listener.remove() }
    }

    /**
     * 위치와 날짜를 기준으로 기존 스케줄이 존재하면 수정하고, 없으면 새로 추가
     * @param updatedSchedule 수정 또는 추가할 스케줄 데이터
     */
    suspend fun updateSchedule(updatedSchedule: ScheduleData) {
        val snapshot = collectionRef
            .whereEqualTo("location", updatedSchedule.location)
            .whereEqualTo("date", updatedSchedule.date)
            .get()
            .await()

        if (!snapshot.isEmpty) {
            // 해당 위치와 날짜에 맞는 문서가 있으면 업데이트
            val docId = snapshot.documents.first().id
            collectionRef.document(docId)
                .set(updatedSchedule)
                .await()
        } else {
            // 없으면 새로 추가
            addSchedule(updatedSchedule)
        }
    }

    // 필요한 경우: 삭제, 단일 조회, 필터 등 추가 가능
}
