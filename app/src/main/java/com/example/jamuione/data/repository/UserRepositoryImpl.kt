package com.example.jamuione.data.repository

import android.util.Log
import com.example.jamuione.BuildConfig
import com.example.jamuione.domain.model.User
import com.example.jamuione.domain.repository.UserRepository
import com.example.jamuione.util.Resource
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : UserRepository {

    override fun getUserProfile(uid: String): Flow<Resource<User?>> = callbackFlow {
        trySend(Resource.Loading())
        if (BuildConfig.DEBUG) {
            Log.d("AUTH_TRACE", "Fetching Firestore user: $uid")
        }
        val docRef = firestore.collection("users").document(uid)
        val listener = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("AUTH_TRACE", "Firestore fetch failed", error)
                trySend(Resource.Error(error.message ?: "Failed to fetch user"))
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val user = snapshot.toObject(User::class.java)
                Log.d("AUTH_TRACE", "Firestore fetch success: profileCompleted=${user?.profileCompleted}")
                trySend(Resource.Success(user))
            } else {
                Log.d("AUTH_TRACE", "Firestore fetch success: document not found")
                trySend(Resource.Success(null))
            }
        }
        awaitClose { listener.remove() }
    }

    override fun createUserProfile(user: User): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        if (BuildConfig.DEBUG) {
            Log.d("AUTH_TRACE", "Creating Firestore user: ${user.uid}")
        }
        try {
            Log.d("AUTH_TRACE", "Firestore write started")
            withTimeout(20000L) {
                firestore.collection("users").document(user.uid)
                    .set(user)
                    .await()
            }
            Log.d("AUTH_TRACE", "Firestore write success")
            emit(Resource.Success(true))
        } catch (e: TimeoutCancellationException) {
            Log.w("AUTH_TRACE", "Firestore write timed out, likely syncing in background")
            emit(Resource.Error("Taking longer than usual — it'll finish syncing in the background."))
        } catch (e: Exception) {
            Log.e("AUTH_TRACE", "Firestore write failed", e)
            emit(Resource.Error(e.message ?: "Failed to create profile"))
        }
    }

    override fun saveUserProfile(user: User): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        if (BuildConfig.DEBUG) {
            Log.d("AUTH_TRACE", "Firestore profile save started for UID: ${user.uid}")
        }
        try {
            withTimeout(20000L) {
                firestore.collection("users").document(user.uid)
                    .set(user)
                    .await()
            }
            Log.d("AUTH_TRACE", "Firestore profile save success")
            emit(Resource.Success(true))
        } catch (e: TimeoutCancellationException) {
            Log.w("AUTH_TRACE", "Firestore profile save timed out, likely syncing in background")
            emit(Resource.Error("Taking longer than usual — it'll finish syncing in the background."))
        } catch (e: Exception) {
            Log.e("AUTH_TRACE", "Firestore profile save failed", e)
            emit(Resource.Error(e.message ?: "Failed to save profile"))
        }
    }

    override fun updateUserFcmToken(uid: String, token: String): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            firestore.collection("users").document(uid)
                .update("fcmToken", token, "updatedAt", System.currentTimeMillis())
                .await()
            emit(Resource.Success(true))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to update FCM token"))
        }
    }
}
