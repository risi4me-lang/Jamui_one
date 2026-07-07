package com.example.jamuione.data.repository

import android.util.Log
import com.example.jamuione.domain.model.Notice
import com.example.jamuione.domain.model.Report
import com.example.jamuione.domain.repository.NoticeRepository
import com.example.jamuione.util.Resource
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import java.util.UUID
import javax.inject.Inject

class NoticeRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val fcm: FirebaseMessaging
) : NoticeRepository {

    override fun getNotices(
        category: String?,
        locality: String?,
        district: String?,
        state: String?,
        searchQuery: String?
    ): Flow<Resource<List<Notice>>> = callbackFlow {
        trySend(Resource.Loading())
        val trimmedLocality = locality?.trim()?.lowercase()
        val trimmedDistrict = district?.trim()?.lowercase()
        val trimmedState = state?.trim()?.lowercase()
        val normalizedSearch = searchQuery?.trim()?.lowercase()
        
        Log.d("FIRESTORE_DEBUG", "getNotices: category=$category, locality=$trimmedLocality, district=$trimmedDistrict, state=$trimmedState, search=$normalizedSearch")

        var query: Query = firestore.collection("notices")
            .whereGreaterThan("expiryDate", System.currentTimeMillis())

        if (category != null) {
            query = query.whereEqualTo("category", category)
        }

        if (trimmedLocality != null) {
            query = query.whereEqualTo("locality", trimmedLocality)
        } else if (trimmedDistrict != null) {
            query = query.whereEqualTo("district", trimmedDistrict)
        } else if (trimmedState != null) {
            query = query.whereEqualTo("state", trimmedState)
        }

        if (!normalizedSearch.isNullOrBlank()) {
            query = query.orderBy("searchableTitle")
                .startAt(normalizedSearch)
                .endAt(normalizedSearch + "\uf8ff")
        } else {
            query = query.orderBy("expiryDate", Query.Direction.ASCENDING)
                .orderBy("createdAt", Query.Direction.DESCENDING)
        }

        query = query.limit(30)

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("FIRESTORE_DEBUG", "getNotices: failed", error)
                trySend(Resource.Error(error.message ?: "Failed to fetch notices"))
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val notices = snapshot.toObjects(Notice::class.java)
                Log.d("FIRESTORE_DEBUG", "getNotices: success, count=${notices.size}")
                trySend(Resource.Success(notices))
            }
        }

        awaitClose { listener.remove() }
    }

    override fun createNotice(notice: Notice): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        Log.d("NOTICE_DEBUG", "createNotice: started")
        try {
            val noticeId = UUID.randomUUID().toString()
            val finalNotice = notice.copy(
                id = noticeId, 
                searchableTitle = notice.title.trim().lowercase(),
                locality = notice.locality.trim().lowercase(),
                district = notice.district.trim().lowercase(),
                state = notice.state.trim().lowercase(),
                createdAt = System.currentTimeMillis()
            )
            
            Log.d("FIRESTORE_DEBUG", "createNotice: writing to firestore, id=$noticeId")
            withTimeout(20000L) {
                firestore.collection("notices").document(noticeId)
                    .set(finalNotice)
                    .await()
            }
            Log.d("NOTICE_DEBUG", "createNotice: success")
            emit(Resource.Success(true))
        } catch (e: TimeoutCancellationException) {
            Log.w("NOTICE_DEBUG", "createNotice timed out, likely syncing in background")
            emit(Resource.Error("Posting notice is taking longer than usual — it'll finish syncing in the background."))
        } catch (e: Exception) {
            Log.e("NOTICE_DEBUG", "createNotice: failed", e)
            emit(Resource.Error(e.message ?: "Failed to create notice"))
        }
    }

    override fun subscribeToTopic(topic: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        Log.d("NOTICE_DEBUG", "subscribeToTopic: $topic")
        try {
            fcm.subscribeToTopic(topic).await()
            Log.d("NOTICE_DEBUG", "subscribeToTopic: success")
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            Log.e("NOTICE_DEBUG", "subscribeToTopic: failed", e)
            emit(Resource.Error(e.message ?: "Topic subscription failed"))
        }
    }

    override fun deleteNotice(noticeId: String): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            firestore.collection("notices").document(noticeId).delete().await()
            emit(Resource.Success(true))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to delete notice"))
        }
    }

    override fun deleteExpiredNotices(): Flow<Resource<Int>> = flow {
        emit(Resource.Loading())
        try {
            val snapshot = firestore.collection("notices")
                .whereLessThan("expiryDate", System.currentTimeMillis())
                .get()
                .await()
            
            val batch = firestore.batch()
            snapshot.documents.forEach { batch.delete(it.reference) }
            batch.commit().await()
            
            emit(Resource.Success(snapshot.size()))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to clear expired notices"))
        }
    }

    override fun reportNotice(noticeId: String, userId: String, reason: String): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            val reportId = UUID.randomUUID().toString()
            val report = Report(
                id = reportId,
                userId = userId,
                contentId = noticeId,
                contentType = "notice",
                reason = reason,
                timestamp = System.currentTimeMillis()
            )
            firestore.collection("reports").document(reportId).set(report).await()
            emit(Resource.Success(true))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to submit report"))
        }
    }
}
