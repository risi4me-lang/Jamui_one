package com.example.jamuione.data.repository

import com.example.jamuione.domain.model.Notice
import com.example.jamuione.domain.repository.NoticeRepository
import com.example.jamuione.util.Resource
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
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
        state: String?
    ): Flow<Resource<List<Notice>>> = callbackFlow {
        trySend(Resource.Loading())

        var query: Query = firestore.collection("notices")
            .whereGreaterThan("expiryDate", System.currentTimeMillis())

        if (category != null) {
            query = query.whereEqualTo("category", category)
        }

        if (locality != null) {
            query = query.whereEqualTo("locality", locality)
        } else if (district != null) {
            query = query.whereEqualTo("district", district)
        } else if (state != null) {
            query = query.whereEqualTo("state", state)
        }

        query = query.orderBy("expiryDate", Query.Direction.ASCENDING)
        query = query.orderBy("createdAt", Query.Direction.DESCENDING)

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(Resource.Error(error.message ?: "Failed to fetch notices"))
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val notices = snapshot.toObjects(Notice::class.java)
                trySend(Resource.Success(notices))
            }
        }

        awaitClose { listener.remove() }
    }

    override fun createNotice(notice: Notice): Flow<Resource<Boolean>> = callbackFlow {
        trySend(Resource.Loading())
        try {
            val noticeId = UUID.randomUUID().toString()
            val finalNotice = notice.copy(id = noticeId, createdAt = System.currentTimeMillis())
            firestore.collection("notices").document(noticeId)
                .set(finalNotice)
                .await()
            trySend(Resource.Success(true))
        } catch (e: Exception) {
            trySend(Resource.Error(e.message ?: "Failed to create notice"))
        }
        awaitClose {}
    }

    override fun subscribeToTopic(topic: String): Flow<Resource<Unit>> = callbackFlow {
        trySend(Resource.Loading())
        fcm.subscribeToTopic(topic)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    trySend(Resource.Success(Unit))
                } else {
                    trySend(Resource.Error(task.exception?.message ?: "Topic subscription failed"))
                }
            }
        awaitClose {}
    }
}
