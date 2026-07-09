package com.example.jamuione.data.repository

import android.net.Uri
import android.util.Log
import com.example.jamuione.data.local.dao.PostDao
import com.example.jamuione.data.local.entity.PostEntity
import com.example.jamuione.domain.model.Comment
import com.example.jamuione.domain.model.Like
import com.example.jamuione.domain.model.Post
import com.example.jamuione.domain.repository.PostRepository
import com.example.jamuione.util.Resource
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
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
import java.util.Calendar
import java.util.UUID
import javax.inject.Inject

class PostRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
    private val postDao: PostDao
) : PostRepository {

    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    override fun getPosts(locality: String?, district: String?, state: String?): Flow<Resource<List<Post>>> = callbackFlow {
        trySend(Resource.Loading())
        val trimmedLocality = locality?.trim()?.lowercase()
        val trimmedDistrict = district?.trim()?.lowercase()
        val trimmedState = state?.trim()?.lowercase()
        
        Log.d("FIRESTORE_DEBUG", "getPosts: locality=$trimmedLocality, district=$trimmedDistrict, state=$trimmedState")
        
        var query: Query = firestore.collection("posts")
        
        if (trimmedLocality != null) {
            query = query.whereEqualTo("locality", trimmedLocality)
        } else if (trimmedDistrict != null) {
            query = query.whereEqualTo("district", trimmedDistrict)
        } else if (trimmedState != null) {
            query = query.whereEqualTo("state", trimmedState)
        }
        
        query = query.orderBy("timestamp", Query.Direction.DESCENDING).limit(20)

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("FIRESTORE_DEBUG", "getPosts: failed", error)
                trySend(Resource.Error(error.message ?: "Failed to fetch posts"))
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val posts = snapshot.toObjects(Post::class.java)
                Log.d("FIRESTORE_DEBUG", "getPosts: success, count=${posts.size}")
                
                if (locality != null) {
                    repositoryScope.launch {
                        val entities = posts.map { it.toEntity() }
                        postDao.clearAllPosts()
                        postDao.insertPosts(entities)
                    }
                }

                trySend(Resource.Success(posts))
            }
        }

        awaitClose { listener.remove() }
    }

    override fun createPost(post: Post, imageUri: Uri?): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        Log.d("POST_DEBUG", "createPost: started")
        
        try {
            var imageUrl: String? = null
            if (imageUri != null) {
                Log.d("POST_DEBUG", "createPost: uploading image")
                val fileName = UUID.randomUUID().toString()
                val ref = storage.reference.child("post_images/$fileName")
                ref.putFile(imageUri).await()
                imageUrl = ref.downloadUrl.await().toString()
                Log.d("POST_DEBUG", "createPost: image uploaded, url=$imageUrl")
            }

            val postId = UUID.randomUUID().toString()
            val finalPost = post.copy(
                id = postId,
                imageUrl = imageUrl,
                locality = post.locality.trim().lowercase(),
                district = post.district.trim().lowercase(),
                state = post.state.trim().lowercase(),
                timestamp = System.currentTimeMillis()
            )

            Log.d("FIRESTORE_DEBUG", "createPost: writing to firestore, id=$postId")
            withTimeout(20000L) {
                firestore.collection("posts").document(postId)
                    .set(finalPost)
                    .await()
            }
            Log.d("POST_DEBUG", "createPost: success")
            emit(Resource.Success(true))
        } catch (e: TimeoutCancellationException) {
            Log.w("POST_DEBUG", "createPost timed out, likely syncing in background")
            emit(Resource.Error("Posting is taking longer than usual — it'll finish syncing in the background."))
        } catch (e: Exception) {
            Log.e("POST_DEBUG", "createPost: failed", e)
            com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().recordException(e)
            emit(Resource.Error(e.message ?: "Failed to create post"))
        }
    }

    override fun getCachedPosts(): Flow<List<Post>> {
        return postDao.getAllPosts().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun deletePost(postId: String): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            firestore.collection("posts").document(postId).delete().await()
            emit(Resource.Success(true))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to delete post"))
        }
    }

    override fun toggleLike(
        postId: String,
        userId: String,
        userName: String,
        userProfileImage: String?,
        isVerified: Boolean
    ): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            withTimeout(20000L) {
                firestore.runTransaction { transaction ->
                    val postRef = firestore.collection("posts").document(postId)
                    val likeRef = postRef.collection("likes").document(userId)
                    
                    val likeSnapshot = transaction.get(likeRef)
                    if (likeSnapshot.exists()) {
                        transaction.delete(likeRef)
                        transaction.update(postRef, "likesCount", FieldValue.increment(-1))
                    } else {
                        val like = Like(userId, userName, userProfileImage, isVerified, System.currentTimeMillis())
                        transaction.set(likeRef, like)
                        transaction.update(postRef, "likesCount", FieldValue.increment(1))
                    }
                }.await()
            }
            emit(Resource.Success(true))
        } catch (e: TimeoutCancellationException) {
            emit(Resource.Error("Action timed out. Please try again."))
        } catch (e: Exception) {
            com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().recordException(e)
            emit(Resource.Error(e.message ?: "Failed to toggle like"))
        }
    }

    override fun observeIsLikedByUser(postId: String, userId: String): Flow<Boolean> = callbackFlow {
        val likeRef = firestore.collection("posts").document(postId)
            .collection("likes").document(userId)
        
        val listener = likeRef.addSnapshotListener { snapshot, _ ->
            trySend(snapshot?.exists() == true)
        }
        awaitClose { listener.remove() }
    }

    override fun getLikers(postId: String): Flow<Resource<List<Like>>> = flow {
        emit(Resource.Loading())
        try {
            val snapshot = firestore.collection("posts").document(postId)
                .collection("likes")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()
            val likers = snapshot.toObjects(Like::class.java)
            emit(Resource.Success(likers))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to fetch likers"))
        }
    }

    override fun addComment(postId: String, comment: Comment): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            val commentId = UUID.randomUUID().toString()
            val finalComment = comment.copy(id = commentId, postId = postId, timestamp = System.currentTimeMillis())
            
            withTimeout(20000L) {
                firestore.runTransaction { transaction ->
                    val postRef = firestore.collection("posts").document(postId)
                    val commentRef = postRef.collection("comments").document(commentId)
                    
                    transaction.set(commentRef, finalComment)
                    transaction.update(postRef, "commentsCount", FieldValue.increment(1))
                }.await()
            }
            emit(Resource.Success(true))
        } catch (e: TimeoutCancellationException) {
            emit(Resource.Error("Comment failed to sync. It will appear once you are online."))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to add comment"))
        }
    }

    override fun getComments(postId: String): Flow<Resource<List<Comment>>> = callbackFlow {
        trySend(Resource.Loading())
        val listener = firestore.collection("posts").document(postId)
            .collection("comments")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(error.message ?: "Failed to fetch comments"))
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val comments = snapshot.toObjects(Comment::class.java)
                    trySend(Resource.Success(comments))
                }
            }
        awaitClose { listener.remove() }
    }

    override fun reportPost(postId: String, reporterId: String, reason: String): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            val reportId = UUID.randomUUID().toString()
            val report = mapOf(
                "id" to reportId,
                "reporterId" to reporterId,
                "targetId" to postId,
                "targetType" to "post",
                "reason" to reason,
                "createdAt" to System.currentTimeMillis()
            )
            firestore.collection("reports").document(reportId).set(report).await()
            com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().log("Post Reported: $postId by $reporterId. Reason: $reason")
            emit(Resource.Success(true))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to submit report"))
        }
    }

    override fun reportComment(postId: String, commentId: String, reporterId: String, reason: String): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            val reportId = UUID.randomUUID().toString()
            val report = mapOf(
                "id" to reportId,
                "reporterId" to reporterId,
                "targetId" to commentId,
                "targetType" to "comment",
                "parentPostId" to postId,
                "reason" to reason,
                "createdAt" to System.currentTimeMillis()
            )
            firestore.collection("reports").document(reportId).set(report).await()
            com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().log("Comment Reported: $commentId by $reporterId. Reason: $reason")
            emit(Resource.Success(true))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to submit report"))
        }
    }

    override fun toggleSavePost(postId: String, userId: String): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            withTimeout(20000L) {
                firestore.runTransaction { transaction ->
                    val savedPostRef = firestore.collection("users").document(userId)
                        .collection("savedPosts").document(postId)
                    
                    if (transaction.get(savedPostRef).exists()) {
                        transaction.delete(savedPostRef)
                    } else {
                        val data = mapOf("postId" to postId, "savedAt" to System.currentTimeMillis())
                        transaction.set(savedPostRef, data)
                    }
                }.await()
            }
            emit(Resource.Success(true))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to save post"))
        }
    }

    override fun observeIsSavedByUser(postId: String, userId: String): Flow<Boolean> = callbackFlow {
        val savedPostRef = firestore.collection("users").document(userId)
            .collection("savedPosts").document(postId)
        val listener = savedPostRef.addSnapshotListener { snapshot, _ ->
            trySend(snapshot?.exists() == true)
        }
        awaitClose { listener.remove() }
    }

    override fun getSavedPosts(userId: String): Flow<Resource<List<Post>>> = flow {
        emit(Resource.Loading())
        try {
            val savedSnapshot = firestore.collection("users").document(userId)
                .collection("savedPosts")
                .orderBy("savedAt", Query.Direction.DESCENDING)
                .get()
                .await()
            
            val postIds = savedSnapshot.documents.mapNotNull { it.getString("postId") }
            if (postIds.isEmpty()) {
                emit(Resource.Success(emptyList()))
                return@flow
            }

            // Batch fetch posts using whereIn (up to 30)
            val posts = mutableListOf<Post>()
            val chunks = postIds.chunked(30)
            for (chunk in chunks) {
                val postSnapshot = firestore.collection("posts")
                    .whereIn("id", chunk)
                    .get()
                    .await()
                posts.addAll(postSnapshot.toObjects(Post::class.java))
            }
            
            // Sort to match saved order
            val sortedPosts = postIds.mapNotNull { id -> posts.find { it.id == id } }
            emit(Resource.Success(sortedPosts))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to fetch saved posts"))
        }
    }

    override fun getTodayPostCount(userId: String): Flow<Resource<Int>> = flow {
        emit(Resource.Loading())
        try {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startOfDay = calendar.timeInMillis

            val count = firestore.collection("posts")
                .whereEqualTo("userId", userId)
                .whereGreaterThanOrEqualTo("timestamp", startOfDay)
                .count()
                .get(AggregateSource.SERVER)
                .await()
                .count
            
            emit(Resource.Success(count.toInt()))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to check limit"))
        }
    }

    private fun Post.toEntity() = PostEntity(
        id = id,
        authorId = userId,
        authorName = userName,
        authorProfileImage = userProfileImage,
        content = content,
        imageUrl = imageUrl,
        state = state,
        district = district,
        locality = locality,
        timestamp = timestamp,
        likesCount = likesCount,
        commentsCount = commentsCount
    )

    private fun PostEntity.toDomain() = Post(
        id = id,
        userId = authorId,
        userName = authorName,
        userProfileImage = authorProfileImage,
        content = content,
        imageUrl = imageUrl,
        state = state,
        district = district,
        locality = locality,
        timestamp = timestamp,
        likesCount = likesCount,
        commentsCount = commentsCount
    )
}
