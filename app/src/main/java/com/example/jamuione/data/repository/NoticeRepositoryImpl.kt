package com.example.jamuione.data.repository

import android.util.Log
import com.example.jamuione.domain.model.Notice
import com.example.jamuione.domain.repository.NoticeRepository
import com.example.jamuione.util.Resource
import com.google.firebase.firestore.AggregateSource
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
import java.util.Calendar
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

        query = if (!normalizedSearch.isNullOrBlank()) {
            query.orderBy("searchableTitle")
                .startAt(normalizedSearch)
                .endAt(normalizedSearch + "\uf8ff")
                .limit(30)
        } else {
            query.whereGreaterThan("expiryDate", System.currentTimeMillis())
                .orderBy("expiryDate", Query.Direction.ASCENDING)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(30)
        }

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("FIRESTORE_DEBUG", "getNotices: failed", error)
                trySend(Resource.Error(error.message ?: "Failed to fetch notices"))
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val notices = snapshot.toObjects(Notice::class.java)
                val filtered = if (!normalizedSearch.isNullOrBlank()) {
                    notices.filter { it.expiryDate > System.currentTimeMillis() }
                } else {
                    notices
                }
                Log.d("FIRESTORE_DEBUG", "getNotices: success, count=${filtered.size}")
                trySend(Resource.Success(filtered))
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
            com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().recordException(e)
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

    override fun reportNotice(noticeId: String, reporterId: String, reason: String): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            val reportId = UUID.randomUUID().toString()
            val report = mapOf(
                "id" to reportId,
                "reporterId" to reporterId,
                "targetId" to noticeId,
                "targetType" to "notice",
                "reason" to reason,
                "createdAt" to System.currentTimeMillis()
            )
            firestore.collection("reports").document(reportId).set(report).await()
            com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().log("Notice Reported: $noticeId by $reporterId. Reason: $reason")
            emit(Resource.Success(true))
        } catch (e: Exception) {
            com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().recordException(e)
            emit(Resource.Error(e.message ?: "Failed to submit report"))
        }
    }

    override fun getTodayNoticeCount(userId: String): Flow<Resource<Int>> = flow {
        emit(Resource.Loading())
        try {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startOfDay = calendar.timeInMillis

            val count = firestore.collection("notices")
                .whereEqualTo("userId", userId)
                .whereGreaterThanOrEqualTo("createdAt", startOfDay)
                .count()
                .get(AggregateSource.SERVER)
                .await()
                .count
            
            emit(Resource.Success(count.toInt()))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to check limit"))
        }
    }

    override fun voteInPoll(noticeId: String, userId: String, optionIndex: Int): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            firestore.runTransaction { transaction ->
                val noticeRef = firestore.collection("notices").document(noticeId)
                val snapshot = transaction.get(noticeRef)
                
                val userVotes = snapshot.get("userVotes") as? MutableMap<String, Long> ?: mutableMapOf()
                val pollVotes = snapshot.get("pollVotes") as? MutableMap<String, Long> ?: mutableMapOf()
                
                val previousVote = userVotes[userId]?.toInt()
                if (previousVote == optionIndex) return@runTransaction

                if (previousVote != null) {
                    val prevCount = pollVotes[previousVote.toString()] ?: 0L
                    pollVotes[previousVote.toString()] = (prevCount - 1).coerceAtLeast(0)
                }

                userVotes[userId] = optionIndex.toLong()
                val newCount = pollVotes[optionIndex.toString()] ?: 0L
                pollVotes[optionIndex.toString()] = newCount + 1

                transaction.update(noticeRef, "userVotes", userVotes)
                transaction.update(noticeRef, "pollVotes", pollVotes)
            }.await()
            emit(Resource.Success(true))
        } catch (e: Exception) {
            com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().recordException(e)
            emit(Resource.Error(e.message ?: "Failed to vote"))
        }
    }
}
