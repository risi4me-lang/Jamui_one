package com.example.jamuione.data.repository

import android.util.Log
import com.example.jamuione.BuildConfig
import com.example.jamuione.data.local.dao.UserDao
import com.example.jamuione.data.local.entity.UserEntity
import com.example.jamuione.domain.model.CommunityStats
import com.example.jamuione.domain.model.User
import com.example.jamuione.domain.repository.UserRepository
import com.example.jamuione.util.Resource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.firestore.AggregateField
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth,
    private val userDao: UserDao
) : UserRepository {

    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    override fun getUserProfile(uid: String): Flow<Resource<User?>> = callbackFlow {
        trySend(Resource.Loading())
        val docRef = firestore.collection("users").document(uid)
        val listener = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(Resource.Error(error.message ?: "Failed to fetch user"))
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val user = snapshot.toObject(User::class.java)
                user?.let {
                    repositoryScope.launch {
                        userDao.upsertUser(it.toEntity())
                    }
                }
                trySend(Resource.Success(user))
            } else {
                trySend(Resource.Success(null))
            }
        }
        awaitClose { listener.remove() }
    }

    override fun createUserProfile(user: User): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            val finalUser = user.copy(
                joinedAt = System.currentTimeMillis(),
                lastSeen = System.currentTimeMillis(),
                createdAt = System.currentTimeMillis()
            )
            withTimeout(20000L) {
                firestore.collection("users").document(finalUser.uid)
                    .set(finalUser)
                    .await()
            }
            userDao.upsertUser(finalUser.toEntity())
            emit(Resource.Success(true))
        } catch (e: Exception) {
            com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().recordException(e)
            emit(Resource.Error(e.message ?: "Failed to create profile"))
        }
    }

    override fun saveUserProfile(user: User): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            val finalUser = user.copy(updatedAt = System.currentTimeMillis())
            withTimeout(20000L) {
                firestore.collection("users").document(finalUser.uid)
                    .set(finalUser)
                    .await()
            }
            userDao.upsertUser(finalUser.toEntity())
            emit(Resource.Success(true))
        } catch (e: Exception) {
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

    override fun getCommunityMembers(
        nativeDistrict: String,
        currentLocality: String?,
        currentDistrict: String?,
        currentState: String?,
        section: String
    ): Flow<Resource<List<User>>> = callbackFlow {
        trySend(Resource.Loading())
        
        var query = firestore.collection("users")
            .whereEqualTo("nativeDistrict", nativeDistrict.trim().lowercase())
            .whereEqualTo("showInCommunity", true)

        when (section) {
            "locality" -> {
                query = query.whereEqualTo("locality", currentLocality?.trim()?.lowercase() ?: "")
            }
            "district" -> {
                query = query.whereEqualTo("district", currentDistrict?.trim()?.lowercase() ?: "")
            }
            // "state" or "hometown" doesn't need additional filters on current location
        }

        query = query.orderBy("joinedAt", Query.Direction.DESCENDING)
        
        if (section == "locality") query = query.limit(10)
        else query = query.limit(50)

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(Resource.Error(error.message ?: "Failed to fetch community"))
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val users = snapshot.toObjects(User::class.java)
                repositoryScope.launch {
                    // We don't clear entire community, just update the section for these users
                    users.forEach {
                        userDao.upsertUser(it.toEntity(isMember = true, section = section))
                    }
                }
                trySend(Resource.Success(users))
            }
        }
        awaitClose { listener.remove() }
    }

    override fun getCachedCommunityMembers(section: String): Flow<List<User>> {
        return userDao.getNativeCommunityBySection(section).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getCommunityStats(nativeDistrict: String): Flow<Resource<CommunityStats>> = flow {
        emit(Resource.Loading())
        try {
            val baseQuery = firestore.collection("users")
                .whereEqualTo("nativeDistrict", nativeDistrict.trim().lowercase())
                .whereEqualTo("showInCommunity", true)

            val totalTask = baseQuery.count().get(AggregateSource.SERVER)
            val verifiedTask = baseQuery.whereEqualTo("isVerified", true).count().get(AggregateSource.SERVER)
            val professionalsTask = baseQuery.whereNotEqualTo("profession", "").count().get(AggregateSource.SERVER)
            val bloodDonorsTask = baseQuery.whereEqualTo("isBloodDonor", true).count().get(AggregateSource.SERVER)

            val total = totalTask.await().count
            val verified = verifiedTask.await().count
            val professionals = professionalsTask.await().count
            val bloodDonors = bloodDonorsTask.await().count

            emit(Resource.Success(CommunityStats(total, verified, professionals, bloodDonors)))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to fetch stats"))
        }
    }

    override fun getLocalityResidents(locality: String): Flow<Resource<List<User>>> = callbackFlow {
        trySend(Resource.Loading())
        val query = firestore.collection("users")
            .whereEqualTo("locality", locality.trim().lowercase())
            .orderBy("joinedAt", Query.Direction.DESCENDING)
            .limit(50)

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(Resource.Error(error.message ?: "Failed to fetch residents"))
                return@addSnapshotListener
            }
            if (snapshot != null) {
                trySend(Resource.Success(snapshot.toObjects(User::class.java)))
            }
        }
        awaitClose { listener.remove() }
    }

    override fun getDistrictResidents(district: String): Flow<Resource<List<User>>> = callbackFlow {
        trySend(Resource.Loading())
        val query = firestore.collection("users")
            .whereEqualTo("district", district.trim().lowercase())
            .orderBy("joinedAt", Query.Direction.DESCENDING)
            .limit(50)

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(Resource.Error(error.message ?: "Failed to fetch neighbors"))
                return@addSnapshotListener
            }
            if (snapshot != null) {
                trySend(Resource.Success(snapshot.toObjects(User::class.java)))
            }
        }
        awaitClose { listener.remove() }
    }

    override fun getDistrictMemberCount(district: String): Flow<Resource<Long>> = flow {
        emit(Resource.Loading())
        try {
            val count = firestore.collection("users")
                .whereEqualTo("district", district.trim().lowercase())
                .count()
                .get(AggregateSource.SERVER)
                .await()
                .count
            emit(Resource.Success(count))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to fetch count"))
        }
    }

    override fun deleteAccount(uid: String): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            val timestamp = System.currentTimeMillis()
            // TODO: clean up Storage images (profile + post photos) for this user
            
            // Delete user document subcollections
            val savedPostsSnapshot = firestore.collection("users").document(uid)
                .collection("savedPosts").get().await()
            val cleanupBatch = firestore.batch()
            savedPostsSnapshot.documents.forEach { cleanupBatch.delete(it.reference) }
            cleanupBatch.commit().await()
            
            // Delete user document
            firestore.collection("users").document(uid).delete().await()
            
            // Anonymize this user's comments across all posts (preserve thread structure)
            val commentsSnapshot = firestore.collectionGroup("comments")
                .whereEqualTo("userId", uid)
                .get().await()
            val commentsBatch = firestore.batch()
            commentsSnapshot.documents.forEach { doc ->
                commentsBatch.update(doc.reference, mapOf(
                    "userName" to "Deleted User",
                    "userProfileImage" to null
                ))
            }
            commentsBatch.commit().await()

            // Remove this user's likes and decrement the corresponding post's likesCount
            val likesSnapshot = firestore.collectionGroup("likes")
                .whereEqualTo("userId", uid)
                .get().await()
            val likesBatch = firestore.batch()
            likesSnapshot.documents.forEach { doc ->
                likesBatch.delete(doc.reference)
                val postRef = doc.reference.parent.parent
                if (postRef != null) {
                    likesBatch.update(postRef, "likesCount", com.google.firebase.firestore.FieldValue.increment(-1))
                }
            }
            likesBatch.commit().await()

            // Soft delete posts
            val postsSnapshot = firestore.collection("posts")
                .whereEqualTo("userId", uid)
                .get().await()
            
            val batch = firestore.batch()
            postsSnapshot.documents.forEach { doc ->
                batch.update(doc.reference, mapOf(
                    "isDeleted" to true,
                    "deletedAt" to timestamp,
                    "userName" to "Deleted User",
                    "userProfileImage" to null
                ))
            }
            
            // Soft delete notices
            val noticesSnapshot = firestore.collection("notices")
                .whereEqualTo("userId", uid)
                .get().await()
                
            noticesSnapshot.documents.forEach { doc ->
                batch.update(doc.reference, mapOf(
                    "isDeleted" to true,
                    "deletedAt" to timestamp,
                    "userName" to "Deleted User",
                    "userProfileImage" to null
                ))
            }

            batch.commit().await()
            
            // Delete Firebase Auth account
            try {
                firebaseAuth.currentUser?.delete()?.await()
            } catch (e: FirebaseAuthRecentLoginRequiredException) {
                emit(Resource.Error("For security, please log out and log back in, then try deleting your account again."))
                return@flow
            }
            
            userDao.clearUser()
            
            emit(Resource.Success(true))
        } catch (e: Exception) {
            com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().recordException(e)
            emit(Resource.Error(e.message ?: "Failed to delete account"))
        }
    }

    override fun getCachedUser(uid: String): Flow<User?> {
        return userDao.getUser(uid).map { it?.toDomain() }
    }

    private fun User.toEntity(isMember: Boolean = false, section: String? = null) = UserEntity(
        uid = uid,
        name = name,
        email = email,
        state = state,
        district = district,
        locality = locality,
        nativeState = nativeState,
        nativeDistrict = nativeDistrict,
        profession = profession,
        company = company,
        bio = bio,
        isBloodDonor = isBloodDonor,
        profileImage = profileImage,
        profileCompleted = profileCompleted,
        isVerified = isVerified,
        showInCommunity = showInCommunity,
        joinedAt = joinedAt,
        isDeleted = isDeleted,
        isNativeCommunityMember = isMember,
        communitySection = section
    )

    private fun UserEntity.toDomain() = User(
        uid = uid,
        name = name,
        email = email,
        state = state,
        district = district,
        locality = locality,
        nativeState = nativeState,
        nativeDistrict = nativeDistrict,
        profession = profession,
        company = company,
        bio = bio,
        isBloodDonor = isBloodDonor,
        profileImage = profileImage,
        profileCompleted = profileCompleted,
        isVerified = isVerified,
        showInCommunity = showInCommunity,
        joinedAt = joinedAt,
        isDeleted = isDeleted
    )
}
