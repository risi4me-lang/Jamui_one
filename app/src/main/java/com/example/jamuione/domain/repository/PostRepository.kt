package com.example.jamuione.domain.repository

import android.net.Uri
import com.example.jamuione.domain.model.Comment
import com.example.jamuione.domain.model.Like
import com.example.jamuione.domain.model.Post
import com.example.jamuione.util.Resource
import kotlinx.coroutines.flow.Flow

interface PostRepository {
    fun getPosts(
        locality: String? = null,
        district: String? = null,
        state: String? = null,
        searchQuery: String? = null
    ): Flow<Resource<List<Post>>>
    fun createPost(post: Post, imageUri: Uri?): Flow<Resource<Boolean>>
    fun getCachedPosts(): Flow<List<Post>>
    fun deletePost(postId: String): Flow<Resource<Boolean>>
    fun toggleLike(postId: String, userId: String, userName: String, userProfileImage: String?, isVerified: Boolean): Flow<Resource<Boolean>>
    fun observeIsLikedByUser(postId: String, userId: String): Flow<Boolean>
    fun getLikers(postId: String): Flow<Resource<List<Like>>>
    fun addComment(postId: String, comment: Comment): Flow<Resource<Boolean>>
    fun getComments(postId: String): Flow<Resource<List<Comment>>>
    fun deleteComment(postId: String, commentId: String): Flow<Resource<Boolean>>
    fun reportPost(postId: String, reporterId: String, reason: String): Flow<Resource<Boolean>>
    fun reportComment(postId: String, commentId: String, reporterId: String, reason: String): Flow<Resource<Boolean>>
    fun toggleSavePost(postId: String, userId: String): Flow<Resource<Boolean>>
    fun observeIsSavedByUser(postId: String, userId: String): Flow<Boolean>
    fun getSavedPosts(userId: String): Flow<Resource<List<Post>>>
}
