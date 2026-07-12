package com.example.jamuione.data.repository

import com.example.jamuione.domain.model.AppNotification
import com.example.jamuione.domain.repository.NotificationRepository
import com.example.jamuione.util.Resource
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class NotificationRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : NotificationRepository {

    override fun getNotifications(userId: String): Flow<Resource<List<AppNotification>>> = callbackFlow {
        trySend(Resource.Loading())
        
        val query = firestore.collection("users").document(userId)
            .collection("notifications")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50)

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(Resource.Error(error.message ?: "Failed to fetch notifications"))
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val notifications = snapshot.toObjects(AppNotification::class.java)
                trySend(Resource.Success(notifications))
            }
        }

        awaitClose { listener.remove() }
    }

    override fun markAsRead(userId: String, notificationId: String): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            firestore.collection("users").document(userId)
                .collection("notifications").document(notificationId)
                .update("isRead", true)
                .await()
            emit(Resource.Success(true))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to update notification"))
        }
    }

    override fun getUnreadCount(userId: String): Flow<Int> = callbackFlow {
        val query = firestore.collection("users").document(userId)
            .collection("notifications")
            .whereEqualTo("isRead", false)

        val listener = query.addSnapshotListener { snapshot, _ ->
            trySend(snapshot?.size() ?: 0)
        }
        awaitClose { listener.remove() }
    }

    override fun deleteNotification(userId: String, notificationId: String): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            firestore.collection("users").document(userId)
                .collection("notifications").document(notificationId)
                .delete()
                .await()
            emit(Resource.Success(true))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to delete notification"))
        }
    }
}
