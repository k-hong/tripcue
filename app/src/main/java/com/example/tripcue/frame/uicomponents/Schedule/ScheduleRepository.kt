package com.example.tripcue.frame.uicomponents.Schedule

import com.example.tripcue.frame.model.ScheduleData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ScheduleRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val collectionRef
        get() = firestore.collection("users")
            .document(auth.currentUser?.uid ?: throw IllegalStateException("User not logged in"))
            .collection("schedules")

    // 스케줄 추가
    suspend fun addSchedule(schedule: ScheduleData) {
        collectionRef.add(schedule).await()
    }

    // 저장된 스케줄 전체 불러오기 (실시간 구독)
    fun getSchedules() = callbackFlow<List<ScheduleData>> {
        val listener = collectionRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val schedules = snapshot.documents.mapNotNull { it.toObject(ScheduleData::class.java) }
                trySend(schedules).isSuccess
            }
        }
        awaitClose { listener.remove() }
    }

    suspend fun updateSchedule(updatedSchedule: ScheduleData) {
        val snapshot = collectionRef
            .whereEqualTo("location", updatedSchedule.location)
            .whereEqualTo("date", updatedSchedule.date)
            .get()
            .await()

        if (!snapshot.isEmpty) {
            val docId = snapshot.documents.first().id
            collectionRef.document(docId)
                .set(updatedSchedule)
                .await()
        } else {
            addSchedule(updatedSchedule)
        }
    }

    // 스케줄 삭제, 수정 등 필요시 추가 가능
}