package com.example.jamuione.domain.repository

import android.net.Uri
import com.example.jamuione.domain.model.Post
import com.example.jamuione.util.Resource
import kotlinx.coroutines.flow.Flow

interface PostRepository {
    fun getPosts(locality: String? = null, district: String? = null, state: String? = null): Flow<Resource<List<Post>>>
    fun createPost(post: Post, imageUri: Uri?): Flow<Resource<Boolean>>
    fun getCachedPosts(): Flow<List<Post>>
}
