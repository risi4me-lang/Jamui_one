package com.example.jamuione.data.repository

import android.net.Uri
import android.util.Log
import com.example.jamuione.data.local.dao.PostDao
import com.example.jamuione.data.local.entity.PostEntity
import com.example.jamuione.domain.model.Post
import com.example.jamuione.domain.repository.PostRepository
import com.example.jamuione.util.Resource
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
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
        Log.d("FIRESTORE_DEBUG", "getPosts: locality=$locality, district=$district, state=$state")
        
        var query: Query = firestore.collection("posts")
        
        if (locality != null) {
            query = query.whereEqualTo("locality", locality)
        } else if (district != null) {
            query = query.whereEqualTo("district", district)
        } else if (state != null) {
            query = query.whereEqualTo("state", state)
        }
        
        query = query.orderBy("timestamp", Query.Direction.DESCENDING)

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
                timestamp = System.currentTimeMillis()
            )

            Log.d("FIRESTORE_DEBUG", "createPost: writing to firestore, id=$postId")
            withTimeout(15000L) {
                firestore.collection("posts").document(postId)
                    .set(finalPost)
                    .await()
            }
            Log.d("POST_DEBUG", "createPost: success")
            emit(Resource.Success(true))
        } catch (e: Exception) {
            Log.e("POST_DEBUG", "createPost: failed", e)
            emit(Resource.Error(e.message ?: "Failed to create post"))
        }
    }

    override fun getCachedPosts(): Flow<List<Post>> {
        return postDao.getAllPosts().map { entities ->
            entities.map { it.toDomain() }
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
